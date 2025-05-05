package com.taichu.infra.persistance.mapper;

import com.taichu.infra.persistance.entity.FicUser;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FicUserMapper {
    
    @Insert("INSERT INTO fic_user (gmt_create, phone_number) VALUES (#{gmtCreate}, #{phoneNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FicUser user);
    
    @Select("SELECT * FROM fic_user WHERE id = #{id}")
    FicUser selectById(Long id);
    
    @Update("UPDATE fic_user SET phone_number = #{phoneNumber} WHERE id = #{id}")
    int updateById(FicUser user);
    
    @Delete("DELETE FROM fic_user WHERE id = #{id}")
    int deleteById(Long id);
} 