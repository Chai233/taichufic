package com.taichu.infra.convertor;

import com.taichu.domain.model.FicScriptBO;
import com.taichu.infra.persistance.model.FicScript;

/**
 * 剧本片段对象转换器
 */
public class FicScriptConvertor {
    public static FicScript toDataObject(FicScriptBO bo) {
        if (bo == null) return null;
        FicScript data = new FicScript();
        data.setId(bo.getId());
        data.setWorkflowId(bo.getWorkflowId());
        data.setGmtCreate(bo.getGmtCreate());
        data.setStatus(bo.getStatus());
        data.setOrderIndex(bo.getOrderIndex());
        data.setContent(bo.getContent());
        data.setExtendInfo(bo.getExtendInfo());
        return data;
    }

    public static FicScriptBO toDomain(FicScript dataObject) {
        if (dataObject == null) return null;
        FicScriptBO bo = new FicScriptBO();
        bo.setId(dataObject.getId());
        bo.setWorkflowId(dataObject.getWorkflowId());
        bo.setGmtCreate(dataObject.getGmtCreate());
        bo.setStatus(dataObject.getStatus());
        bo.setOrderIndex(dataObject.getOrderIndex());
        bo.setContent(dataObject.getContent());
        bo.setExtendInfo(dataObject.getExtendInfo());
        return bo;
    }
} 