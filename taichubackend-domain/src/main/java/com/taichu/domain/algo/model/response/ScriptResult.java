package com.taichu.domain.algo.model.response;

import com.taichu.domain.algo.model.common.RoleDTO;
import lombok.Data;

import java.util.List;

/**
 * 剧本生成结果
 */
@Data
public class ScriptResult {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 剧本内容列表
     */
    private List<String> scripts;
    
    /**
     * 角色列表
     */
    private List<RoleDTO> roles;
} 