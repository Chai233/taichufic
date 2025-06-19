package com.taichu.infra.repo;

import com.taichu.domain.enums.CommonStatusEnum;
import com.taichu.domain.enums.ResourceTypeEnum;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.infra.convertor.FicResourceConvertor;
import com.taichu.infra.persistance.mapper.FicResourceMapper;
import com.taichu.infra.persistance.model.FicResource;
import com.taichu.infra.persistance.model.FicResourceExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 资源仓储类
 */
@Repository
public class FicResourceRepository {

    @Autowired
    private FicResourceMapper resourceMapper;

    /**
     * 创建资源
     *
     * @param resource 资源对象
     * @return 创建的资源ID
     */
    public long insert(FicResourceBO resource) {
        FicResource resourceDO = FicResourceConvertor.toDataObject(resource);
        int res = resourceMapper.insert(resourceDO);
        return (long) res;
    }

    /**
     * 根据ID查询资源
     *
     * @param id 资源ID
     * @return 资源对象
     */
    public FicResourceBO findById(Long id) {
        FicResource resource = resourceMapper.selectByPrimaryKey(id);
        return FicResourceConvertor.toDomain(resource);
    }

    /**
     * 根据工作流ID和资源类型查询资源列表
     *
     * @param workflowId 工作流ID
     * @param resourceType 资源类型
     * @return 资源列表
     */
    public List<FicResourceBO> findByWorkflowIdAndResourceType(Long workflowId, ResourceTypeEnum resourceType) {
        FicResourceExample example = new FicResourceExample();
        example.createCriteria()
                .andWorkflowIdEqualTo(workflowId)
                .andResourceTypeEqualTo(resourceType.name())
                .andStatusEqualTo(CommonStatusEnum.VALID.getValue());
        List<FicResource> resources = resourceMapper.selectByExample(example);
        return resources.stream()
                .map(FicResourceConvertor::toDomain)
                .collect(Collectors.toList());
    }

    public void offlineResourceById(Long id) {
        FicResource resource = resourceMapper.selectByPrimaryKey(id);
        if (resource == null) {
            return;
        }
        resource.setStatus(CommonStatusEnum.INVALID.getValue());
        resourceMapper.updateByPrimaryKey(resource);
    }
} 