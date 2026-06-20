package org.hongxi.cloud.sample.ai.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 数据转换工具服务
 * <p>
 * 提供常见数据格式转换功能，通过 MCP 协议对外暴露。
 * MCP Client 可以利用这些工具进行编码、解码、格式转换等操作。
 * </p>
 *
 * @author hongxi
 */
@Service
public class ConversionToolService {

    /**
     * URL 编码
     *
     * @param url 原始 URL
     * @return URL 编码后的字符串
     */
    @Tool(description = "对 URL 或查询参数进行 URL 编码")
    public String urlEncode(@ToolParam(description = "需要进行 URL 编码的字符串") String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "URL 编码失败: " + e.getMessage();
        }
    }

    /**
     * URL 解码
     *
     * @param encodedUrl URL 编码后的字符串
     * @return 解码后的 URL
     */
    @Tool(description = "对 URL 编码的字符串进行解码")
    public String urlDecode(@ToolParam(description = "需要进行 URL 解码的字符串") String encodedUrl) {
        try {
            return URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "URL 解码失败: " + e.getMessage();
        }
    }

    /**
     * Base64 编码
     *
     * @param text 原始文本
     * @return Base64 编码后的字符串
     */
    @Tool(description = "将文本转换为 Base64 编码")
    public String base64Encode(@ToolParam(description = "需要编码的原始文本") String text) {
        try {
            return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return "Base64 编码失败: " + e.getMessage();
        }
    }

    /**
     * Base64 解码
     *
     * @param encodedText Base64 编码的文本
     * @return 解码后的文本
     */
    @Tool(description = "将 Base64 编码的字符串解码为原始文本")
    public String base64Decode(@ToolParam(description = "Base64 编码的字符串") String encodedText) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedText);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Base64 解码失败: " + e.getMessage();
        }
    }

    /**
     * 计算字符串长度
     *
     * @param text 输入文本
     * @return 字符串长度
     */
    @Tool(description = "计算字符串的字符数量")
    public int stringLength(@ToolParam(description = "要计算长度的字符串") String text) {
        return text.length();
    }

    /**
     * 统计单词数量
     *
     * @param text 输入文本
     * @return 单词数量
     */
    @Tool(description = "统计英文文本中的单词数量（以空格分隔）")
    public int wordCount(@ToolParam(description = "要统计单词数的英文文本") String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
