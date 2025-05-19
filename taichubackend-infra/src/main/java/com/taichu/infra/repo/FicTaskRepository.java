package com.taichu.infra.repo;

import com.taichu.infra.persistance.mapper.FicTaskMapper;
import com.taichu.infra.persistance.model.FicTask;
import com.taichu.infra.persistance.model.FicTaskExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 任务仓储类
 */
@Repository
public class FicTaskRepository {

    @Autowired
    private FicTaskMapper taskMapper;

    /**
     * 根据工作流ID和状态查询任务
     *
     * @param workflowId 工作流ID
     * @param status 状态
     * @return 任务列表
     */
    public List<FicTask> findByWorkflowIdAndStatus(Long workflowId, Byte status) {
        FicTaskExample example = new FicTaskExample();
        example.createCriteria()
                .andWorkflowIdEqualTo(workflowId)
                .andStatusEqualTo(status);
        return taskMapper.selectByExample(example);
    }
} 