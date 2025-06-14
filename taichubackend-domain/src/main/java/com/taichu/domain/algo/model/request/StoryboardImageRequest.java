package com.taichu.domain.algo.model.request;

import lombok.Data;

import java.util.List;

@Data
public class StoryboardImageRequest {
    private String storyboard_id;  // 分镜ID
    private String storyboard;    // 分镜描述文本
    private List<RoleDTO> roles;  // 角色信息
    private String workflow_id;    // 工作流ID
    private String image_style;    // 图片风格
    private Float scale;            // 文本引导强度
    private Float style_scale;    // 风格引导强度

    @Data
    public static class RoleDTO {
        private String role;        // 角色名
        private String image;       // 角色图文件名
    }
} 