package com.taichu.application.service.impl;

import com.taichu.application.service.UserAppService;
import com.taichu.gateway.web.user.cache.AuthCache;
import com.taichu.gateway.web.user.dto.AuthDTO;
import com.taichu.infra.persistance.model.FicUser;
import com.taichu.infra.repository.FicUserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserAppServiceImpl implements UserAppService {
    
    private final AuthCache authCache;
    private final FicUserRepository ficUserRepository;
    
    public UserAppServiceImpl(AuthCache authCache, FicUserRepository ficUserRepository) {
        this.authCache = authCache;
        this.ficUserRepository = ficUserRepository;
    }
    
    @Override
    public AuthDTO login(String phone, String verifyCode) {
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
        }
        
        // 生成认证信息
        String authId = UUID.randomUUID().toString();
        AuthDTO authDTO = new AuthDTO();
        authDTO.setAuthId(authId);
        authDTO.setPhone(phone);
        authDTO.setUserId(user.getId());
        
        // 保存认证信息
        authCache.saveAuth(authId, authDTO);
        
        return authDTO;
    }
} 