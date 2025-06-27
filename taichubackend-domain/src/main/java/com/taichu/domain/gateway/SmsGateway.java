package com.taichu.domain.gateway;

/**
 * 短信网关接口
 */
public interface SmsGateway {
    
    /**
     * 发送短信验证码
     * @param phoneNumber 手机号
     * @param verifyCode 验证码
     * @return 是否发送成功
     */
    boolean sendVerifyCode(String phoneNumber, String verifyCode);
    
    /**
     * 验证短信验证码
     * @param phoneNumber 手机号
     * @param verifyCode 验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String phoneNumber, String verifyCode);
} 