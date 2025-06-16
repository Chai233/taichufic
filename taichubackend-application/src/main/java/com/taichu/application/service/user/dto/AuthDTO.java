package com.taichu.application.service.user.dto;

import lombok.Data;

@Data
public class AuthDTO {
    private String authId;
    private Long userId;
    private String phone;
} 