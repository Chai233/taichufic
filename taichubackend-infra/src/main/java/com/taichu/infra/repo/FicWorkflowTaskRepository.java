package com.taichu.infra.repo;

import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.convertor.FicWorkflowTaskConvertor;
import com.taichu.infra.persistance.mapper.FicWorkflowTaskMapper;
import com.taichu.infra.persistance.model.FicWorkflowTask;
import com.taichu.infra.persistance.model.FicWorkflowTaskExample;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FicWorkflowTaskRepository {

    @Autowired
    private FicWorkflowTaskMapper ficWorkflowTaskMapper;

    public long createFicWorkflowTask(FicWorkflowTaskBO ficWorkflowTask) {
        FicWorkflowTask taskDO = FicWorkflowTaskConvertor.INSTANCE.toDataObject(ficWorkflowTask);
        int res = ficWorkflowTaskMapper.insert(taskDO);
        return (long) res;
    }

    public FicWorkflowTaskBO findById(Long workflowTaskId) {
        FicWorkflowTask ficWorkflowTask = ficWorkflowTaskMapper.selectByPrimaryKey(workflowTaskId);
        return FicWorkflowTaskConvertor.INSTANCE.toDomain(ficWorkflowTask);
    }

    public List<FicWorkflowTaskBO> findByWorkflowId(Long workflowId) {
        FicWorkflowTaskExample example = new FicWorkflowTaskExample();
        example.createCriteria().andWorkflowIdEqualTo(workflowId);
        List<FicWorkflowTask> taskDOs = ficWorkflowTaskMapper.selectByExample(example);
        return taskDOs.stream().map(FicWorkflowTaskConvertor.INSTANCE::toDomain).collect(Collectors.toList());
    }

    public FicWorkflowTaskBO findByWorkflowIdAndTaskType(Long workflowId, String taskType) {
        try {
            FicWorkflowTaskExample example = new FicWorkflowTaskExample();
            example.createCriteria().andWorkflowIdEqualTo(workflowId).andTaskTypeEqualTo(taskType);
            List<FicWorkflowTask> taskDOs = ficWorkflowTaskMapper.selectByExample(example);
            if (taskDOs.isEmpty()) {
                return null;
            }
            return FicWorkflowTaskConvertor.INSTANCE.toDomain(taskDOs.get(0));
        } catch (Exception e) {
            // log.error("查询工作流任务失败, workflowId={}, taskType={}", workflowId, taskType, e);
            return null;
        }
    }

    public void updateTaskStatus(Long id, TaskStatusEnum status) {
        FicWorkflowTask task = new FicWorkflowTask();
        task.setId(id);
        task.setStatus(status.getCode());
        ficWorkflowTaskMapper.updateByPrimaryKeySelective(task);
    }
}
