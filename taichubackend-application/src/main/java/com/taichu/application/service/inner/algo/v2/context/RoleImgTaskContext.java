package com.taichu.application.service.inner.algo.v2.context;

import com.taichu.domain.model.FicRoleBO;

/**
 * 角色图片生成任务上下文
 */
public class RoleImgTaskContext extends AlgoTaskContext {
    private FicRoleBO role;
    private String imageStyle;
    
    @Override
    public String getTaskSummary() {
        return String.format("Role image generation for role %s in workflow %d", 
            role != null ? role.getRoleName() : "unknown", getWorkflowId());
    }
    
    // getter/setter
    public FicRoleBO getRole() { return role; }
    public void setRole(FicRoleBO role) { this.role = role; }
    public String getImageStyle() { return imageStyle; }
    public void setImageStyle(String imageStyle) { this.imageStyle = imageStyle; }
} 