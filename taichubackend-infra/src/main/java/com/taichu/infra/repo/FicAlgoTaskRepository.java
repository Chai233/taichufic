package com.taichu.infra.repo;

import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.infra.convertor.FicAlgoTaskConvertor;
import com.taichu.infra.persistance.mapper.FicAlgoTaskMapper;
import com.taichu.infra.persistance.model.FicAlgoTask;
import com.taichu.infra.persistance.model.FicAlgoTaskExample;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务仓储类
 */
@Repository
@Slf4j
public class FicAlgoTaskRepository {

    @Autowired
    private FicAlgoTaskMapper taskMapper;

    /**
     * 保存任务
     * @param task 任务
     * @return 保存后的任务
     */
    public FicAlgoTaskBO save(FicAlgoTaskBO task) {
        FicAlgoTask taskDO = FicAlgoTaskConvertor.toDataObject(task);
        taskMapper.insert(taskDO);
        return FicAlgoTaskConvertor.toDomain(taskDO);
    }

    /**
     * 更新任务
     *
     * @param taskId
     * @param taskStatus
     * @return
     */
    public boolean updateStatus(Long taskId, TaskStatusEnum taskStatus) {
        FicAlgoTask taskDO = new FicAlgoTask();
        taskDO.setId(taskId);
        taskDO.setStatus(taskStatus.getCode());
        int cnt = taskMapper.updateByPrimaryKeySelective(taskDO);

        return cnt == 1;
    }

    /**
     * 根据ID查询任务
     * @param id 任务ID
     * @return 任务
     */
    public FicAlgoTaskBO findById(Long id) {
        FicAlgoTask taskDO = taskMapper.selectByPrimaryKey(id);
        return taskDO == null ? null : FicAlgoTaskConvertor.toDomain(taskDO);
    }

    /**
     * 根据工作流任务ID查询算法任务列表
     * @param workflowTaskId 工作流任务ID
     * @return 算法任务列表
     */
    public List<FicAlgoTaskBO> findByWorkflowTaskId(Long workflowTaskId) {
        FicAlgoTaskExample example = new FicAlgoTaskExample();
        example.createCriteria().andWorkflowTaskIdEqualTo(workflowTaskId);
        List<FicAlgoTask> taskDOs = taskMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(taskDOs)) {
            log.info("findByWorkflowTaskId - 工作流任务ID: {}, 未找到算法任务", workflowTaskId);
            return null;
        }
        return taskDOs.stream().map(FicAlgoTaskConvertor::toDomain).collect(Collectors.toList());
    }

    public List<FicAlgoTaskBO> findByWorkflowTaskIdAndTaskType(Long workflowTaskId, AlgoTaskTypeEnum taskType) {
        FicAlgoTaskExample example = new FicAlgoTaskExample();
        example.createCriteria()
                .andWorkflowTaskIdEqualTo(workflowTaskId)
                .andTaskTypeEqualTo(taskType.name())
        ;
        List<FicAlgoTask> taskDOs = taskMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(taskDOs)) {
            log.info("findByWorkflowTaskIdAndTaskType - 工作流任务ID: {}, 算法任务类型: {}, 未找到算法任务", workflowTaskId, taskType);
            return null;
        }
        return taskDOs.stream().map(FicAlgoTaskConvertor::toDomain).collect(Collectors.toList());
    }

    public void saveAll(List<FicAlgoTaskBO> algoTasks) {
        List<FicAlgoTask> taskDOs = algoTasks.stream().map(FicAlgoTaskConvertor::toDataObject).collect(Collectors.toList());
        for (FicAlgoTask ficAlgoTask : taskDOs) {
            taskMapper.insert(ficAlgoTask);
        }
    }
}