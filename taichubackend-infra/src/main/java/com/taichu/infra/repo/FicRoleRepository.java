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
        FicRole roleDO = FicRoleConvertor.INSTANCE.toDataObject(role);
        int res = roleMapper.insert(roleDO);
        return (long) res;
    }

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 角色对象
     */
    public FicRoleBO findById(Long id) {
        FicRole role = roleMapper.selectByPrimaryKey(id);
        return FicRoleConvertor.INSTANCE.toDomain(role);
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
        List<FicRole> roleDOs = roleMapper.selectByExample(example);
        return StreamUtil.toStream(roleDOs)
                .map(FicRoleConvertor.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }
} 