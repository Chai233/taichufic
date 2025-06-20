package com.taichu.sdk.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分镜任务状态
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StoryboardWorkflowTaskStatusDTO extends WorkflowTaskStatusDTO {
    /**
     * 已完成的分镜id
     */
    private List<Long> completedStoryboardIds;
    /**
     * 已完成的分镜数
     */
    private Integer completeCnt;
    /**
     * 分镜/视频总数
     */
    private Integer totalCnt;
}
