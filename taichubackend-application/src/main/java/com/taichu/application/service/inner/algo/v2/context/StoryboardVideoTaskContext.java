package com.taichu.application.service.inner.algo.v2.context;

import com.taichu.domain.model.FicRoleBO;
import com.taichu.domain.model.FicStoryboardBO;

import java.util.List;

/**
 * 分镜视频生成任务上下文
 */
public class StoryboardVideoTaskContext extends AlgoTaskContext {
    private FicStoryboardBO storyboard;
    private List<FicRoleBO> roles;
    private String voiceType;
    private String bgmType;
    
    @Override
    public String getTaskSummary() {
        return String.format("Storyboard video generation for storyboard %d in workflow %d", 
            storyboard != null ? storyboard.getId() : 0, getWorkflowId());
    }
    
    // getter/setter
    public FicStoryboardBO getStoryboard() { return storyboard; }
    public void setStoryboard(FicStoryboardBO storyboard) { this.storyboard = storyboard; }
    public List<FicRoleBO> getRoles() { return roles; }
    public void setRoles(List<FicRoleBO> roles) { this.roles = roles; }
    public String getVoiceType() { return voiceType; }
    public void setVoiceType(String voiceType) { this.voiceType = voiceType; }
    public String getBgmType() { return bgmType; }
    public void setBgmType(String bgmType) { this.bgmType = bgmType; }
} 