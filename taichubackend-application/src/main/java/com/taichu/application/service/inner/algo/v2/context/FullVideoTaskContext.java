package com.taichu.application.service.inner.algo.v2.context;

import com.taichu.domain.model.FicStoryboardBO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 完整视频生成任务上下文
 */
@Setter
@Getter
public class FullVideoTaskContext extends AlgoTaskContext {
    // getter/setter
    private List<FicStoryboardBO> storyboards;
    private String voiceType;
    private String bgmType;
    
    @Override
    public String getTaskSummary() {
        return String.format("Full video generation for workflow %d with %d storyboards", 
            getWorkflowId(), storyboards != null ? storyboards.size() : 0);
    }

}