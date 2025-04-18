-- 用户表
create table if not exists user
(
    id       bigint auto_increment primary key,
    username varchar(128) not null
);


-- 点赞记录表
create table if not exists thumb
(
    id         bigint auto_increment primary key,
    userId     bigint                             not null,
    blogId     bigint                             not null,
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间'
);
create unique index idx_userId_blogId on thumb (userId, blogId);
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 添加字段
ALTER TABLE user
    ADD COLUMN userAccount VARCHAR(256) NOT NULL COMMENT '账号',
    ADD COLUMN userPassword VARCHAR(512) NOT NULL COMMENT '密码',
    ADD COLUMN userAvatar VARCHAR(1024) NULL COMMENT '用户头像',
    ADD COLUMN userProfile VARCHAR(512) NULL COMMENT '用户简介',
    ADD COLUMN userRole VARCHAR(256) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
    ADD COLUMN editTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    ADD COLUMN createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    ADD COLUMN updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ADD COLUMN isDelete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除';

-- 添加唯一约束
ALTER TABLE user
    ADD UNIQUE KEY uk_userAccount (userAccount);

-- 添加普通索引
ALTER TABLE user
    ADD INDEX idx_userName (userName);

-- 设置表字符集和注释（这部分可能需要通过修改表属性来完成）
ALTER TABLE user
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE user
    COMMENT = '用户';
INSERT INTO blog (userId, title, coverImg, content, thumbCount, createTime, updateTime)
VALUES
    (1, '第一次发博文', 'https://example.com/cover1.jpg', '这是我第一篇博客内容，欢迎阅读！', 10, NOW(), NOW()),
    (1, '我的第二篇博客', 'https://example.com/cover2.jpg', '第二篇博客内容更加精彩，快来看看！', 25, NOW(), NOW());
