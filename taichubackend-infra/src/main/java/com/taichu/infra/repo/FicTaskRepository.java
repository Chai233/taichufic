package com.taichu.infra.repo;

import com.taichu.domain.model.FicTaskBO;
import com.taichu.infra.convertor.FicTaskConvertor;
import com.taichu.infra.persistance.mapper.FicTaskMapper;
import com.taichu.infra.persistance.model.FicTask;
import com.taichu.infra.persistance.model.FicTaskExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<FicTaskBO> findByWorkflowIdAndStatus(Long workflowId, Byte status) {
        FicTaskExample example = new FicTaskExample();
        example.createCriteria()
                .andWorkflowIdEqualTo(workflowId)
                .andStatusEqualTo(status);
        return taskMapper.selectByExample(example).stream()
                .map(FicTaskConvertor.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 保存任务
     * @param task 任务
     * @return 保存后的任务
     */
    public FicTaskBO save(FicTaskBO task) {
        FicTask taskDO = FicTaskConvertor.INSTANCE.toDataObject(task);
        taskMapper.insert(taskDO);
        return FicTaskConvertor.INSTANCE.toDomain(taskDO);
    }

    /**
     * 更新任务
     * @param task 任务
     */
    public void update(FicTaskBO task) {
        FicTask taskDO = FicTaskConvertor.INSTANCE.toDataObject(task);
        taskMapper.updateByPrimaryKey(taskDO);
    }

    /**
     * 根据工作流ID和任务类型查询任务
     * @param workflowId 工作流ID
     * @param taskType 任务类型
     * @return 任务
     */
    public FicTaskBO findByWorkflowIdAndTaskType(Long workflowId, String taskType) {
        FicTaskExample example = new FicTaskExample();
        example.createCriteria()
                .andWorkflowIdEqualTo(workflowId)
                .andTaskTypeEqualTo(taskType);
        List<FicTask> tasks = taskMapper.selectByExample(example);
        if (tasks.isEmpty()) {
            return null;
        }
        return FicTaskConvertor.INSTANCE.toDomain(tasks.get(0));
    }

    /**
     * 根据ID查询任务
     * @param id 任务ID
     * @return 任务
     */
    public FicTaskBO findById(Long id) {
        FicTask taskDO = taskMapper.selectByPrimaryKey(id);
        return taskDO == null ? null : FicTaskConvertor.INSTANCE.toDomain(taskDO);
    }
} 