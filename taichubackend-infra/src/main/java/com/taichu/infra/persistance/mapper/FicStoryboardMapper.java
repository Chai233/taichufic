package com.taichu.infra.persistance.mapper;

import com.taichu.infra.persistance.model.FicStoryboard;
import com.taichu.infra.persistance.model.FicStoryboardExample;
import java.util.List;

public interface FicStoryboardMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    long countByExample(FicStoryboardExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int deleteByExample(FicStoryboardExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int deleteByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int insert(FicStoryboard record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int insertSelective(FicStoryboard record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    List<FicStoryboard> selectByExampleWithBLOBs(FicStoryboardExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    List<FicStoryboard> selectByExample(FicStoryboardExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    FicStoryboard selectByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int updateByPrimaryKeySelective(FicStoryboard record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int updateByPrimaryKeyWithBLOBs(FicStoryboard record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_storyboard
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    int updateByPrimaryKey(FicStoryboard record);
}