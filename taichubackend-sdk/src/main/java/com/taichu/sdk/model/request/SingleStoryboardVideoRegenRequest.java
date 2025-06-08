package com.taichu.sdk.model.request;

import lombok.Data;

/**
 * 单分镜视频重生成请求
 */
@Data
public class SingleStoryboardVideoRegenRequest {
    /**
     * 分镜id
     */
    private Long storyboardId;
    /**
     * 文本引导强度
     */
    private Long paramWenbenyindaoqiangdu;
    /**
     * 风格强度
     */
    private Long paramFenggeqiangdu;
    /**
     * 用户自定义prompt
     */
    private String userPrompt;
}
