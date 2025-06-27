package com.taichu.common.util;

import java.util.Random;

/**
 * 验证码生成工具类
 */
public class VerifyCodeUtil {
    
    private static final Random RANDOM = new Random();
    
    /**
     * 生成6位数字验证码
     * @return 6位数字验证码字符串
     */
    public static String generateVerifyCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * 验证验证码格式是否正确（6位数字）
     * @param code 验证码
     * @return 是否有效
     */
    public static boolean isValidFormat(String code) {
        return code != null && code.length() == 6 && code.matches("\\d{6}");
    }
} 