package com.taichu.infra.convertor;

import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.infra.persistance.model.FicStoryboard;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 分镜对象转换器
 */
@Mapper
public interface FicStoryboardConvertor {
    
    FicStoryboardConvertor INSTANCE = Mappers.getMapper(FicStoryboardConvertor.class);

    /**
     * 将业务对象转换为数据对象
     * @param bo 业务对象
     * @return 数据对象
     */
    FicStoryboard toDataObject(FicStoryboardBO bo);

    /**
     * 将数据对象转换为业务对象
     * @param dataObject 数据对象
     * @return 业务对象
     */
    FicStoryboardBO toDomain(FicStoryboard dataObject);
} 