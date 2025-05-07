package com.taichu.gateway.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "剧本生成请求参数")
public class GenerateScriptRequest {

    @ApiModelProperty(value = "流程id", required = true)
    private Long workflowId;
    @ApiModelProperty(value = "用户自定义prompt")
    private String userPrompt;
    @ApiModelProperty(value = "标签：赛博朋克/外星文明")
    private String tag;
}
