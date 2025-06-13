package com.taichu.infra.convertor;

import com.taichu.domain.model.FicScriptBO;
import com.taichu.infra.persistance.model.FicScript;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 剧本片段对象转换器
 */
@Mapper
public interface FicScriptConvertor {
    FicScriptConvertor INSTANCE = Mappers.getMapper(FicScriptConvertor.class);

    /**
     * 将领域对象转换为数据对象
     *
     * @param scriptBO 领域对象
     * @return 数据对象
     */
    FicScript toDataObject(FicScriptBO scriptBO);

    /**
     * 将数据对象转换为领域对象
     *
     * @param script 数据对象
     * @return 领域对象
     */
    FicScriptBO toDomain(FicScript script);
} 