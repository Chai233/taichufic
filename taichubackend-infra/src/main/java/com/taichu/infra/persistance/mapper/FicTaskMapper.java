package com.taichu.infra.persistance.mapper;

import com.taichu.infra.persistance.entity.FicTask;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FicTaskMapper {
    
    @Insert("INSERT INTO fic_task (gmt_create, workflow_id, status, task_type, relevance_id, " +
            "relevance_type, algo_task_id, task_abstract) " +
            "VALUES (#{gmtCreate}, #{workflowId}, #{status}, #{taskType}, #{relevanceId}, " +
            "#{relevanceType}, #{algoTaskId}, #{taskAbstract})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FicTask task);
    
    @Select("SELECT * FROM fic_task WHERE id = #{id}")
    FicTask selectById(Long id);
    
    @Select("SELECT * FROM fic_task WHERE workflow_id = #{workflowId}")
    List<FicTask> selectByWorkflowId(Long workflowId);
    
    @Update("<script>" +
            "UPDATE fic_task" +
            "<set>" +
            "<if test='status != null'>status = #{status},</if>" +
            "<if test='algoTaskId != null'>algo_task_id = #{algoTaskId},</if>" +
            "<if test='taskAbstract != null'>task_abstract = #{taskAbstract},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateById(FicTask task);
    
    @Delete("DELETE FROM fic_task WHERE id = #{id}")
    int deleteById(Long id);
} 