package com.taichu.infra.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.persistance.model.FicWorkflowTask;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务对象转换器
 */
public class FicWorkflowTaskConvertor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static FicWorkflowTask toDataObject(FicWorkflowTaskBO bo) {
        if (bo == null) return null;
        FicWorkflowTask data = new FicWorkflowTask();
        data.setId(bo.getId());
        data.setGmtCreate(bo.getGmtCreate());
        data.setWorkflowId(bo.getWorkflowId());
        data.setStatus(bo.getStatus());
        data.setTaskType(bo.getTaskType());
        data.setParams(mapToJson(bo.getParams()));
        return data;
    }

    public static FicWorkflowTaskBO toDomain(FicWorkflowTask dataObject) {
        if (dataObject == null) return null;
        FicWorkflowTaskBO bo = new FicWorkflowTaskBO();
        bo.setId(dataObject.getId());
        bo.setGmtCreate(dataObject.getGmtCreate());
        bo.setWorkflowId(dataObject.getWorkflowId());
        bo.setStatus(dataObject.getStatus());
        bo.setTaskType(dataObject.getTaskType());
        bo.setParams(jsonToMap(dataObject.getParams()));
        return bo;
    }

    private static String mapToJson(Map<String, String> params) {
        if (params == null) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static Map<String, String> jsonToMap(String params) {
        if (params == null || params.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(params, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
} 