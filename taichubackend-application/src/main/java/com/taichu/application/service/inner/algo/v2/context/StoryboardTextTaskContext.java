package com.taichu.application.service.inner.algo.v2.context;

import com.taichu.domain.model.FicScriptBO;

/**
 * 分镜文本生成任务上下文
 */
public class StoryboardTextTaskContext extends AlgoTaskContext {
    private FicScriptBO script;
    
    @Override
    public String getTaskSummary() {
        return String.format("Storyboard text generation for script %d in workflow %d", 
            script != null ? script.getId() : 0, getWorkflowId());
    }
    
    // getter/setter
    public FicScriptBO getScript() { return script; }
    public void setScript(FicScriptBO script) { this.script = script; }
} 