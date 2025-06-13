package com.taichu.domain.algo.model.request;

import com.taichu.domain.algo.model.common.RoleDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StoryboardImageRequest {
    private String storyboardId;  // 分镜ID
    private String storyboard;    // 分镜描述文本
    private List<RoleDTO> roles;  // 角色信息
    private String workflowId;    // 工作流ID
} 