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
    private static final String ADMIN_HEADER = "X-Admin-Key";
    private static final String ADMIN_KEY = "8424b858-7e8f-45f5-9134-6b0fc2334cd2";
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

    /**
     * 判断当前请求是否为管理员请求
     * @return true表示是管理员，false表示不是管理员
     */
    public static boolean isAdmin() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String adminKey = request.getHeader(ADMIN_HEADER);
            return ADMIN_KEY.equals(adminKey);
        } catch (Exception e) {
            log.warn("获取管理员权限时发生异常", e);
            return false;
        }
    }
} 