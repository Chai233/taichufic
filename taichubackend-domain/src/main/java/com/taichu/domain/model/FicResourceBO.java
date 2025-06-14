package com.taichu.domain.model;

import com.taichu.domain.enums.RelevanceType;
import lombok.Data;

/**
 * 资源业务对象
 */
@Data
public class FicResourceBO {
    /**
     * 资源ID
     */
    private Long id;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long gmtCreate;

    /**
     * 工作流ID
     */
    private Long workflowId;

    /**
     * 状态：1-有效 0-无效
     * @see com.taichu.domain.enums.CommonStatusEnum
     */
    private Byte status;

    /**
     * 关联键
     */
    private Long relevanceId;

    /**
     * 关联键类型
     * @see RelevanceType
     */
    private String relevanceType;

    /**
     * 资源类型
     * @see com.taichu.domain.enums.ResourceTypeEnum
     */
    private String resourceType;

    /**
     * 存储方式
     * @see com.taichu.domain.enums.ResourceStorageTypeEnum
     */
    private String resourceStorageType;

    /**
     * 存储路径
     */
    private String resourceUrl;

    /**
     * 算法返回时的原始文件名称
     */
    private String orginName;

} 