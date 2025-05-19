package com.taichu.domain.model;

import lombok.Data;

/**
 * 算法服务结果
 */
@Data
public class AlgoResult {
    /**
     * 剧本内容
     */
    private String scriptContent;

    /**
     * 分镜内容
     */
    private String storyboardContent;
} 