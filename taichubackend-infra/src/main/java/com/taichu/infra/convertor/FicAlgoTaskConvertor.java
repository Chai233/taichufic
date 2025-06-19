package com.taichu.infra.convertor;

import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.infra.persistance.model.FicAlgoTask;

/**
 * 任务对象转换器
 */
public class FicAlgoTaskConvertor {
    public static FicAlgoTask toDataObject(FicAlgoTaskBO bo) {
        if (bo == null) return null;
        FicAlgoTask data = new FicAlgoTask();
        data.setId(bo.getId());
        data.setGmtCreate(bo.getGmtCreate());
        data.setWorkflowTaskId(bo.getWorkflowTaskId());
        data.setStatus(bo.getStatus());
        data.setTaskType(bo.getTaskType());
        data.setAlgoTaskId(bo.getAlgoTaskId());
        data.setRelevantIdType(bo.getRelevantIdType());
        data.setRelevantId(bo.getRelevantId());
        data.setTaskAbstract(bo.getTaskAbstract());
        return data;
    }

    public static FicAlgoTaskBO toDomain(FicAlgoTask dataObject) {
        if (dataObject == null) return null;
        FicAlgoTaskBO bo = new FicAlgoTaskBO();
        bo.setId(dataObject.getId());
        bo.setGmtCreate(dataObject.getGmtCreate());
        bo.setWorkflowTaskId(dataObject.getWorkflowTaskId());
        bo.setStatus(dataObject.getStatus());
        bo.setTaskType(dataObject.getTaskType());
        bo.setAlgoTaskId(dataObject.getAlgoTaskId());
        bo.setRelevantIdType(dataObject.getRelevantIdType());
        bo.setRelevantId(dataObject.getRelevantId());
        bo.setTaskAbstract(dataObject.getTaskAbstract());
        return bo;
    }
} 