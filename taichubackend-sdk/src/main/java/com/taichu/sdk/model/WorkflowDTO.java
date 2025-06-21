package com.taichu.sdk.model;

import lombok.Data;

/**
 * 工作流数据传输对象
 */
@Data
public class WorkflowDTO {
    /**
     * 工作流ID
     */
    private Long workflowId;
    /**
     * 当前工作流状态
     * @see com.taichu.sdk.constant.WorkflowPageEnum
     */
    private String currentWorkflowPage;
    /**
     * 剧本生成标签
     */
    private String tag;
    /**
     * 上次用户提交的剧本生成prompt
     */
    private String scripUserPrompt;
    /**
     * 工作流状态
     */
    private String status;

    /**
     * 当前执行的任务id
     * 没有执行中的任务id则字段为空
     */
    private Long currentRunningTaskId;
    /**
     * 当前执行的任务状态
     * @see com.taichu.sdk.constant.WorkflowTaskTypeEnum
     */
    private String currentRunningTaskType;
}
