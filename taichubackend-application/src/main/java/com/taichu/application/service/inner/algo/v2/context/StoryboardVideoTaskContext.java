package com.taichu.application.service.inner.algo.v2.context;

import com.taichu.domain.model.FicRoleBO;
import com.taichu.domain.model.FicStoryboardBO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 分镜视频生成任务上下文
 */
@Setter
@Getter
public class StoryboardVideoTaskContext extends AlgoTaskContext {
    // getter/setter
    private FicStoryboardBO storyboard;
    private List<FicRoleBO> roles;
    private String voiceType;
    private String bgmType;
    
    @Override
    public String getTaskSummary() {
        return String.format("Storyboard video generation for storyboard %d in workflow %d", 
            storyboard != null ? storyboard.getId() : 0, getWorkflowId());
    }

}