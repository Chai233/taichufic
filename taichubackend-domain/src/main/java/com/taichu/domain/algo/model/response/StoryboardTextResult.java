package com.taichu.domain.algo.model.response;

import com.taichu.domain.algo.model.common.RoleBO;
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
    private String content;
    
    /**
     * 角色列表
     */
    private List<RoleBO> roles;
} 