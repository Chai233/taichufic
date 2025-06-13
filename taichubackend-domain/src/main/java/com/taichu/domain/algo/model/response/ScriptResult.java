package com.taichu.domain.algo.model.response;

import com.taichu.domain.algo.model.common.RoleDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 剧本生成结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptResult extends BaseAlgoResult {
    
    /**
     * 剧本内容
     */
    private String content;
    
    /**
     * 角色列表
     */
    private List<RoleDTO> roles;
} 