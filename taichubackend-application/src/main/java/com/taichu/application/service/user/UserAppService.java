package com.taichu.application.service.user;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.application.service.user.cache.AuthCache;
import com.taichu.application.service.user.cache.VerifyCodeCache;
import com.taichu.application.service.user.dto.AuthDTO;
import com.taichu.application.service.user.util.AuthUtil;
import com.taichu.common.common.exception.AppServiceExceptionHandle;
import com.taichu.common.util.VerifyCodeUtil;
import com.taichu.domain.gateway.SmsGateway;
import com.taichu.infra.persistance.model.FicUser;
import com.taichu.infra.repository.FicUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class UserAppService {
    
    private final AuthCache authCache;
    private final VerifyCodeCache verifyCodeCache;
    private final FicUserRepository ficUserRepository;
    private final SmsGateway smsGateway;
    
    public UserAppService(AuthCache authCache, VerifyCodeCache verifyCodeCache, 
                         FicUserRepository ficUserRepository, SmsGateway smsGateway) {
        this.authCache = authCache;
        this.verifyCodeCache = verifyCodeCache;
        this.ficUserRepository = ficUserRepository;
        this.smsGateway = smsGateway;
    }

    @EntranceLog(bizCode = "SEND_VERIFY_CODE")
    @AppServiceExceptionHandle(biz = "SEND_VERIFY_CODE")
    public SingleResponse<Void> sendVerifyCode(String phone) {
        // 检查是否可以发送验证码（1分钟内不能重复发送）
        if (!verifyCodeCache.canSendVerifyCode(phone)) {
            return SingleResponse.buildFailure("SMS_0001", "验证码发送过于频繁，请1分钟后再试");
        }
        
        // 生成6位数字验证码
        String verifyCode = VerifyCodeUtil.generateVerifyCode();
        
        // 发送短信验证码
        boolean sendSuccess = smsGateway.sendVerifyCode(phone, verifyCode);
        if (!sendSuccess) {
            return SingleResponse.buildFailure("SMS_0002", "短信发送失败，请稍后重试");
        }
        
        // 保存验证码到缓存
        verifyCodeCache.saveVerifyCode(phone, verifyCode);

        log.info("发送验证码成功: phone={}, code={}", phone, verifyCode);
        return SingleResponse.buildSuccess();
    }

    @EntranceLog(bizCode = "USER_LOGIN")
    @AppServiceExceptionHandle(biz = "USER_LOGIN")
    public SingleResponse<AuthDTO> login(String phone, String verifyCode) {

        if (AuthUtil.isAdmin()) {
            // do nothing
            // 超级管理员后门
        } else {
            // 验证短信验证码格式
            if (!VerifyCodeUtil.isValidFormat(verifyCode)) {
                return SingleResponse.buildFailure("SMS_0003", "验证码格式错误，请输入6位数字验证码");
            }

            // 校验验证码是否为刚刚发送的验证码
            if (!verifyCodeCache.verifyCode(phone, verifyCode)) {
                return SingleResponse.buildFailure("SMS_0004", "验证码错误或已过期，请重新获取验证码");
            }
        }
        
        // 查找或创建用户
        FicUser user = ficUserRepository.findByPhoneNumber(phone);
        if (user == null) {
            user = new FicUser();
            user.setPhoneNumber(phone);
            user.setGmtCreate(System.currentTimeMillis());
            user = ficUserRepository.save(user);
            log.info("创建新用户: phone={}, userId={}", phone, user.getId());
        }
        
        // 生成认证信息
        String authId = UUID.randomUUID().toString();
        AuthDTO authDTO = new AuthDTO();
        authDTO.setAuthId(authId);
        authDTO.setPhone(phone);
        authDTO.setUserId(user.getId());
        
        // 保存认证信息
        authCache.saveAuth(authId, authDTO);

        log.info("用户登录成功: phone={}, userId={}, authId={}", phone, user.getId(), authId);
        return SingleResponse.of(authDTO);
    }
} 