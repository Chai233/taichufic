package com.taichu.infra.convertor;

import com.taichu.domain.model.FicResourceBO;
import com.taichu.infra.persistance.model.FicResource;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 资源对象转换器
 */
@Mapper
public interface FicResourceConvertor {
    
    FicResourceConvertor INSTANCE = Mappers.getMapper(FicResourceConvertor.class);

    /**
     * 将业务对象转换为数据对象
     * @param bo 业务对象
     * @return 数据对象
     */
    FicResource toDataObject(FicResourceBO bo);

    /**
     * 将数据对象转换为业务对象
     * @param dataObject 数据对象
     * @return 业务对象
     */
    FicResourceBO toDomain(FicResource dataObject);
} 