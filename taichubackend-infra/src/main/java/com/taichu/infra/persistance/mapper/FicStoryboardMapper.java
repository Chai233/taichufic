package com.taichu.infra.persistance.mapper;

import com.taichu.infra.persistance.entity.FicStoryboard;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FicStoryboardMapper {
    
    @Insert("INSERT INTO fic_storyboard (workflow_id, gmt_create, status, script_id, order_index, content, extend_info) " +
            "VALUES (#{workflowId}, #{gmtCreate}, #{status}, #{scriptId}, #{orderIndex}, #{content}, #{extendInfo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FicStoryboard storyboard);
    
    @Select("SELECT * FROM fic_storyboard WHERE id = #{id}")
    FicStoryboard selectById(Long id);
    
    @Select("SELECT * FROM fic_storyboard WHERE workflow_id = #{workflowId} ORDER BY order_index")
    List<FicStoryboard> selectByWorkflowId(Long workflowId);
    
    @Select("SELECT * FROM fic_storyboard WHERE script_id = #{scriptId} ORDER BY order_index")
    List<FicStoryboard> selectByScriptId(Long scriptId);
    
    @Update("<script>" +
            "UPDATE fic_storyboard" +
            "<set>" +
            "<if test='status != null'>status = #{status},</if>" +
            "<if test='orderIndex != null'>order_index = #{orderIndex},</if>" +
            "<if test='content != null'>content = #{content},</if>" +
            "<if test='extendInfo != null'>extend_info = #{extendInfo},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateById(FicStoryboard storyboard);
    
    @Delete("DELETE FROM fic_storyboard WHERE id = #{id}")
    int deleteById(Long id);
} 