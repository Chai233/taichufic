package com.taichu.infra.repo.query;

import lombok.Builder;
import lombok.Data;

/**
 * 工作流单条查询参数
 */
@Data
@Builder
public class SingleWorkflowQuery {
    /**
     * 工作流ID
     */
    private Long workflowId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 状态
     */
    private Byte status;
} 