package org.hongxi.cloud.sample.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Web 搜索工具（基于 Tavily Search API）
 * <p>
 * 提供实时网络搜索能力，AI 模型在需要获取最新信息时会自动调用。
 * 需要配置 TAVILY_API_KEY 环境变量（免费额度：1000 次/月）。
 * 申请地址：https://tavily.com
 * </p>
 *
 * @author javahongxi
 */
@Component
public class WebSearchTool {

    private final HttpClient httpClient;
    private final String apiKey;

    public WebSearchTool(@Value("${TAVILY_API_KEY:}") String apiKey) {
        this.apiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : System.getenv("TAVILY_API_KEY");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 搜索网络信息
     *
     * @param query      搜索关键词
     * @param maxResults 最大结果数（1-10）
     * @return 搜索结果
     */
    @Tool(description = "搜索网络获取实时信息，适用于查询最新新闻、事件、价格等需要时效性数据的场景。"
            + "返回搜索结果的标题、摘要和链接。")
    public String webSearch(
            @ToolParam(description = "搜索关键词，尽量具体明确。如需时效性信息可加上年份，如 '2026年最新电影'") String query,
            @ToolParam(description = "最大结果数量（1-10，默认 5）", required = false) Integer maxResults) {

        if (apiKey == null || apiKey.isEmpty()) {
            return "Web 搜索功能未配置。请设置 TAVILY_API_KEY 环境变量。\n"
                    + "申请地址：https://tavily.com（免费额度：1000 次/月）";
        }

        int results = (maxResults != null && maxResults >= 1 && maxResults <= 10) ? maxResults : 5;

        try {
            String requestBody = String.format(
                    "{\"query\":\"%s\",\"max_results\":%d,\"include_answer\":true}",
                    escapeJson(query), results);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.tavily.com/search"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "搜索请求失败: HTTP " + response.statusCode();
            }

            return parseSearchResults(response.body());

        } catch (Exception e) {
            return "搜索失败: " + e.getMessage();
        }
    }

    private String parseSearchResults(String responseBody) {
        // 简单解析 JSON 响应，提取关键信息
        StringBuilder result = new StringBuilder();

        // 提取 answer（如果有）
        String answer = extractJsonValue(responseBody, "answer");
        if (answer != null && !answer.isEmpty()) {
            result.append("【AI 摘要】\n").append(answer).append("\n\n");
        }

        // 提取 results 数组中的标题和摘要
        result.append("【搜索结果】\n");
        int idx = responseBody.indexOf("\"results\"");
        if (idx >= 0) {
            String resultsSection = responseBody.substring(idx);
            int count = 0;
            int pos = 0;
            while (count < 10) {
                int titleIdx = resultsSection.indexOf("\"title\"", pos);
                if (titleIdx < 0) break;

                String title = extractJsonValue(resultsSection.substring(titleIdx), "title");
                String content = extractJsonValue(resultsSection.substring(titleIdx), "content");
                String url = extractJsonValue(resultsSection.substring(titleIdx), "url");

                if (title != null) {
                    count++;
                    result.append(count).append(". ").append(title).append("\n");
                    if (content != null) {
                        result.append("   ").append(truncate(content, 200)).append("\n");
                    }
                    if (url != null) {
                        result.append("   链接: ").append(url).append("\n");
                    }
                }
                pos = titleIdx + 1;
            }
        }

        return result.length() > 0 ? result.toString() : "未找到相关搜索结果";
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(":", keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        int startIdx = colonIdx + 1;
        while (startIdx < json.length() && json.charAt(startIdx) == ' ') startIdx++;

        if (startIdx >= json.length()) return null;

        if (json.charAt(startIdx) == '"') {
            int endIdx = startIdx + 1;
            while (endIdx < json.length()) {
                if (json.charAt(endIdx) == '"' && json.charAt(endIdx - 1) != '\\') break;
                endIdx++;
            }
            return json.substring(startIdx + 1, endIdx)
                    .replace("\\\"", "\"")
                    .replace("\\n", " ")
                    .replace("\\\\", "\\");
        }
        return null;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
