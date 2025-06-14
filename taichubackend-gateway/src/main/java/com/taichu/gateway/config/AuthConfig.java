package com.taichu.gateway.config;

import com.taichu.gateway.web.user.cache.AuthCache;
import com.taichu.gateway.web.user.util.AuthUtil;
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