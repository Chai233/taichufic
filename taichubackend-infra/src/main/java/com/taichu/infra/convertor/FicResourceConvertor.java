package com.taichu.infra.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.infra.persistance.model.FicResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

/**
 * 资源对象转换器
 */
@Mapper
public interface FicResourceConvertor {
    
    FicResourceConvertor INSTANCE = Mappers.getMapper(FicResourceConvertor.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将业务对象转换为数据对象
     * @param bo 业务对象
     * @return 数据对象
     */
    @Mapping(target = "extendInfo", source = "orginName", qualifiedByName = "originNameToExtendInfo")
    FicResource toDataObject(FicResourceBO bo);

    /**
     * 将数据对象转换为业务对象
     * @param dataObject 数据对象
     * @return 业务对象
     */
    @Mapping(target = "orginName", source = "extendInfo", qualifiedByName = "extendInfoToOriginName")
    FicResourceBO toDomain(FicResource dataObject);

    /**
     * 将原始文件名转换为扩展信息JSON
     * @param originName 原始文件名
     * @return JSON格式的扩展信息
     */
    @Named("originNameToExtendInfo")
    default String originNameToExtendInfo(String originName) {
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

    /**
     * 从扩展信息JSON中提取原始文件名
     * @param extendInfo JSON格式的扩展信息
     * @return 原始文件名
     */
    @Named("extendInfoToOriginName")
    default String extendInfoToOriginName(String extendInfo) {
        if (extendInfo == null) {
            return null;
        }
        try {
            Map<String, String> map = OBJECT_MAPPER.readValue(extendInfo, Map.class);
            return map.get("originName");
        } catch (JsonProcessingException e) {
            return null;
        }
    }
} 