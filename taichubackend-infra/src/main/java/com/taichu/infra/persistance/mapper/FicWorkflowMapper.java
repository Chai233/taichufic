package com.taichu.infra.persistance.mapper;

import com.taichu.infra.persistance.entity.FicWorkflow;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FicWorkflowMapper {
    
    @Insert("INSERT INTO fic_workflow (user_id, gmt_create, status, extend_info) " +
            "VALUES (#{userId}, #{gmtCreate}, #{status}, #{extendInfo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FicWorkflow workflow);
    
    @Select("SELECT * FROM fic_workflow WHERE id = #{id}")
    FicWorkflow selectById(Long id);
    
    @Select("SELECT * FROM fic_workflow WHERE user_id = #{userId}")
    List<FicWorkflow> selectByUserId(Long userId);
    
    @Update("<script>" +
            "UPDATE fic_workflow" +
            "<set>" +
            "<if test='status != null'>status = #{status},</if>" +
            "<if test='extendInfo != null'>extend_info = #{extendInfo},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateById(FicWorkflow workflow);
    
    @Delete("DELETE FROM fic_workflow WHERE id = #{id}")
    int deleteById(Long id);
} 