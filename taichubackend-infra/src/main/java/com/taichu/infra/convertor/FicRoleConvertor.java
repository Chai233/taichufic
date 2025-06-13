package com.taichu.infra.convertor;

import com.taichu.domain.model.FicRoleBO;
import com.taichu.infra.persistance.model.FicRole;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 角色对象转换器
 */
@Mapper
public interface FicRoleConvertor {
    FicRoleConvertor INSTANCE = Mappers.getMapper(FicRoleConvertor.class);

    /**
     * 将领域对象转换为数据对象
     */
    FicRole toDataObject(FicRoleBO bo);

    /**
     * 将数据对象转换为领域对象
     */
    FicRoleBO toDomain(FicRole role);
} 