package com.taichu.gateway.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class SingleStoryboardRegenRequest {
    @ApiModelProperty(value = "分镜id", required = true)
    private Long storyboardId;
    @ApiModelProperty(value = "文本引导强度", required = false)
    private Long paramWenbenyindaoqiangdu;
    @ApiModelProperty(value = "风格强度", required = false)
    private Long paramFenggeqiangdu;
    @ApiModelProperty(value = "用户自定义prompt", required = false)
    private String userPrompt;
    @ApiModelProperty(value = "用户选定图片范围", required = false)
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
