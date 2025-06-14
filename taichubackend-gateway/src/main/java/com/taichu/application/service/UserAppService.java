package com.taichu.application.service;

import com.taichu.gateway.web.user.dto.AuthDTO;

public interface UserAppService {
    AuthDTO login(String phone, String verifyCode);
} 