package com.taichu.infra.repo;

import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.convertor.FicWorkflowTaskConvertor;
import com.taichu.infra.persistance.mapper.FicWorkflowTaskMapper;
import com.taichu.infra.persistance.model.FicWorkflowTask;
import com.taichu.infra.persistance.model.FicWorkflowTaskExample;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FicWorkflowTaskRepository {

    @Autowired
    private FicWorkflowTaskMapper ficWorkflowTaskMapper;

    public long createFicWorkflowTask(FicWorkflowTaskBO ficWorkflowTask) {
        FicWorkflowTask taskDO = FicWorkflowTaskConvertor.toDataObject(ficWorkflowTask);
        int res = ficWorkflowTaskMapper.insert(taskDO);
        return taskDO.getId();
    }

    public FicWorkflowTaskBO findById(Long workflowTaskId) {
        FicWorkflowTask ficWorkflowTask = ficWorkflowTaskMapper.selectByPrimaryKey(workflowTaskId);
        return FicWorkflowTaskConvertor.toDomain(ficWorkflowTask);
    }

    public List<FicWorkflowTaskBO> findByWorkflowId(Long workflowId) {
        FicWorkflowTaskExample example = new FicWorkflowTaskExample();
        example.createCriteria().andWorkflowIdEqualTo(workflowId);
        List<FicWorkflowTask> taskDOs = ficWorkflowTaskMapper.selectByExample(example);
        return taskDOs.stream().map(FicWorkflowTaskConvertor::toDomain).collect(Collectors.toList());
    }

    public FicWorkflowTaskBO findLatestByWorkflowIdAndTaskType(Long workflowId, List<String> taskType) {
        try {
            FicWorkflowTaskExample example = new FicWorkflowTaskExample();
            example.createCriteria().andWorkflowIdEqualTo(workflowId).andTaskTypeIn(taskType);
            List<FicWorkflowTask> taskDOs = ficWorkflowTaskMapper.selectByExample(example);
            if (taskDOs.isEmpty()) {
                return null;
            }

            FicWorkflowTask task = taskDOs.stream().max(Comparator.comparing(FicWorkflowTask::getGmtCreate)).get();
            return FicWorkflowTaskConvertor.toDomain(task);
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

    /**
     * 更新工作流任务的参数
     * @param id 任务ID
     * @param params 参数Map
     */
    public void updateParams(Long id, java.util.Map<String, String> params) {
        FicWorkflowTask task = new FicWorkflowTask();
        task.setId(id);
        // 将Map转换为JSON字符串
        String paramsJson = FicWorkflowTaskConvertor.mapToJson(params);
        task.setParams(paramsJson);
        ficWorkflowTaskMapper.updateByPrimaryKeySelective(task);
    }

    /**
     * 查找所有运行中的工作流任务
     * @return 运行中的工作流任务列表
     */
    public List<FicWorkflowTaskBO> findRunningTasks() {
        FicWorkflowTaskExample example = new FicWorkflowTaskExample();
        example.createCriteria().andStatusEqualTo(TaskStatusEnum.RUNNING.getCode());
        List<FicWorkflowTask> taskDOs = ficWorkflowTaskMapper.selectByExample(example);
        return taskDOs.stream().map(FicWorkflowTaskConvertor::toDomain).collect(Collectors.toList());
    }

    /**
     * 查找所有工作流任务
     * @return 所有工作流任务列表
     */
    public List<FicWorkflowTaskBO> findAllTasks() {
        FicWorkflowTaskExample example = new FicWorkflowTaskExample();
        List<FicWorkflowTask> taskDOs = ficWorkflowTaskMapper.selectByExample(example);
        return taskDOs.stream().map(FicWorkflowTaskConvertor::toDomain).collect(Collectors.toList());
    }
}
