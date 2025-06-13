package com.taichu.domain.model;

import lombok.Data;

/**
 * 剧本片段业务对象
 */
@Data
public class FicScriptBO {
    /**
     * 剧本片段ID
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
     * 展示顺序
     */
    private Long orderIndex;

    /**
     * 剧本内容
     */
    private String content;

    /**
     * 扩展字段，jsonObject格式
     */
    private String extendInfo;
} 