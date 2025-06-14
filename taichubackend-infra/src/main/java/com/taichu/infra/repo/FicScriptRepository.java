package com.taichu.infra.repo;

import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.enums.CommonStatusEnum;
import com.taichu.domain.model.FicScriptBO;
import com.taichu.infra.convertor.FicScriptConvertor;
import com.taichu.infra.persistance.mapper.FicScriptMapper;
import com.taichu.infra.persistance.model.FicScript;
import com.taichu.infra.persistance.model.FicScriptExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 剧本片段仓储类
 */
@Repository
public class FicScriptRepository {

    @Autowired
    private FicScriptMapper scriptMapper;

    /**
     * 创建剧本片段
     *
     * @param script 剧本片段对象
     * @return 创建的剧本片段ID
     */
    public long insert(FicScriptBO script) {
        FicScript scriptDO = FicScriptConvertor.INSTANCE.toDataObject(script);
        int res = scriptMapper.insert(scriptDO);
        return (long) res;
    }

    /**
     * 根据ID查询剧本片段
     *
     * @param id 剧本片段ID
     * @return 剧本片段对象
     */
    public FicScriptBO findById(Long id) {
        FicScript script = scriptMapper.selectByPrimaryKey(id);
        return FicScriptConvertor.INSTANCE.toDomain(script);
    }

    /**
     * 根据workflowId查询剧本片段列表
     * @param workflowId 工作流ID
     * @return 剧本片段列表
     */
    public List<FicScriptBO> findByWorkflowId(Long workflowId) {
        FicScriptExample example = new FicScriptExample();
        example.createCriteria()
                .andWorkflowIdEqualTo(workflowId)
                .andStatusEqualTo(CommonStatusEnum.VALID.getValue());
        List<FicScript> scriptDOs = scriptMapper.selectByExample(example);
        return StreamUtil.toStream(scriptDOs)
                .map(FicScriptConvertor.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 更新剧本片段
     *
     * @param script 剧本片段对象
     * @return 是否更新成功
     */
    public boolean update(FicScriptBO script) {
        FicScript scriptDO = FicScriptConvertor.INSTANCE.toDataObject(script);
        int res = scriptMapper.updateByPrimaryKeySelective(scriptDO);
        return res == 1;
    }

    /**
     * 将workflowId下所有状态为VALID的script设置为INVALID
     * @param workflowId 工作流ID
     */
    public void offlineByWorkflowId(Long workflowId) {
        FicScriptExample example = new FicScriptExample();
        example.createCriteria()
                .andWorkflowIdEqualTo(workflowId)
                .andStatusEqualTo(CommonStatusEnum.VALID.getValue());
        List<FicScript> scripts = scriptMapper.selectByExample(example);

        if (scripts.isEmpty()) {
            return;
        }

        for (FicScript script : scripts) {
            script.setStatus(CommonStatusEnum.INVALID.getValue());
            scriptMapper.updateByPrimaryKeySelective(script);
        }
    }
} 