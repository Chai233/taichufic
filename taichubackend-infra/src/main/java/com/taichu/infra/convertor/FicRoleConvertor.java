package com.taichu.infra.convertor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taichu.domain.model.FicRoleBO;
import com.taichu.infra.persistance.model.FicRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * 角色对象转换器
 */
@Mapper
public interface FicRoleConvertor {
    FicRoleConvertor INSTANCE = Mappers.getMapper(FicRoleConvertor.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将领域对象转换为数据对象
     */
    @Mapping(target = "extendInfo", source = "defaultImageResourceId", qualifiedByName = "toExtendInfo")
    FicRole toDataObject(FicRoleBO bo);

    /**
     * 将数据对象转换为领域对象
     */
    @Mapping(target = "defaultImageResourceId", source = "extendInfo", qualifiedByName = "fromExtendInfo")
    FicRoleBO toDomain(FicRole role);

    /**
     * 将 defaultImageResourceId 转换为 extendInfo JSON 字符串
     */
    @Named("toExtendInfo")
    default String toExtendInfo(Long defaultImageResourceId) {
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

    /**
     * 从 extendInfo JSON 字符串中提取 defaultImageResourceId
     */
    @Named("fromExtendInfo")
    default Long fromExtendInfo(String extendInfo) {
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