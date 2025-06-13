package com.taichu.infra.repo;

import com.taichu.infra.persistance.mapper.FicWorkflowMapper;
import com.taichu.infra.persistance.model.FicWorkflow;
import com.taichu.infra.persistance.model.FicWorkflowExample;
import com.taichu.infra.repo.query.SingleWorkflowQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流仓储类
 */
@Repository
public class FicWorkflowRepository {

    @Autowired
    private FicWorkflowMapper workflowMapper;

    /**
     * 创建工作流
     *
     * @param workflow 工作流对象
     * @return 创建的工作流ID
     */
    public long insert(FicWorkflow workflow) {
        int res = workflowMapper.insert(workflow);
        return workflow.getId();
    }

    /**
     * 根据条件查询工作流列表
     *
     * @param example 查询条件
     * @return 工作流列表
     */
    public List<FicWorkflow> findByExample(FicWorkflowExample example) {
        return workflowMapper.selectByExample(example);
    }

    /**
     * 查询单个工作流
     *
     * @param query 查询参数
     * @return 工作流
     */
    public Optional<FicWorkflow> findSingleWorkflow(SingleWorkflowQuery query) {
        FicWorkflowExample example = new FicWorkflowExample();
        FicWorkflowExample.Criteria criteria = example.createCriteria();
        
        // 工作流ID是必填的
        criteria.andIdEqualTo(query.getWorkflowId());
        
        // 按需添加其他查询条件
        if (query.getUserId() != null) {
            criteria.andUserIdEqualTo(query.getUserId());
        }
        if (query.getStatus() != null) {
            criteria.andStatusEqualTo(query.getStatus());
        }
        
        List<FicWorkflow> workflows = workflowMapper.selectByExample(example);
        return workflows.isEmpty() ? Optional.empty() : Optional.of(workflows.get(0));
    }

    /**
     * 更新工作流状态
     *
     * @param workflowId 工作流ID
     * @param code 状态码
     */
    public void updateStatus(Long workflowId, Byte code) {
        FicWorkflow workflow = new FicWorkflow();
        workflow.setId(workflowId);
        workflow.setStatus(code);
        workflowMapper.updateByPrimaryKeySelective(workflow);
    }
} 