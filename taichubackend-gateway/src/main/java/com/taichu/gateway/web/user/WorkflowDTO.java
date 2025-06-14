package com.taichu.gateway.web.user;

import lombok.Data;

@Data
public class WorkflowDTO {
    /**
     * 工作流ID
     */
    private Long id;
    /**
     * @see com.taichu.domain.enums.WorkflowStatusEnum
     */
    private String currentStage;
}
