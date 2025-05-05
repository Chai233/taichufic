package com.taichu.gateway.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class SingleStoryboardVideoRegenRequest {
    @ApiModelProperty(value = "分镜id", required = true)
    private Long storyboardId;
    @ApiModelProperty(value = "文本引导强度", required = false)
    private Long paramWenbenyindaoqiangdu;
    @ApiModelProperty(value = "风格强度", required = false)
    private Long paramFenggeqiangdu;
    @ApiModelProperty(value = "用户自定义prompt", required = false)
    private String userPrompt;
}
