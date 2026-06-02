package org.hongxi.cloud.sample.api;

import java.util.concurrent.CompletableFuture;

/**
 * Created by javahongxi on 2026/6/1.
 */
public interface DemoService {

	String sayHello(String name);

	default CompletableFuture<String> sayHelloAsync(String name) {
		return CompletableFuture.completedFuture(sayHello(name));
	}
}
