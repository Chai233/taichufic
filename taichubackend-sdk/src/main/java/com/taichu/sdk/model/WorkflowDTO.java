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
     * 工作流状态
     */
    private String status;
}
