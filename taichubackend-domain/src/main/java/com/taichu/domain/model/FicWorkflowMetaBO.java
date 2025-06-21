package com.taichu.domain.model;

import lombok.Data;

/**
 * 工作流元数据业务对象
 */
@Data
public class FicWorkflowMetaBO {
    /** 主键ID */
    private Long id;
    /** 工作流ID */
    private Long workflowId;
    /** 风格类型 */
    private String styleType;
    /** 故事名称 */
    private String storyName;
    /** 故事简介 */
    private String storyInfo;

    private String userPrompt;
} 