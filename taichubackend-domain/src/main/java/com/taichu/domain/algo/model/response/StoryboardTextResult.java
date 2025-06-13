package com.taichu.domain.algo.model.response;

import lombok.Data;

import java.util.List;

/**
 * 分镜文本生成结果
 */
@Data
public class StoryboardTextResult {
    
    /**
     * 分镜文本内容
     */
    private List<String> data;

} 