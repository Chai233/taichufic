package com.taichu.domain.algo.model.common;

import lombok.Data;

@Data
public class RoleBO {
    private String name;        // 角色名
    private String prompt;      // 提示词
    private String description; // 角色描述
} 