package com.taichu.sdk.model.request;

import lombok.Data;

/**
 * 视频合成请求参数
 */
@Data
public class ComposeVideoRequest {
    /**
     * 流程id
     */
    private Long workflowId;
}