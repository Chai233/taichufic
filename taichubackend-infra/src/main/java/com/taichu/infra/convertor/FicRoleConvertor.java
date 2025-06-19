package com.taichu.infra.convertor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taichu.domain.model.FicRoleBO;
import com.taichu.infra.persistance.model.FicRole;

/**
 * 角色对象转换器
 */
public class FicRoleConvertor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static FicRole toDataObject(FicRoleBO bo) {
        if (bo == null) return null;
        FicRole data = new FicRole();
        data.setId(bo.getId());
        data.setWorkflowId(bo.getWorkflowId());
        data.setGmtCreate(bo.getGmtCreate());
        data.setStatus(bo.getStatus());
        data.setRoleName(bo.getRoleName());
        data.setDescription(bo.getDescription());
        data.setPrompt(bo.getPrompt());
        data.setExtendInfo(toExtendInfo(bo.getDefaultImageResourceId()));
        return data;
    }

    public static FicRoleBO toDomain(FicRole dataObject) {
        if (dataObject == null) return null;
        FicRoleBO bo = new FicRoleBO();
        bo.setId(dataObject.getId());
        bo.setWorkflowId(dataObject.getWorkflowId());
        bo.setGmtCreate(dataObject.getGmtCreate());
        bo.setStatus(dataObject.getStatus());
        bo.setRoleName(dataObject.getRoleName());
        bo.setDescription(dataObject.getDescription());
        bo.setPrompt(dataObject.getPrompt());
        bo.setDefaultImageResourceId(fromExtendInfo(dataObject.getExtendInfo()));
        return bo;
    }

    private static String toExtendInfo(Long defaultImageResourceId) {
        if (defaultImageResourceId == null) {
            return null;
        }
        try {
            ObjectNode jsonNode = OBJECT_MAPPER.createObjectNode();
            jsonNode.put("defaultImageResourceId", defaultImageResourceId);
            return OBJECT_MAPPER.writeValueAsString(jsonNode);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long fromExtendInfo(String extendInfo) {
        if (extendInfo == null) {
            return null;
        }
        try {
            ObjectNode jsonNode = (ObjectNode) OBJECT_MAPPER.readTree(extendInfo);
            return jsonNode.has("defaultImageResourceId") ? jsonNode.get("defaultImageResourceId").asLong() : null;
        } catch (Exception e) {
            return null;
        }
    }
} 