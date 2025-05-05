package com.taichu.gateway.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel
public class StoryboardTaskStatusDTO extends TaskStatusDTO {

    @ApiModelProperty(value = "已完成的分镜id")
    private List<Long> completedStoryboardIds;

    @ApiModelProperty(value = "已完成的分镜数")
    private Integer completeCnt;

    @ApiModelProperty(value = "分镜/视频总数")
    private Integer totalCnt;
}
