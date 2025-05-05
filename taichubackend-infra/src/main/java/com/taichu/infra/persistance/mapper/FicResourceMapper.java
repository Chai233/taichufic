package com.taichu.infra.persistance.mapper;

import com.taichu.infra.persistance.entity.FicResource;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FicResourceMapper {
    
    @Insert("INSERT INTO fic_resource (gmt_create, status, relevance_id, relevance_type, resource_type, " +
            "resource_storage_type, resource_url, extend_info) " +
            "VALUES (#{gmtCreate}, #{status}, #{relevanceId}, #{relevanceType}, #{resourceType}, " +
            "#{resourceStorageType}, #{resourceUrl}, #{extendInfo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FicResource resource);
    
    @Select("SELECT * FROM fic_resource WHERE id = #{id}")
    FicResource selectById(Long id);
    
    @Select("SELECT * FROM fic_resource WHERE relevance_id = #{relevanceId} AND relevance_type = #{relevanceType}")
    List<FicResource> selectByRelevance(@Param("relevanceId") Long relevanceId, @Param("relevanceType") String relevanceType);
    
    @Update("<script>" +
            "UPDATE fic_resource" +
            "<set>" +
            "<if test='status != null'>status = #{status},</if>" +
            "<if test='resourceUrl != null'>resource_url = #{resourceUrl},</if>" +
            "<if test='extendInfo != null'>extend_info = #{extendInfo},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateById(FicResource resource);
    
    @Delete("DELETE FROM fic_resource WHERE id = #{id}")
    int deleteById(Long id);
} 