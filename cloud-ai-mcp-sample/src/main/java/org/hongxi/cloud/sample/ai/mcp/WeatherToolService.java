package org.hongxi.cloud.sample.ai.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 天气工具服务
 * <p>
 * 通过 MCP 协议对外暴露天气查询能力。
 * MCP Client（如 AI 助手）可以自动发现并调用这些工具。
 * </p>
 *
 * @author hongxi
 */
@Service
public class WeatherToolService {

    /**
     * 根据城市名称获取天气信息
     * <p>
     * description 非常重要，它会告诉 MCP Client 这个工具是做什么的。
     * AI 模型根据 description 来决定是否调用此工具。
     * </p>
     *
     * @param city 城市名称
     * @return 天气信息
     */
    @Tool(description = "根据城市名称获取当地的实时天气状况，包括温度、天气状况等")
    public String getWeatherByCity(@ToolParam(description = "城市名称，例如：北京、上海、广州") String city) {
        // 模拟天气数据（实际项目中可调用第三方天气 API）
        return switch (city) {
            case "北京" -> "晴天，气温 25°C，空气质量良好";
            case "上海" -> "多云，气温 28°C，湿度 65%";
            case "广州" -> "小雨，气温 30°C，注意带伞";
            case "深圳" -> "晴天，气温 29°C，适合出行";
            case "杭州" -> "阴天，气温 26°C，空气湿度 70%";
            default -> city + " 今天天气晴朗，气温25度！";
        };
    }

    /**
     * 获取天气建议
     *
     * @param city 城市名称
     * @return 出行建议
     */
    @Tool(description = "根据城市天气给出出行和穿衣建议")
    public String getWeatherAdvice(@ToolParam(description = "城市名称") String city) {
        return switch (city) {
            case "北京" -> "今天天气晴好，适合户外活动，建议穿薄外套";
            case "上海" -> "多云天气，温度适中，建议穿长袖衬衫";
            case "广州" -> "有小雨，出门请带伞，建议穿短袖加薄外套";
            default -> city + " 天气状况良好，正常出行即可";
        };
    }
}
