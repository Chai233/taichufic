package com.taichu.infra.convertor;

import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.infra.persistance.model.FicAlgoTask;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 任务对象转换器
 */
@Mapper
public interface FicAlgoTaskConvertor {
    
    FicAlgoTaskConvertor INSTANCE = Mappers.getMapper(FicAlgoTaskConvertor.class);

    /**
     * 将业务对象转换为数据对象
     * @param bo 业务对象
     * @return 数据对象
     */
    FicAlgoTask toDataObject(FicAlgoTaskBO bo);

    /**
     * 将数据对象转换为业务对象
     * @param dataObject 数据对象
     * @return 业务对象
     */
    FicAlgoTaskBO toDomain(FicAlgoTask dataObject);
} 