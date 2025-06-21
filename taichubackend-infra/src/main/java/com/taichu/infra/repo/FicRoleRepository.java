package com.taichu.infra.repo;

import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.enums.CommonStatusEnum;
import com.taichu.domain.model.FicRoleBO;
import com.taichu.infra.convertor.FicRoleConvertor;
import com.taichu.infra.persistance.mapper.FicRoleMapper;
import com.taichu.infra.persistance.model.FicRole;
import com.taichu.infra.persistance.model.FicRoleExample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色仓储类
 */
@Repository
public class FicRoleRepository {

    @Autowired
    private FicRoleMapper roleMapper;

    /**
     * 创建角色
     *
     * @param role 角色对象
     * @return 创建的角色ID
     */
    public long insert(FicRoleBO role) {
        FicRole roleDO = FicRoleConvertor.toDataObject(role);
        int res = roleMapper.insert(roleDO);
        return roleDO.getId();
    }

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 角色对象
     */
    public FicRoleBO findById(Long id) {
        FicRole role = roleMapper.selectByPrimaryKey(id);
        return FicRoleConvertor.toDomain(role);
    }

    /**
     * 根据workflowId查询角色列表
     * @param workflowId 工作流ID
     * @return 角色列表
     */
    public List<FicRoleBO> findByWorkflowId(Long workflowId) {
        FicRoleExample example = new FicRoleExample();
        example.createCriteria()
                .andWorkflowIdEqualTo(workflowId)
                .andStatusEqualTo(CommonStatusEnum.VALID.getValue());
        List<FicRole> roleDOs = roleMapper.selectByExampleWithBLOBs(example);
        return StreamUtil.toStream(roleDOs)
                .map(FicRoleConvertor::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 下线指定工作流的所有有效角色
     * @param workflowId 工作流ID
     */
    public void offlineByWorkflowId(Long workflowId) {
        // 先查询所有需要下线的角色
        List<FicRoleBO> roles = findByWorkflowId(workflowId);
        
        // 逐个更新角色状态为无效
        for (FicRoleBO role : roles) {
            FicRole roleDO = new FicRole();
            roleDO.setId(role.getId());
            roleDO.setStatus(CommonStatusEnum.INVALID.getValue());
            roleMapper.updateByPrimaryKeySelective(roleDO);
        }
    }

    /**
     * 更新角色
     * @param role
     */
    public void update(FicRoleBO role) {
        FicRole roleDO = FicRoleConvertor.toDataObject(role);
        roleMapper.updateByPrimaryKeySelective(roleDO);
    }
}