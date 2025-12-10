create table course
(
    id            bigint               not null
        primary key,
    course_name   varchar(255)         not null,
    category      varchar(255)         null,
    description   text                 null,
    online        tinyint(1) default 0 null,
    highlight_str text                 null
);

create table highlight
(
    id   bigint       not null
        primary key,
    name varchar(100) not null comment '亮点名称，如：精品课程、小班授课、高质量讲师',
    constraint name
        unique (name)
);

create table course_highlight
(
    course_id    bigint not null,
    highlight_id bigint not null,
    primary key (course_id, highlight_id),
    constraint course_highlight_ibfk_1
        foreign key (course_id) references course (id)
            on delete cascade,
    constraint course_highlight_ibfk_2
        foreign key (highlight_id) references highlight (id)
            on delete cascade
);

create index highlight_id
    on course_highlight (highlight_id);

create table news
(
    id               bigint                               not null
        primary key,
    news_name        varchar(20)                          not null comment '新闻名称（不超过20字）',
    news_content     varchar(200)                         not null comment '新闻内容（不超过200字）',
    news_category    varchar(255)                         not null comment '新闻分类（多选，逗号分隔）',
    news_description text                                 not null comment '新闻描述',
    has_image        tinyint(1) default 0                 not null comment '是否有图片（1=有，0=无）',
    image_url        varchar(500)                         null comment '图片URL',
    news_tags        varchar(255)                         not null comment '新闻标签（多选，逗号分隔）',
    deleted          tinyint(1) default 0                 not null comment '逻辑删除（1=已删除，0=正常）',
    create_time      datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time      datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '新闻信息表';

create index idx_create_time
    on news (create_time);

create index idx_deleted
    on news (deleted);

create index idx_news_name
    on news (news_name);

create table permission
(
    id          bigint                             not null
        primary key,
    name        varchar(100)                       not null,
    code        varchar(100)                       not null,
    type        varchar(20)                        not null,
    path        varchar(255)                       null,
    method      varchar(10)                        null,
    parent_id   bigint                             null,
    order_num   int      default 0                 null,
    create_time datetime default CURRENT_TIMESTAMP null,
    component   varchar(255)                       null,
    icon        varchar(100)                       null comment '菜单图标',
    extra       varchar(255)                       null comment '额外配置，存储 JSON 或其他参数',
    constraint code
        unique (code)
);

create table role
(
    id          bigint                             not null
        primary key,
    name        varchar(50)                        not null,
    code        varchar(50)                        not null,
    description varchar(255)                       null,
    create_time datetime default CURRENT_TIMESTAMP null,
    constraint code
        unique (code)
);

create table role_permission
(
    role_id       bigint not null,
    permission_id bigint not null,
    primary key (role_id, permission_id),
    constraint role_permission_ibfk_1
        foreign key (role_id) references role (id),
    constraint role_permission_ibfk_2
        foreign key (permission_id) references permission (id)
);

create index permission_id
    on role_permission (permission_id);

create table user
(
    id       bigint                        not null
        primary key,
    name     varchar(30)                   null comment '姓名',
    age      int                           null comment '年龄',
    email    varchar(50)                   null comment '邮箱',
    password varchar(255) default '123456' not null,
    role     varchar(50)  default 'admin'  not null,
    constraint ux_user_name
        unique (name)
);

create table user_role
(
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    constraint fk_user_role_user
        foreign key (user_id) references user (id)
            on delete cascade,
    constraint user_role_ibfk_1
        foreign key (user_id) references user (id),
    constraint user_role_ibfk_2
        foreign key (role_id) references role (id)
);

create index role_id
    on user_role (role_id);


