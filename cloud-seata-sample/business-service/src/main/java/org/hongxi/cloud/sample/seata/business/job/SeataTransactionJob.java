package org.hongxi.cloud.sample.seata.business.job;

import java.util.List;
import java.util.Map;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Seata 分布式事务运维定时任务
 * <p>
 * 提供三类定时运维能力：
 * 1. 事务执行摘要统计 — 定期汇总 global_table 中各状态事务数量
 * 2. 悬挂事务检测   — 发现长时间处于 Begin 状态的全局事务并告警
 * 3. Undo Log 清理  — 清理已完成的过期 undo_log 记录，释放存储空间
 */
@Component
public class SeataTransactionJob {

    private static final Logger log = LoggerFactory.getLogger(SeataTransactionJob.class);

    /**
     * Seata 全局事务状态：Begin
     */
    private static final int STATUS_BEGIN = 1;

    /**
     * Seata 全局事务状态：Committed
     */
    private static final int STATUS_COMMITTED = 2;

    /**
     * Seata 全局事务状态：Committing（异步提交中）
     */
    private static final int STATUS_COMMITTING = 8;

    /**
     * Seata 全局事务状态：Rollbacked
     */
    private static final int STATUS_ROLLBACKED = 3;

    /**
     * Seata 全局事务状态：Rollbacking
     */
    private static final int STATUS_ROLLBACKING = 9;

    /**
     * 悬挂事务告警阈值：超过 5 分钟仍处于 Begin 状态视为异常
     */
    private static final long HANGING_THRESHOLD_MS = 5 * 60 * 1000L;

    /**
     * Undo Log 清理阈值：超过 24 小时的已完成事务对应的 undo_log
     */
    private static final int UNDO_LOG_RETAIN_HOURS = 24;

    private final JdbcTemplate jdbcTemplate;

    public SeataTransactionJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 事务执行摘要统计
     * <p>
     * 每 30 秒执行一次，查询 global_table 中各状态事务数量并输出摘要日志。
     * 使用 ShedLock 保证多实例部署时只有一个实例执行。
     */
    @Scheduled(fixedRate = 30000)
    @SchedulerLock(name = "seataTransactionSummary", lockAtMostFor = "20s")
    public void transactionSummary() {
        try {
            Map<String, Integer> statusCounts = Map.of(
                    "Begin", countByStatus(STATUS_BEGIN),
                    "Committed", countByStatus(STATUS_COMMITTED),
                    "Committing", countByStatus(STATUS_COMMITTING),
                    "Rollbacked", countByStatus(STATUS_ROLLBACKED),
                    "Rollbacking", countByStatus(STATUS_ROLLBACKING)
            );

            int total = statusCounts.values().stream().mapToInt(Integer::intValue).sum();

            if (total > 0) {
                log.info("[Seata摘要] 全局事务统计 => 总计: {}, Begin: {}, Committed: {}, "
                                + "Committing: {}, Rollbacked: {}, Rollbacking: {}",
                        total,
                        statusCounts.get("Begin"),
                        statusCounts.get("Committed"),
                        statusCounts.get("Committing"),
                        statusCounts.get("Rollbacked"),
                        statusCounts.get("Rollbacking"));
            } else {
                log.debug("[Seata摘要] 当前无全局事务记录");
            }
        } catch (Exception e) {
            log.warn("[Seata摘要] 统计查询异常: {}", e.getMessage());
        }
    }

    /**
     * 悬挂事务检测
     * <p>
     * 每 60 秒执行一次，检测超过阈值时间仍处于 Begin 状态的全局事务。
     * 这些事务可能是由于网络超时、服务宕机等原因导致未正常完成，需要告警。
     */
    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "seataHangingTransactionCheck", lockAtMostFor = "30s")
    public void hangingTransactionCheck() {
        try {
            long thresholdTime = System.currentTimeMillis() - HANGING_THRESHOLD_MS;

            List<Map<String, Object>> hangingTxs = jdbcTemplate.queryForList(
                    "SELECT xid, transaction_name, begin_time, timeout "
                            + "FROM global_table WHERE status = ? AND begin_time < ?",
                    STATUS_BEGIN, thresholdTime);

            if (hangingTxs.isEmpty()) {
                log.debug("[Seata巡检] 无悬挂事务");
                return;
            }

            log.warn("[Seata巡检] 发现 {} 个悬挂事务（超过 {} 分钟未完成）:",
                    hangingTxs.size(), HANGING_THRESHOLD_MS / 60000);

            for (Map<String, Object> tx : hangingTxs) {
                log.warn("[Seata巡检]   XID: {}, 事务名: {}, 开始时间: {}, 超时设置: {}ms",
                        tx.get("xid"),
                        tx.get("transaction_name"),
                        tx.get("begin_time"),
                        tx.get("timeout"));
            }
        } catch (Exception e) {
            log.warn("[Seata巡检] 悬挂事务检测异常: {}", e.getMessage());
        }
    }

    /**
     * Undo Log 过期清理
     * <p>
     * 每 5 分钟执行一次，清理已完成事务（Committed/Rollbacked）对应的过期 undo_log。
     * 仅删除 log_created 超过保留时间的记录，避免误删正在使用的回滚数据。
     */
    @Scheduled(fixedRate = 300000)
    @SchedulerLock(name = "seataUndoLogCleanup", lockAtMostFor = "60s")
    public void undoLogCleanup() {
        try {
            // 查询已完成的 XID（Committed 或 Rollbacked），且超过保留时间
            List<String> expiredXids = jdbcTemplate.queryForList(
                    "SELECT xid FROM global_table "
                            + "WHERE status IN (?, ?) "
                            + "AND gmt_modified < DATE_SUB(NOW(), INTERVAL ? HOUR)",
                    String.class,
                    STATUS_COMMITTED, STATUS_ROLLBACKED, UNDO_LOG_RETAIN_HOURS);

            if (expiredXids.isEmpty()) {
                log.debug("[Seata清理] 无过期 undo_log 需要清理");
                return;
            }

            int deletedCount = 0;
            // 分批删除，每批最多 100 条
            for (int i = 0; i < expiredXids.size(); i += 100) {
                List<String> batch = expiredXids.subList(i, Math.min(i + 100, expiredXids.size()));
                String placeholders = String.join(",", java.util.Collections.nCopies(batch.size(), "?"));
                int affected = jdbcTemplate.update(
                        "DELETE FROM undo_log WHERE xid IN (" + placeholders + ")",
                        batch.toArray());
                deletedCount += affected;
            }

            log.info("[Seata清理] 已清理 {} 条过期 undo_log（涉及 {} 个已完成事务，保留 {} 小时内的记录）",
                    deletedCount, expiredXids.size(), UNDO_LOG_RETAIN_HOURS);
        } catch (Exception e) {
            log.warn("[Seata清理] undo_log 清理异常: {}", e.getMessage());
        }
    }

    private int countByStatus(int status) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM global_table WHERE status = ?",
                Integer.class, status);
        return count != null ? count : 0;
    }
}
