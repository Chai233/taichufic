package com.taichu.domain.algo.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分镜文本生成结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StoryboardTextResult extends BaseAlgoResult {
    
    /**
     * 分镜文本内容
     */
    private List<String> data;

} 