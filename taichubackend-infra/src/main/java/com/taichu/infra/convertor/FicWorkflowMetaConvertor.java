package com.taichu.infra.convertor;

import com.taichu.domain.model.FicWorkflowMetaBO;
import com.taichu.infra.persistance.model.FicWorkflowMeta;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 工作流元数据对象转换器
 */
@Mapper
public interface FicWorkflowMetaConvertor {
    FicWorkflowMetaConvertor INSTANCE = Mappers.getMapper(FicWorkflowMetaConvertor.class);

    FicWorkflowMeta toDataObject(FicWorkflowMetaBO bo);
    FicWorkflowMetaBO toDomain(FicWorkflowMeta entity);
} 