package org.hongxi.cloud.sample.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 通用工具类
 * <p>
 * 提供数学运算和字符串处理等通用功能，既可用于内部 AI Tool Calling，
 * 也可通过 MCP 协议对外暴露给 MCP Client 调用。
 * </p>
 *
 * @author javahongxi
 */
@Component
public class SystemTools {

    /**
     * 计算两个数的和
     *
     * @param a 第一个数
     * @param b 第二个数
     * @return 两数之和
     */
    @Tool(description = "计算两个整数的和")
    public int add(@ToolParam(description = "第一个整数") int a,
                   @ToolParam(description = "第二个整数") int b) {
        return a + b;
    }

    /**
     * 计算两个数的乘积
     *
     * @param a 第一个数
     * @param b 第二个数
     * @return 两数之积
     */
    @Tool(description = "计算两个整数的乘积")
    public int multiply(@ToolParam(description = "第一个整数") int a,
                        @ToolParam(description = "第二个整数") int b) {
        return a * b;
    }

    /**
     * 将文本转换为大写
     *
     * @param text 输入文本
     * @return 大写文本
     */
    @Tool(description = "将英文文本转换为大写形式")
    public String toUpperCase(@ToolParam(description = "要转换的英文文本") String text) {
        return text.toUpperCase();
    }

    /**
     * 将文本转换为小写
     *
     * @param text 输入文本
     * @return 小写文本
     */
    @Tool(description = "将英文文本转换为小写形式")
    public String toLowerCase(@ToolParam(description = "要转换的英文文本") String text) {
        return text.toLowerCase();
    }

    /**
     * 反转字符串
     *
     * @param text 输入文本
     * @return 反转后的文本
     */
    @Tool(description = "反转字符串中的字符顺序")
    public String reverseString(@ToolParam(description = "要反转的字符串") String text) {
        return new StringBuilder(text).reverse().toString();
    }
}
