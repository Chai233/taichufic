package com.taichu.sdk.model.request;

import lombok.Data;

/**
 * 单分镜重生成请求
 */
@Data
public class GenerateStoryboardImgRequest {

    /**
     * 工作流Id
     */
    private Long workflowId;

    /**
     * 分镜id
     */
    private Long storyboardId;
    /**
     * 文本引导强度(1-100)
     */
    private Long scale;
    /**
     * 风格强度(1-100)
     */
    private Long styleScale;
    /**
     * 用户自定义prompt
     */
    private String userPrompt;

    /**
     * 图片风格
     */
    private String imageStyle;

    /**
     * 用户选定图片范围
     */
    private BoxDTO selectBox;

    @Data
    static class BoxDTO {
        /**
         * 框选范围左上角横坐标（以图片左上角为原点）
         */
        private Long x;
        /**
         * 框选范围左上角纵坐标（以图片左上角为原点）
         */
        private Long y;
        /**
         * 框选范围宽度
         */
        private Long width;
        /**
         * 框选范围高度
         */
        private Long height;
    }
}
