package com.taichu.infra.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.persistance.model.FicWorkflowTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务对象转换器
 */
@Mapper
public interface FicWorkflowTaskConvertor {
    
    FicWorkflowTaskConvertor INSTANCE = Mappers.getMapper(FicWorkflowTaskConvertor.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将业务对象转换为数据对象
     * @param bo 业务对象
     * @return 数据对象
     */
    @Mapping(target = "params", source = "params", qualifiedByName = "mapToJson")
    FicWorkflowTask toDataObject(FicWorkflowTaskBO bo);

    /**
     * 将数据对象转换为业务对象
     * @param dataObject 数据对象
     * @return 业务对象
     */
    @Mapping(target = "params", source = "params", qualifiedByName = "jsonToMap")
    FicWorkflowTaskBO toDomain(FicWorkflowTask dataObject);

    /**
     * 将Map转换为JSON字符串
     * @param params Map参数
     * @return JSON字符串
     */
    @Named("mapToJson")
    static String mapToJson(Map<String, String> params) {
        if (params == null) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 将JSON字符串转换为Map
     * @param params JSON字符串
     * @return Map参数
     */
    @Named("jsonToMap")
    static Map<String, String> jsonToMap(String params) {
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