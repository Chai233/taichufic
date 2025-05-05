package com.taichu.infra.persistance.mapper;

import com.taichu.infra.persistance.entity.FicRole;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FicRoleMapper {
    
    @Insert("INSERT INTO fic_role (workflow_id, gmt_create, status, role_name, description, prompt, extend_info) " +
            "VALUES (#{workflowId}, #{gmtCreate}, #{status}, #{roleName}, #{description}, #{prompt}, #{extendInfo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FicRole role);
    
    @Select("SELECT * FROM fic_role WHERE id = #{id}")
    FicRole selectById(Long id);
    
    @Select("SELECT * FROM fic_role WHERE workflow_id = #{workflowId}")
    List<FicRole> selectByWorkflowId(Long workflowId);
    
    @Update("<script>" +
            "UPDATE fic_role" +
            "<set>" +
            "<if test='status != null'>status = #{status},</if>" +
            "<if test='roleName != null'>role_name = #{roleName},</if>" +
            "<if test='description != null'>description = #{description},</if>" +
            "<if test='prompt != null'>prompt = #{prompt},</if>" +
            "<if test='extendInfo != null'>extend_info = #{extendInfo},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateById(FicRole role);
    
    @Delete("DELETE FROM fic_role WHERE id = #{id}")
    int deleteById(Long id);
} 