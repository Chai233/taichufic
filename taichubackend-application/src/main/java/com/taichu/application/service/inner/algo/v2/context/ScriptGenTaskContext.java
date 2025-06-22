package com.taichu.application.service.inner.algo.v2.context;

import com.taichu.domain.model.FicResourceBO;

import java.util.List;

/**
 * 剧本生成任务上下文
 */
public class ScriptGenTaskContext extends AlgoTaskContext {
    private List<FicResourceBO> novelFiles;
    private String userPrompt;
    
    @Override
    public String getTaskSummary() {
        return String.format("Script generation for workflow %d with %d novel files", 
            getWorkflowId(), novelFiles != null ? novelFiles.size() : 0);
    }
    
    // getter/setter
    public List<FicResourceBO> getNovelFiles() { return novelFiles; }
    public void setNovelFiles(List<FicResourceBO> novelFiles) { this.novelFiles = novelFiles; }
    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
} 