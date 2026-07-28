package org.hongxi.cloud.sample.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 通用 HTTP 请求工具
 * <p>
 * 支持 GET/POST/PUT/DELETE 等 HTTP 方法，可自定义请求头和请求体。
 * 适用于调用 REST API、测试接口等场景。
 * </p>
 *
 * @author javahongxi
 */
@Component
public class HttpRequestTool {

    private final HttpClient httpClient;

    public HttpRequestTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 发送 HTTP 请求
     *
     * @param method  HTTP 方法（GET, POST, PUT, DELETE, PATCH）
     * @param url     请求 URL
     * @param headers 请求头（JSON 格式，可选）
     * @param body    请求体（可选）
     * @return 响应内容
     */
    @Tool(name = "http_request", description = "发送 HTTP 请求到指定 URL，支持 GET/POST/PUT/DELETE 等方法。"
            + "可自定义请求头和请求体，适用于调用 REST API、测试接口等场景。")
    public String httpRequest(
            @ToolParam(description = "HTTP 方法：GET, POST, PUT, DELETE, PATCH") String method,
            @ToolParam(description = "完整的请求 URL") String url,
            @ToolParam(description = "请求头，JSON 格式，如 {\"Content-Type\":\"application/json\"}", required = false) Map<String, String> headers,
            @ToolParam(description = "请求体内容（POST/PUT/PATCH 时使用）", required = false) String body) {

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30));

            // 设置请求头
            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            // 设置请求方法和体
            HttpRequest.BodyPublisher bodyPublisher = (body != null && !body.isEmpty())
                    ? HttpRequest.BodyPublishers.ofString(body)
                    : HttpRequest.BodyPublishers.noBody();

            requestBuilder.method(method.toUpperCase(), bodyPublisher);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return String.format("HTTP %d %s\n\n响应头: %s\n\n响应体:\n%s",
                    response.statusCode(),
                    statusText(response.statusCode()),
                    formatHeaders(response.headers().map()),
                    truncate(response.body(), 4000));

        } catch (Exception e) {
            return "请求失败: " + e.getMessage();
        }
    }

    private String statusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }

    private String formatHeaders(Map<String, java.util.List<String>> headers) {
        StringBuilder sb = new StringBuilder();
        headers.forEach((key, values) -> {
            if (!key.isEmpty()) {
                sb.append(key).append(": ").append(String.join(", ", values)).append("; ");
            }
        });
        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "\n... (已截断)" : text;
    }
}
