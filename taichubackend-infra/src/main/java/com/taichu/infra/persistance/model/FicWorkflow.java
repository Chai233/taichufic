package com.taichu.infra.persistance.model;

public class FicWorkflow {
    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column fic_workflow.id
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    private Long id;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column fic_workflow.user_id
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    private Long userId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column fic_workflow.gmt_create
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    private Long gmtCreate;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column fic_workflow.status
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    private Byte status;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column fic_workflow.extend_info
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    private String extendInfo;

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public FicWorkflow(Long id, Long userId, Long gmtCreate, Byte status) {
        this.id = id;
        this.userId = userId;
        this.gmtCreate = gmtCreate;
        this.status = status;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public FicWorkflow(Long id, Long userId, Long gmtCreate, Byte status, String extendInfo) {
        this.id = id;
        this.userId = userId;
        this.gmtCreate = gmtCreate;
        this.status = status;
        this.extendInfo = extendInfo;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table fic_workflow
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public FicWorkflow() {
        super();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column fic_workflow.id
     *
     * @return the value of fic_workflow.id
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public Long getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column fic_workflow.id
     *
     * @param id the value for fic_workflow.id
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column fic_workflow.user_id
     *
     * @return the value of fic_workflow.user_id
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column fic_workflow.user_id
     *
     * @param userId the value for fic_workflow.user_id
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column fic_workflow.gmt_create
     *
     * @return the value of fic_workflow.gmt_create
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public Long getGmtCreate() {
        return gmtCreate;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column fic_workflow.gmt_create
     *
     * @param gmtCreate the value for fic_workflow.gmt_create
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public void setGmtCreate(Long gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column fic_workflow.status
     *
     * @return the value of fic_workflow.status
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public Byte getStatus() {
        return status;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column fic_workflow.status
     *
     * @param status the value for fic_workflow.status
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public void setStatus(Byte status) {
        this.status = status;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column fic_workflow.extend_info
     *
     * @return the value of fic_workflow.extend_info
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public String getExtendInfo() {
        return extendInfo;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column fic_workflow.extend_info
     *
     * @param extendInfo the value for fic_workflow.extend_info
     *
     * @mbg.generated Sat Jun 14 19:02:20 CST 2025
     */
    public void setExtendInfo(String extendInfo) {
        this.extendInfo = extendInfo == null ? null : extendInfo.trim();
    }
}