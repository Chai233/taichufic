package com.taichu.infra.repo;

import com.taichu.domain.model.FicTaskBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.convertor.FicTaskConvertor;
import com.taichu.infra.convertor.FicWorkflowTaskConvertor;
import com.taichu.infra.persistance.mapper.FicWorkflowTaskMapper;
import com.taichu.infra.persistance.model.FicTask;
import com.taichu.infra.persistance.model.FicWorkflowTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FicWorkflowTaskRepository {

    @Autowired
    private FicWorkflowTaskMapper ficWorkflowTaskMapper;

    public int createFicWorkflowTask(FicWorkflowTaskBO ficWorkflowTask) {
        FicWorkflowTask taskDO = FicWorkflowTaskConvertor.INSTANCE.toDataObject(ficWorkflowTask);
        int res = ficWorkflowTaskMapper.insert(taskDO);
        return res;
    }
}
