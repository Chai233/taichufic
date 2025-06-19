package com.taichu.infra.convertor;

import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.infra.persistance.model.FicStoryboard;

/**
 * 分镜对象转换器
 */
public class FicStoryboardConvertor {
    public static FicStoryboard toDataObject(FicStoryboardBO bo) {
        if (bo == null) return null;
        FicStoryboard data = new FicStoryboard();
        data.setId(bo.getId());
        data.setWorkflowId(bo.getWorkflowId());
        data.setGmtCreate(bo.getGmtCreate());
        data.setStatus(bo.getStatus());
        data.setScriptId(bo.getScriptId());
        data.setOrderIndex(bo.getOrderIndex());
        data.setContent(bo.getContent());
        data.setExtendInfo(bo.getExtendInfo());
        return data;
    }

    public static FicStoryboardBO toDomain(FicStoryboard dataObject) {
        if (dataObject == null) return null;
        FicStoryboardBO bo = new FicStoryboardBO();
        bo.setId(dataObject.getId());
        bo.setWorkflowId(dataObject.getWorkflowId());
        bo.setGmtCreate(dataObject.getGmtCreate());
        bo.setStatus(dataObject.getStatus());
        bo.setScriptId(dataObject.getScriptId());
        bo.setOrderIndex(dataObject.getOrderIndex());
        bo.setContent(dataObject.getContent());
        bo.setExtendInfo(dataObject.getExtendInfo());
        return bo;
    }
} 