package com.taichu.gateway.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "分镜生成请求参数")
public class GenerateStoryboardRequest {
    @ApiModelProperty(value = "流程id", required = true)
    private Long workflowId;
}
