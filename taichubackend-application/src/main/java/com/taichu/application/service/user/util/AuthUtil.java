package com.taichu.application.service.user.util;

import com.taichu.application.service.user.cache.AuthCache;
import com.taichu.application.service.user.dto.AuthDTO;
import com.taichu.common.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
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
            throw new BusinessException("1001", "未登录");
        }
        
        AuthDTO authDTO = authCache.getAuth(authId);
        if (authDTO == null) {
            throw new BusinessException("1002", "登录已过期");
        }

        log.debug("getCurrentUserId:{}", authDTO.getUserId());
        return authDTO.getUserId();
    }

} 