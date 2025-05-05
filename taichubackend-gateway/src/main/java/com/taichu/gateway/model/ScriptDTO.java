package com.taichu.gateway.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "剧本分片对象")
public class ScriptDTO {
    @ApiModelProperty(value = "剧本分片顺序（从0开始）")
    private Long order;
    @ApiModelProperty(value = "剧本内容")
    private String scriptContent;
}
