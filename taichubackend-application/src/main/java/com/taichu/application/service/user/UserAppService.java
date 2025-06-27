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
    private final FicUserRepository ficUserRepository;
    
    public UserAppService(AuthCache authCache, FicUserRepository ficUserRepository) {
        this.authCache = authCache;
        this.ficUserRepository = ficUserRepository;
    }

    @EntranceLog(bizCode = "USER_LOGIN")
    @AppServiceExceptionHandle(biz = "USER_LOGIN")
    public SingleResponse<AuthDTO> login(String phone, String verifyCode) {
        // TODO: 验证短信验证码
        // 这里应该调用短信服务验证验证码
        if (verifyCode.length() != 6) {
            throw new RuntimeException("验证码错误");
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