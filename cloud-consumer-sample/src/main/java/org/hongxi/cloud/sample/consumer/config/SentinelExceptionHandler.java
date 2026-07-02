package org.hongxi.cloud.sample.consumer.config;

import com.alibaba.cloud.sentinel.rest.SentinelClientHttpResponse;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;

/**
 * Sentinel RestTemplate 的 blockHandler / fallback / urlCleaner 处理类。
 * 必须是顶级 public 类，否则 SentinelProtectInterceptor 通过反射调用时
 * 会因嵌套类访问控制抛出 IllegalAccessException。
 */
public class SentinelExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SentinelExceptionHandler.class);

    private SentinelExceptionHandler() {
    }

    public static SentinelClientHttpResponse handleException(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution, BlockException e) {
        log.info("Oops: {}", e.getClass().getCanonicalName());
        return new SentinelClientHttpResponse("Blocked by Sentinel");
    }

    public static SentinelClientHttpResponse handleFallback(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution, BlockException e) {
        log.info("Fallback triggered: {}", e.getClass().getCanonicalName());
        return new SentinelClientHttpResponse("Blocked by Sentinel");
    }

    /**
     * URL 清理器：去除资源名中的端口号，使 RestTemplate 资源名与 Feign 保持一致
     * 例如: GET:http://provider-sample:8765/hello → GET:http://provider-sample/hello
     */
    public static String cleanUrl(String originUrl) {
        return originUrl.replaceAll(":\\d+", "");
    }
}
