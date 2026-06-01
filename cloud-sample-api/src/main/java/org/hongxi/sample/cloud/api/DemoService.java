package org.hongxi.sample.cloud.api;

import java.util.concurrent.CompletableFuture;

/**
 * Created by shenhongxi on 2026/6/1.
 */
public interface DemoService {

	String sayHello(String name);

	default CompletableFuture<String> sayHelloAsync(String name) {
		return CompletableFuture.completedFuture(sayHello(name));
	}
}