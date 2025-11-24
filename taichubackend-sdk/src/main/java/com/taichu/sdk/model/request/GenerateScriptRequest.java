package com.taichu.sdk.model.request;

import lombok.Data;

/**
 * 剧本生成请求参数
 */
@Data
public class GenerateScriptRequest {
    /**
     * 流程id
     */
    private Long workflowId;
    /**
     * 用户自定义prompt
     */
    private String userPrompt;
    /**
     * 标签：赛博朋克/外星文明
     * 默认赛博朋克
     */
    private String tag = "赛博朋克";
}
