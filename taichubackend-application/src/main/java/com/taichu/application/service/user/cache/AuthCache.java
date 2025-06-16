package com.taichu.application.service.user.cache;

import com.taichu.application.service.user.dto.AuthDTO;
import org.springframework.stereotype.Component;

@Component
public class AuthCache {
    private static final long EXPIRATION_HOURS = 12;
    private final ExpiringCache<String, AuthDTO> cache;
    
    public AuthCache() {
        this.cache = new ExpiringCache<>(EXPIRATION_HOURS * 60 * 60 * 1000);
    }
    
    public void saveAuth(String authId, AuthDTO authDTO) {
        cache.put(authId, authDTO);
    }
    
    public AuthDTO getAuth(String authId) {
        return cache.get(authId);
    }
    
    public void removeAuth(String authId) {
        cache.remove(authId);
    }
} 