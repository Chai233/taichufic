package com.taichu.sdk.model.request;

import lombok.Data;

/**
 * 视频合成请求参数
 */
@Data
public class ComposeVideoRequest {
    /**
     * 必填
     * 工作流id
     */
    private Long workflowId;
    /**
     * 必填
     * 旁白配音风格，默认“磁性男声”
     */
    private String voiceType;
    /**
     * 必填
     * bgm风格
     */
    private String bgmType;
}