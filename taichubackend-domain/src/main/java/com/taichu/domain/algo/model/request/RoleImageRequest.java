package com.taichu.domain.algo.model.request;

import lombok.Data;

import java.util.List;

@Data
public class RoleImageRequest {
    private String role_info;  // 角色心思
    private String workflow_id;    // 工作流ID
    private String image_style;    // 图片风格
    private Integer image_num;    // 生成数量

    @Data
    public static class RoleInfo {
        private String role;        // 角色名
        private String prompt;      // 角色prompt
    }
} 