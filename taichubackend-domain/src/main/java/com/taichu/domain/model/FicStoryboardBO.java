package com.taichu.domain.model;

import lombok.Data;

/**
 * 分镜业务对象
 */
@Data
public class FicStoryboardBO {
    /**
     * 分镜ID
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
     * 关联的剧本分片ID
     */
    private Long scriptId;

    /**
     * 展示顺序：关联的剧本分片展示顺序 * 10000 + 自身的展示顺序[1]，自身展示顺序从0开始递增
     */
    private Long orderIndex;

    /**
     * 分镜描述
     */
    private String content;

    /**
     * 扩展字段，jsonObject格式
     */
    private String extendInfo;
} 