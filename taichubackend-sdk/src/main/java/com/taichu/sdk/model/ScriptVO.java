package com.taichu.sdk.model;

import lombok.Data;

/**
 * 剧本分片对象
 */
@Data
public class ScriptVO {
    /**
     * 剧本分片顺序（从0开始）
     */
    private Long order;
    /**
     * 剧本内容
     */
    private String scriptContent;
}
