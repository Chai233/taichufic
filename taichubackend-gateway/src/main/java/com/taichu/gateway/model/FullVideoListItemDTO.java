package com.taichu.gateway.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "视频对象")
public class FullVideoListItemDTO {
    @ApiModelProperty(value = "展示顺序，由小到大排序")
    private Long orderIndex;

    @ApiModelProperty(value = "视频首帧图片url")
    private String thumbnailUrl;

    @ApiModelProperty(value = "工作流id")
    private Long workflowId;

    @ApiModelProperty(value = "视频资源id")
    private Long storyboardResourceId;
}
