package com.taichu.infra.repo;

import com.taichu.domain.model.FicWorkflowMetaBO;
import com.taichu.infra.convertor.FicWorkflowMetaConvertor;
import com.taichu.infra.persistance.mapper.FicWorkflowMetaMapper;
import com.taichu.infra.persistance.model.FicWorkflowMeta;
import com.taichu.infra.persistance.model.FicWorkflowMetaExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FicWorkflowMetaRepository {
    @Autowired
    private FicWorkflowMetaMapper metaMapper;

    public FicWorkflowMetaBO findByWorkflowId(Long workflowId) {
        FicWorkflowMetaExample example = new FicWorkflowMetaExample();
        example.createCriteria().andWorkflowIdEqualTo(workflowId);
        List<FicWorkflowMeta> list = metaMapper.selectByExample(example);
        if (list.isEmpty()) {
            return null;
        }
        return FicWorkflowMetaConvertor.toDomain(list.get(0));
    }

    public void updateStyleType(Long workflowId, String styleType) {
        FicWorkflowMetaExample example = new FicWorkflowMetaExample();
        example.createCriteria().andWorkflowIdEqualTo(workflowId);
        List<FicWorkflowMeta> list = metaMapper.selectByExample(example);
        if (list.isEmpty()) {
            return;
        }
        FicWorkflowMeta meta = list.get(0);
        meta.setStyleType(styleType);
        metaMapper.updateByPrimaryKeySelective(meta);
    }

    public long insert(FicWorkflowMetaBO metaBO) {
        FicWorkflowMeta meta = FicWorkflowMetaConvertor.toDataObject(metaBO);
        metaMapper.insertSelective(meta);
        return meta.getId();
    }
} 