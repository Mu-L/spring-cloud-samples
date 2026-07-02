package org.hongxi.cloud.sample.scheduling.job;

import java.util.concurrent.TimeUnit;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SimpleJob {

	private static final Logger log = LoggerFactory.getLogger(SimpleJob.class);

	/**
	 * run without lock, all instance running at the same time.
	 */
	@Scheduled(cron = "0/5 * * * * ?")
	public void job1() {
        log.info("time={} do job1...", DateTime.now().toString("YYYY-MM-dd HH:mm:ss"));
	}


	/**
	 * run with lock, only one instance running at the same time.
	 *
	 * @throws InterruptedException interrupted exception
	 */
	@Scheduled(cron = "0/5 * * * * ?")
	@SchedulerLock(name = "lock-job2", lockAtMostFor = "10s")
	public void job2() throws InterruptedException {
        log.info("time={} do job2...", DateTime.now().toString("YYYY-MM-dd HH:mm:ss"));
		TimeUnit.SECONDS.sleep(1L);
	}
}