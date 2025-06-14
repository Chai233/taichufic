package com.taichu.gateway.web.user.util;

import com.taichu.gateway.web.user.cache.AuthCache;
import com.taichu.gateway.web.user.dto.AuthDTO;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class AuthUtil {
    private static final String AUTH_HEADER = "X-Auth-Id";
    private static AuthCache authCache;
    
    public static void setAuthCache(AuthCache cache) {
        authCache = cache;
    }
    
    public static String getAuthId() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return request.getHeader(AUTH_HEADER);
    }
    
    public static Long getCurrentUserId() {
        String authId = getAuthId();
        if (authId == null) {
            throw new RuntimeException("未登录");
        }
        
        AuthDTO authDTO = authCache.getAuth(authId);
        if (authDTO == null) {
            throw new RuntimeException("登录已过期");
        }
        
        return authDTO.getUserId();
    }
    
    public static void validateAuth() {
        getCurrentUserId();
    }
} 