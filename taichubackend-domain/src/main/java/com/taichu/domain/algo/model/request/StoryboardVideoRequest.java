package com.taichu.domain.algo.model.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class StoryboardVideoRequest {
    private String storyboard_id;  // 分镜ID
    private String storyboard;    // 分镜描述文本
    private String workflow_id;    // 工作流ID
    private List<RoleDTO> roles;
    /**
     * 视频画面风格，默认“赛博朋克”
     */
    private String video_style;
    /**
     * 旁白配音风格，默认“磁性男声”
     */
    private String voice_type;

    @Getter
    @Setter
    public static class RoleDTO {
        // 角色名称
        String role;
        String image;
    }
} 