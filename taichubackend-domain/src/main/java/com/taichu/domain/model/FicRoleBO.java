package com.taichu.domain.model;

import lombok.Data;

/**
 * 角色业务对象
 */
@Data
public class FicRoleBO {
    /**
     * 角色ID
     */
    private Long id;

    /**
     * 工作流ID
     */
    private Long workflowId;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long gmtCreate;

    /**
     * 状态：1-有效 0-无效
     * @see com.taichu.domain.enums.CommonStatusEnum
     */
    private Byte status;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 提示词
     */
    private String prompt;

    /**
     * 默认角色图片resourceId
     */
    private Long defaultImageResourceId;

} 