package com.taichu.infra.convertor;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taichu.domain.model.FicWorkflowMetaBO;
import com.taichu.infra.persistance.model.FicWorkflowMeta;

/**
 * 工作流元数据对象转换器
 */
public class FicWorkflowMetaConvertor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static FicWorkflowMeta toDataObject(FicWorkflowMetaBO bo) {
        if (bo == null) return null;
        FicWorkflowMeta data = new FicWorkflowMeta();
        data.setId(bo.getId());
        data.setWorkflowId(bo.getWorkflowId());
        data.setStyleType(bo.getStyleType());
        data.setStoryName(bo.getStoryName());
        data.setStoryInfo(bo.getStoryInfo());
        data.setExtInfo(toExtInfo(bo));
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
        bo.setUserPrompt(fromExtInfo(entity.getExtInfo()));
        return bo;
    }

    private static String toExtInfo(FicWorkflowMetaBO bo) {
        if (bo == null || bo.getUserPrompt() == null) {
            return null;
        }
        try {
            ObjectNode jsonNode = OBJECT_MAPPER.createObjectNode();
            jsonNode.put("userPrompt", bo.getUserPrompt());
            return OBJECT_MAPPER.writeValueAsString(jsonNode);
        } catch (Exception e) {
            return null;
        }
    }

    private static String fromExtInfo(String extInfo) {
        if (extInfo == null) {
            return null;
        }
        try {
            ObjectNode jsonNode = (ObjectNode) OBJECT_MAPPER.readTree(extInfo);
            return jsonNode.has("userPrompt") ? jsonNode.get("userPrompt").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
} 