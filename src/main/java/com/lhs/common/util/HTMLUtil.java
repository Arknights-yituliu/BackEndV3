package com.lhs.common.util;

import java.util.Stack;

public class HTMLUtil {
    /**
     * 校验描述字符串，删除所有多余的、匹配不上的“<”或“>”。
     * 每一个“<”都会与最近的一个“>”进行匹配，且每个“<”或“>”都只能互相匹配一次。
     *
     * @param htmlStr 待清洗的HTML字符串
     * @return 清洗后的HTML字符串
     */
    public static String removeExcessParentheses(String htmlStr) {
        char[] chars = htmlStr.toCharArray();
        int length = chars.length;
        boolean[] matched = new boolean[length]; // 标记每个字符是否已匹配
        StringBuilder sb = new StringBuilder();
        Stack<Integer> stack = new Stack<>(); // 栈用于存储“<”的索引

        // 遍历字符数组，使用栈匹配“<”与“>”
        for (int i = 0; i < length; i++) {
            if (chars[i] == '<') {
                // 将“<”的索引压入栈中
                stack.push(i);
            } else if (chars[i] == '>') {
                if (!stack.isEmpty()) {
                    // 弹出栈顶的“<”索引，标记这对“<”和“>”为匹配
                    int start = stack.pop();
                    matched[start] = true;
                    matched[i] = true;
                }
                // 如果栈为空，说明当前“>”没有对应的“<”，忽略它
            }
        }

        // 构建最终的HTML，保留匹配的“<”和“>”，删除未匹配的
        for (int i = 0; i < length; i++) {
            if (chars[i] == '<' || chars[i] == '>') {
                if (matched[i]) {
                    sb.append(chars[i]);
                }
                // 未匹配的“<”或“>”不添加到结果中
            } else {
                sb.append(chars[i]);
            }
        }

        return sb.toString();
    }
}
