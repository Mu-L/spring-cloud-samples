package org.hongxi.cloud.sample.kafka.controller;

import org.hongxi.cloud.sample.kafka.Producer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka")
public class SampleController {

	private final Producer producer;

	public SampleController(Producer producer) {
		this.producer = producer;
	}

	/**
	 * 发送传统 Consumer Group 消息
	 */
	@PostMapping("/traditional")
	public ResponseEntity<String> sendTraditional() {
		producer.sendTraditional();
		return ResponseEntity.ok("Sent traditional message");
	}

	/**
	 * 发送 Share Group 隐式确认消息
	 */
	@PostMapping("/share/implicit")
	public ResponseEntity<String> sendShareImplicit(@RequestParam(defaultValue = "10") int count) {
		producer.sendShareImplicit(count);
		return ResponseEntity.ok("Sent share implicit message, count: " + count);
	}

	/**
	 * 发送 Share Group 显式确认消息
	 */
	@PostMapping("/share/explicit")
	public ResponseEntity<String> sendShareExplicit(@RequestParam(defaultValue = "10") int count) {
		producer.sendShareExplicit(count);
		return ResponseEntity.ok("Sent share explicit message, count: " + count);
	}
}
