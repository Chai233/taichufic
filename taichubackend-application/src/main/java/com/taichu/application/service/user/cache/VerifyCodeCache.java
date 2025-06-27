package com.taichu.application.service.user.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码缓存服务
 */
@Component
@Slf4j
public class VerifyCodeCache {
    
    private final Map<String, VerifyCodeInfo> cache = new ConcurrentHashMap<>();
    
    /**
     * 验证码信息
     */
    public static class VerifyCodeInfo {
        private final String code;
        private final long createTime;
        
        public VerifyCodeInfo(String code) {
            this.code = code;
            this.createTime = System.currentTimeMillis();
        }
        
        public String getCode() {
            return code;
        }
        
        public long getCreateTime() {
            return createTime;
        }
        
        public boolean isExpired(long expireTimeMillis) {
            return System.currentTimeMillis() - createTime > expireTimeMillis;
        }
    }
    
    /**
     * 保存验证码
     * @param phoneNumber 手机号
     * @param code 验证码
     */
    public void saveVerifyCode(String phoneNumber, String code) {
        cache.put(phoneNumber, new VerifyCodeInfo(code));
        log.debug("保存验证码: phoneNumber={}, code={}", phoneNumber, code);
    }
    
    /**
     * 获取验证码信息
     * @param phoneNumber 手机号
     * @return 验证码信息，如果不存在或已过期返回null
     */
    public VerifyCodeInfo getVerifyCode(String phoneNumber) {
        VerifyCodeInfo info = cache.get(phoneNumber);
        if (info != null && info.isExpired(5 * 60 * 1000)) { // 5分钟过期
            cache.remove(phoneNumber);
            info = null;
        }
        return info;
    }
    
    /**
     * 验证验证码
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String phoneNumber, String code) {
        VerifyCodeInfo info = getVerifyCode(phoneNumber);
        if (info != null && info.getCode().equals(code)) {
            // 验证成功后删除验证码
            cache.remove(phoneNumber);
            log.debug("验证码验证成功: phoneNumber={}", phoneNumber);
            return true;
        }
        log.debug("验证码验证失败: phoneNumber={}, inputCode={}, cachedCode={}", 
                phoneNumber, code, info != null ? info.getCode() : "null");
        return false;
    }
    
    /**
     * 检查是否可以发送验证码（1分钟内不能重复发送）
     * @param phoneNumber 手机号
     * @return 是否可以发送
     */
    public boolean canSendVerifyCode(String phoneNumber) {
        VerifyCodeInfo info = cache.get(phoneNumber);
        if (info == null) {
            return true;
        }
        
        // 检查是否在1分钟内
        boolean canSend = System.currentTimeMillis() - info.getCreateTime() > 60 * 1000;
        log.debug("检查是否可以发送验证码: phoneNumber={}, canSend={}", phoneNumber, canSend);
        return canSend;
    }
    
    /**
     * 清理过期的验证码
     */
    public void cleanExpiredCodes() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(5 * 60 * 1000));
    }
} 