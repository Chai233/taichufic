package com.taichu.application.service.inner.algo.v2.context;

import com.taichu.domain.model.FicRoleBO;
import com.taichu.domain.model.FicStoryboardBO;

import java.util.List;

/**
 * 分镜图片生成任务上下文
 */
public class StoryboardImgTaskContext extends AlgoTaskContext {
    private FicStoryboardBO storyboard;
    private List<FicRoleBO> roles;
    private String imageStyle;
    private Float scale;
    private Float styleScale;
    
    @Override
    public String getTaskSummary() {
        return String.format("Storyboard image generation for storyboard %d in workflow %d", 
            storyboard != null ? storyboard.getId() : 0, getWorkflowId());
    }
    
    // getter/setter
    public FicStoryboardBO getStoryboard() { return storyboard; }
    public void setStoryboard(FicStoryboardBO storyboard) { this.storyboard = storyboard; }
    public List<FicRoleBO> getRoles() { return roles; }
    public void setRoles(List<FicRoleBO> roles) { this.roles = roles; }
    public String getImageStyle() { return imageStyle; }
    public void setImageStyle(String imageStyle) { this.imageStyle = imageStyle; }
    public Float getScale() { return scale; }
    public void setScale(Float scale) { this.scale = scale; }
    public Float getStyleScale() { return styleScale; }
    public void setStyleScale(Float styleScale) { this.styleScale = styleScale; }
} 