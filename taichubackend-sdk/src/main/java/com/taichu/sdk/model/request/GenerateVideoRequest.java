package com.taichu.sdk.model.request;

import lombok.Data;

/**
 * 分镜视频生成请求参数
 */
@Data
public class GenerateVideoRequest {
    /**
     * 流程id
     */
    private Long workflowId;
}
