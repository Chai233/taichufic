package com.taichu.gateway.model;

import io.swagger.annotations.ApiModelProperty;

public class TaskStatusDTO {
    @ApiModelProperty(value = "进度百分比", example = "60")
    private Integer percent;
    @ApiModelProperty(value = "剩余等待时间")
    private Integer minutesLeft;
}
