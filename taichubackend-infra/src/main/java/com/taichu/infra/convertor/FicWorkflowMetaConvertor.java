package com.taichu.infra.convertor;

import com.taichu.domain.model.FicWorkflowMetaBO;
import com.taichu.infra.persistance.model.FicWorkflowMeta;

/**
 * 工作流元数据对象转换器
 */
public class FicWorkflowMetaConvertor {
    public static FicWorkflowMeta toDataObject(FicWorkflowMetaBO bo) {
        if (bo == null) return null;
        FicWorkflowMeta data = new FicWorkflowMeta();
        data.setId(bo.getId());
        data.setWorkflowId(bo.getWorkflowId());
        data.setStyleType(bo.getStyleType());
        data.setStoryName(bo.getStoryName());
        data.setStoryInfo(bo.getStoryInfo());
        return data;
    }

    public static FicWorkflowMetaBO toDomain(FicWorkflowMeta entity) {
        if (entity == null) return null;
        FicWorkflowMetaBO bo = new FicWorkflowMetaBO();
        bo.setId(entity.getId());
        bo.setWorkflowId(entity.getWorkflowId());
        bo.setStyleType(entity.getStyleType());
        bo.setStoryName(entity.getStoryName());
        bo.setStoryInfo(entity.getStoryInfo());
        return bo;
    }
} 