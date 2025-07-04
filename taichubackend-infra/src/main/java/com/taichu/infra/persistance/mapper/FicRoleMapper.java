package com.taichu.infra.persistance.mapper;

import com.taichu.infra.persistance.model.FicRole;
import com.taichu.infra.persistance.model.FicRoleExample;
import java.util.List;

public interface FicRoleMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    long countByExample(FicRoleExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int deleteByExample(FicRoleExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int deleteByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int insert(FicRole record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int insertSelective(FicRole record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    List<FicRole> selectByExampleWithBLOBs(FicRoleExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    List<FicRole> selectByExample(FicRoleExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    FicRole selectByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int updateByPrimaryKeySelective(FicRole record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int updateByPrimaryKeyWithBLOBs(FicRole record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_role
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int updateByPrimaryKey(FicRole record);
}