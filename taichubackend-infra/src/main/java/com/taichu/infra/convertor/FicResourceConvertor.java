package com.taichu.infra.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.infra.persistance.model.FicResource;

import java.util.HashMap;
import java.util.Map;

/**
 * 资源对象转换器
 */
public class FicResourceConvertor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static FicResource toDataObject(FicResourceBO bo) {
        if (bo == null) return null;
        FicResource data = new FicResource();
        data.setId(bo.getId());
        data.setGmtCreate(bo.getGmtCreate());
        data.setWorkflowId(bo.getWorkflowId());
        data.setStatus(bo.getStatus());
        data.setRelevanceId(bo.getRelevanceId());
        data.setRelevanceType(bo.getRelevanceType());
        data.setResourceType(bo.getResourceType());
        data.setResourceStorageType(bo.getResourceStorageType());
        data.setResourceUrl(bo.getResourceUrl());
        data.setExtendInfo(originNameToExtendInfo(bo.getOriginName()));
        return data;
    }

    public static FicResourceBO toDomain(FicResource dataObject) {
        if (dataObject == null) return null;
        FicResourceBO bo = new FicResourceBO();
        bo.setId(dataObject.getId());
        bo.setGmtCreate(dataObject.getGmtCreate());
        bo.setWorkflowId(dataObject.getWorkflowId());
        bo.setStatus(dataObject.getStatus());
        bo.setRelevanceId(dataObject.getRelevanceId());
        bo.setRelevanceType(dataObject.getRelevanceType());
        bo.setResourceType(dataObject.getResourceType());
        bo.setResourceStorageType(dataObject.getResourceStorageType());
        bo.setResourceUrl(dataObject.getResourceUrl());
        bo.setOriginName(extendInfoToOriginName(dataObject.getExtendInfo()));
        return bo;
    }

    private static String originNameToExtendInfo(String originName) {
        if (originName == null) {
            return "{}";
        }
        try {
            Map<String, String> extendInfo = new HashMap<>();
            extendInfo.put("originName", originName);
            return OBJECT_MAPPER.writeValueAsString(extendInfo);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String extendInfoToOriginName(String extendInfo) {
        if (extendInfo == null) {
            return null;
        }
        try {
            Map map = OBJECT_MAPPER.readValue(extendInfo, Map.class);
            return (String) map.get("originName");
        } catch (Exception e) {
            return null;
        }
    }
} 