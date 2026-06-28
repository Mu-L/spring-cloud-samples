package org.hongxi.cloud.sample.ai.vo;

/**
 * 复杂任务处理结果
 *
 * @param task     任务描述
 * @param solution 解决方案
 * @param type     类型标识
 * @author hongxi
 */
public record TaskResult(String task, String solution, String type) {
}
