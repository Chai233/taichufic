-- 创建用户表 (fic_user)
CREATE TABLE fic_user (
                          id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '用户id',
                          gmt_create bigint NOT NULL COMMENT '创建时间（毫秒时间戳）',
                          phone_number varchar(32) COMMENT '电话号码'
);

-- 创建工作流表 (fic_workflow)
CREATE TABLE fic_workflow (
                              id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '工作流id',
                              user_id bigint NOT NULL COMMENT '关联用户id',
                              gmt_create bigint NOT NULL COMMENT '创建时间（毫秒时间戳）',
                              status tinyint NOT NULL COMMENT '状态',
                              extend_info text COMMENT '扩展字段，jsonObject格式'
);

-- 创建剧本表（fic_script）
CREATE TABLE fic_script (
                            id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '资源id',
                            workflow_id bigint NOT NULL COMMENT '工作流id',
                            gmt_create bigint NOT NULL COMMENT '创建时间（毫秒时间戳）',
                            status tinyint NOT NULL COMMENT '1-有效 0-无效',
                            order_index bigint NOT NULL COMMENT '展示顺序，从0开始递增',
                            content text COMMENT '剧本文本',
                            extend_info text COMMENT '扩展字段，jsonObject格式'
);

-- 创建角色表（fic_role）
CREATE TABLE fic_role (
                          id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '资源id',
                          workflow_id bigint NOT NULL COMMENT '工作流id',
                          gmt_create bigint NOT NULL COMMENT '创建时间（毫秒时间戳）',
                          status tinyint NOT NULL COMMENT '1-有效 0-无效',
                          role_name varchar(128) NOT NULL COMMENT '人物名称',
                          description text COMMENT '角色初始形象描述',
                          prompt text COMMENT '角色描述，用于生成角色初始形象图',
                          extend_info text COMMENT '扩展字段，jsonObject格式'
);


-- 创建分镜表（fic_storyboard）
CREATE TABLE fic_storyboard (
                                id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '分镜id',
                                workflow_id bigint NOT NULL COMMENT '工作流id',
                                gmt_create bigint NOT NULL COMMENT '创建时间（毫秒时间戳）',
                                status tinyint NOT NULL COMMENT '1-有效 0-无效',
                                script_id bigint NOT NULL COMMENT '关联的剧本分片id',
                                order_index bigint NOT NULL COMMENT '展示顺序 关联的剧本分片展示顺序 * 10000 + 自身的展示顺序[1] 自身展示顺序从0开始递增',
                                content text COMMENT '分镜描述',
                                extend_info text COMMENT '扩展字段，jsonObject格式'
);

-- 创建资源表（fic_resource）
CREATE TABLE fic_resource (
                              id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '分镜图id',
                              gmt_create bigint NOT NULL COMMENT '创建时间（毫秒时间戳）',
                              workflow_id bigint NOT NULL COMMENT '工作流id',
                              status tinyint NOT NULL COMMENT '1-有效 0-无效',
                              relevance_id bigint NOT NULL COMMENT '关联键',
                              relevance_type varchar(32) NOT NULL COMMENT '关联键类型',
                              resource_type varchar(32) NOT NULL COMMENT '资源类型',
                              resource_storage_type varchar(32) NOT NULL COMMENT '存储方式: FILE_SYS / ALICLOUD_OSS / AMAZON_S3 等等，可扩展',
                              resource_url varchar(256) NOT NULL COMMENT '存储路径',
                              extend_info text COMMENT '扩展字段，jsonObject格式'
);

-- 创建任务表（fic_task）
CREATE TABLE fic_task (
                          id bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'task id',
                          gmt_create bigint NOT NULL COMMENT '创建时间（毫秒时间戳）',
                          workflow_id bigint NOT NULL COMMENT 'workflow_id',
                          status tinyint NOT NULL COMMENT '1-RUNNING 2-SUCCESS 0-FAIL',
                          task_type varchar(64) NOT NULL COMMENT 'SCRIPT_GENERATION, STORYBOARD_GENERATION, STORYBOARD_IMG_GENERATION, STORYBOARD_VIDEO_GENERATION, FULL_VIDEO_GENERATION',
                          relevance_id bigint NOT NULL COMMENT '关联键',
                          relevance_type varchar(32) NOT NULL COMMENT '关联键类型',
                          algo_task_id bigint COMMENT '算法task id',
                          task_abstract text COMMENT '任务参数摘要'
);