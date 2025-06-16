package com.taichu.application.config;

import com.taichu.application.service.user.cache.AuthCache;
import com.taichu.application.service.user.util.AuthUtil;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class AuthConfig {
    
    private final AuthCache authCache;
    
    public AuthConfig(AuthCache authCache) {
        this.authCache = authCache;
    }
    
    @PostConstruct
    public void init() {
        AuthUtil.setAuthCache(authCache);
    }
} 