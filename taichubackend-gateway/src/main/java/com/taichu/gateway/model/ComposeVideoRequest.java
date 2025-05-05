package com.taichu.gateway.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "视频合成请求参数")
public class ComposeVideoRequest {
    @ApiModelProperty(value = "流程id", required = true)
    private Long workflowId;
}