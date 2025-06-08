package com.taichu.infra.convertor;

import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.persistance.model.FicWorkflowTask;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 任务对象转换器
 */
@Mapper
public interface FicWorkflowTaskConvertor {
    
    FicWorkflowTaskConvertor INSTANCE = Mappers.getMapper(FicWorkflowTaskConvertor.class);

    /**
     * 将业务对象转换为数据对象
     * @param bo 业务对象
     * @return 数据对象
     */
    FicWorkflowTask toDataObject(FicWorkflowTaskBO bo);

    /**
     * 将数据对象转换为业务对象
     * @param dataObject 数据对象
     * @return 业务对象
     */
    FicWorkflowTaskBO toDomain(FicWorkflowTask dataObject);
} 