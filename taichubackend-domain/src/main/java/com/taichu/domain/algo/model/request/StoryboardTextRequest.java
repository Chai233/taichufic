package com.taichu.domain.algo.model.request;

import lombok.Data;

@Data
public class StoryboardTextRequest {
    private String script;      // 剧本片段
    private String workflow_id;  // 工作流ID
} 