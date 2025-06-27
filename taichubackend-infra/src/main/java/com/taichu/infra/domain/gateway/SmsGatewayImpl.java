package com.taichu.infra.domain.gateway;

import com.taichu.domain.gateway.SmsGateway;
import com.taichu.infra.sms.AliCloudSmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信网关实现
 */
@Service
@Slf4j
public class SmsGatewayImpl implements SmsGateway {
    
    private final AliCloudSmsService aliCloudSmsService;
    
    public SmsGatewayImpl(AliCloudSmsService aliCloudSmsService) {
        this.aliCloudSmsService = aliCloudSmsService;
    }
    
    @Override
    public boolean sendVerifyCode(String phoneNumber, String verifyCode) {
        log.info("发送短信验证码: phoneNumber={}", phoneNumber);
        return aliCloudSmsService.sendVerifyCode(phoneNumber, verifyCode);
    }
    
    @Override
    public boolean verifyCode(String phoneNumber, String verifyCode) {
        // 这里可以调用阿里云的验证码验证接口，或者使用本地缓存验证
        // 目前使用本地缓存验证，实际项目中可能需要调用阿里云的验证接口
        log.info("验证短信验证码: phoneNumber={}", phoneNumber);
        // 注意：这里应该由应用层传入验证结果，而不是在这里直接验证
        // 因为验证码的存储和验证逻辑在应用层的缓存中
        return false;
    }
} 