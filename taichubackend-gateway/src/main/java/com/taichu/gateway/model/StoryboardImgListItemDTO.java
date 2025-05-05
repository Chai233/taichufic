package com.taichu.gateway.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "分镜图对象")
public class StoryboardImgListItemDTO {
    @ApiModelProperty(value = "展示顺序，由小到大排序")
    private Long orderIndex;

    @ApiModelProperty(value = "分镜缩略图url")
    private String thumbnailUrl;

    @ApiModelProperty(value = "分镜图url")
    private String imgUrl;

    @ApiModelProperty(value = "分镜id")
    private Long storyboardId;

    @ApiModelProperty(value = "分镜资源id")
    private Long storyboardResourceId;

}
