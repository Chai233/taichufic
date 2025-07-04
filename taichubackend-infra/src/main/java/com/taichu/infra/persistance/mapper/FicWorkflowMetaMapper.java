package com.taichu.infra.persistance.mapper;

import com.taichu.infra.persistance.model.FicWorkflowMeta;
import com.taichu.infra.persistance.model.FicWorkflowMetaExample;
import java.util.List;

public interface FicWorkflowMetaMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    long countByExample(FicWorkflowMetaExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    int deleteByExample(FicWorkflowMetaExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    int deleteByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    int insert(FicWorkflowMeta record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    int insertSelective(FicWorkflowMeta record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    List<FicWorkflowMeta> selectByExampleWithBLOBs(FicWorkflowMetaExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    List<FicWorkflowMeta> selectByExample(FicWorkflowMetaExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    FicWorkflowMeta selectByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    int updateByPrimaryKeySelective(FicWorkflowMeta record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    int updateByPrimaryKeyWithBLOBs(FicWorkflowMeta record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow_meta
     *
     * @mbg.generated Sat Jun 21 15:05:51 CST 2025
     */
    int updateByPrimaryKey(FicWorkflowMeta record);
}