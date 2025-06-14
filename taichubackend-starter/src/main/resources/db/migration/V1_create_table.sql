create table fic_algo_task
(
    id               bigint auto_increment comment 'task id'
        primary key,
    gmt_create       bigint        not null comment '创建时间（毫秒时间戳）',
    workflow_task_id bigint        not null comment 'workflow_task_id',
    status           tinyint       not null comment '1-RUNNING 2-SUCCESS 0-FAIL',
    task_type        varchar(64)   not null comment 'SCRIPT_GENERATION, STORYBOARD_GENERATION, STORYBOARD_IMG_GENERATION, STORYBOARD_VIDEO_GENERATION, FULL_VIDEO_GENERATION',
    algo_task_id     bigint        null comment '算法task id',
    task_abstract    varchar(4096) null comment '任务参数摘要',
    relevant_id_type varchar(64)   null comment 'workflowId / 分镜id',
    relevant_id      bigint        null comment 'workflowId 或者 分镜id'
);

create table fic_resource
(
    id                    bigint auto_increment comment 'id'
        primary key,
    gmt_create            bigint        not null,
    workflow_id           bigint        not null comment 'id',
    status                tinyint       not null comment '1- 0-',
    relevance_id          bigint        not null,
    relevance_type        varchar(32)   not null,
    resource_type         varchar(32)   not null,
    resource_storage_type varchar(32)   not null comment ': FILE_SYS / ALICLOUD_OSS / AMAZON_S3 ',
    resource_url          varchar(256)  not null,
    extend_info           varchar(4096) null comment 'jsonObject'
);

create table fic_role
(
    id          bigint auto_increment comment '资源id'
        primary key,
    workflow_id bigint       not null comment '工作流id',
    gmt_create  bigint       not null comment '创建时间（毫秒时间戳）',
    status      tinyint      not null comment '1-有效 0-无效',
    role_name   varchar(128) not null comment '人物名称',
    description text         null comment '角色初始形象描述',
    prompt      text         null comment '角色描述，用于生成角色初始形象图',
    extend_info text         null comment '扩展字段，jsonObject格式'
);

create table fic_script
(
    id          bigint auto_increment comment '资源id'
        primary key,
    workflow_id bigint  not null comment '工作流id',
    gmt_create  bigint  not null comment '创建时间（毫秒时间戳）',
    status      tinyint not null comment '1-有效 0-无效',
    order_index bigint  not null comment '展示顺序，从0开始递增',
    content     text    null comment '剧本文本',
    extend_info text    null comment '扩展字段，jsonObject格式'
);

create table fic_storyboard
(
    id          bigint auto_increment comment '分镜id'
        primary key,
    workflow_id bigint  not null comment '工作流id',
    gmt_create  bigint  not null comment '创建时间（毫秒时间戳）',
    status      tinyint not null comment '1-有效 0-无效',
    script_id   bigint  not null comment '关联的剧本分片id',
    order_index bigint  not null comment '展示顺序 关联的剧本分片展示顺序 * 10000 + 自身的展示顺序[1] 自身展示顺序从0开始递增',
    content     text    null comment '分镜描述',
    extend_info text    null comment '扩展字段，jsonObject格式'
);

create table fic_user
(
    id           bigint auto_increment comment '用户id'
        primary key,
    gmt_create   bigint      not null comment '创建时间（毫秒时间戳）',
    phone_number varchar(32) null comment '电话号码'
);

create table fic_workflow
(
    id          bigint auto_increment comment '工作流id'
        primary key,
    user_id     bigint  not null comment '关联用户id',
    gmt_create  bigint  not null comment '创建时间（毫秒时间戳）',
    status      tinyint not null comment '状态',
    extend_info text    null comment '扩展字段，jsonObject格式'
);

create table fic_workflow_meta
(
    id          bigint auto_increment comment 'id'
        primary key,
    workflow_id bigint        not null comment 'workflow_id',
    style_type  varchar(256)  null comment '基础风格 (外星文明/赛博朋克/...)',
    story_name  varchar(256)  null comment '小说名称',
    story_info  varchar(1024) null comment '小说信息（json格式）: 章节数、字数...'
);

create table fic_workflow_task
(
    id          bigint auto_increment comment 'workflow task id'
        primary key,
    gmt_create  bigint        not null comment '创建时间（毫秒时间戳）',
    workflow_id bigint        not null comment 'workflow_id',
    status      tinyint       not null comment '1-RUNNING 2-SUCCESS 0-FAIL',
    task_type   varchar(64)   not null comment 'SCRIPT_GENERATION, STORYBOARD_IMG_GENERATION, STORYBOARD_VIDEO_GENERATION,FULL_VIDEO_GENERATION, USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION, USER_RETRY_SINGLE_STORYBOARD_VIDEO_GENERATION, USER_RETRY_FULL_VIDEO_GENERATION',
    params      varchar(4096) null comment '重要任务参数（json格式，everything is string）'
);

