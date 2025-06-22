package com.taichu.infra.repo;

import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.enums.CommonStatusEnum;
import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.infra.convertor.FicStoryboardConvertor;
import com.taichu.infra.persistance.mapper.FicStoryboardMapper;
import com.taichu.infra.persistance.model.FicStoryboard;
import com.taichu.infra.persistance.model.FicStoryboardExample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分镜仓储类
 */
@Repository
public class FicStoryboardRepository {

    @Autowired
    private FicStoryboardMapper storyboardMapper;

    /**
     * 创建分镜
     *
     * @param storyboard 分镜对象
     * @return 创建的分镜ID
     */
    public long insert(FicStoryboardBO storyboard) {
        FicStoryboard storyboardDO = FicStoryboardConvertor.toDataObject(storyboard);
        int res = storyboardMapper.insert(storyboardDO);
        return storyboardDO.getId();
    }

    /**
     * 根据ID查询分镜
     *
     * @param id 分镜ID
     * @return 分镜对象
     */
    public FicStoryboardBO findById(Long id) {
        FicStoryboard storyboard = storyboardMapper.selectByPrimaryKey(id);
        return FicStoryboardConvertor.toDomain(storyboard);
    }

    /**
     * 根据workflowId查询分镜
     * @param workflowId
     * @return
     */
    public List<FicStoryboardBO> findValidByWorkflowId(Long workflowId) {
        FicStoryboardExample example = new FicStoryboardExample();
        example.createCriteria().andWorkflowIdEqualTo(workflowId).andStatusEqualTo(CommonStatusEnum.VALID.getValue());
        List<FicStoryboard> storyboardDOs = storyboardMapper.selectByExampleWithBLOBs(example);
        return StreamUtil.toStream(storyboardDOs).map(FicStoryboardConvertor::toDomain).collect(Collectors.toList());
    }

    /**
     * 根据workflowId查询分镜
     * @param workflowId
     * @return
     */
    public List<FicStoryboardBO> findValidByWorkflowIdAndScripId(Long workflowId, Long scriptId) {
        FicStoryboardExample example = new FicStoryboardExample();
        example.createCriteria().andWorkflowIdEqualTo(workflowId)
                .andScriptIdEqualTo(scriptId)
                .andStatusEqualTo(CommonStatusEnum.VALID.getValue());
        List<FicStoryboard> storyboardDOs = storyboardMapper.selectByExampleWithBLOBs(example);
        return StreamUtil.toStream(storyboardDOs).map(FicStoryboardConvertor::toDomain).collect(Collectors.toList());
    }

    /**
     * TODO
     * @param id
     */
    public void offlineById(Long id) {
        FicStoryboard storyboard = new FicStoryboard();
        storyboard.setId(id);
        storyboard.setStatus(CommonStatusEnum.INVALID.getValue());
        storyboardMapper.updateByPrimaryKeySelective(storyboard);
    }
}
