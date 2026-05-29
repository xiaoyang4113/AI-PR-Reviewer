-- ============================================================
-- AI PR Review 助手 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- 执行方式: mysql -u root -p < schema.sql
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS ai_pr_reviewer
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_pr_reviewer;

-- ============================================================
-- 1. 评审任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS review_tasks (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    repo_url        VARCHAR(500)    NOT NULL                 COMMENT 'GitHub仓库地址',
    pr_number       INT             NOT NULL                 COMMENT 'PR编号',
    pr_title        TEXT            DEFAULT NULL             COMMENT 'PR标题（从GitHub API获取）',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING(待处理) / PROCESSING(分析中) / COMPLETED(已完成) / FAILED(失败)',
    summary         TEXT            DEFAULT NULL             COMMENT 'AI生成的变更总结',
    ai_model        VARCHAR(50)     DEFAULT NULL             COMMENT '使用的AI模型名称',
    diff_size       INT             DEFAULT 0                COMMENT 'Diff原始字符数',
    file_count      INT             DEFAULT 0                COMMENT '变更文件数量',
    error_message   TEXT            DEFAULT NULL             COMMENT '失败时的错误信息',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_status (status),
    UNIQUE INDEX idx_repo_url_pr_number (repo_url(100), pr_number),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评审任务表';

-- ============================================================
-- 2. 评审意见表（与任务表 1:N 关联）
-- ============================================================
CREATE TABLE IF NOT EXISTS review_comments (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    task_id         BIGINT          NOT NULL                 COMMENT '关联的任务ID',
    file_path       VARCHAR(500)    NOT NULL                 COMMENT '变更的文件相对路径',
    line_number     INT             DEFAULT NULL             COMMENT '问题所在行号',
    risk_level      VARCHAR(20)     NOT NULL DEFAULT 'INFO'  COMMENT '风险等级: INFO(建议) / WARNING(警告) / CRITICAL(严重)',
    match_code      TEXT            DEFAULT NULL             COMMENT '触发审查的原始代码片段',
    suggestion      TEXT            NOT NULL                 COMMENT 'AI给出的修改建议',
    optimized_code  TEXT            DEFAULT NULL             COMMENT 'AI提供的优化代码示例',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_task_id (task_id),
    INDEX idx_risk_level (risk_level),
    INDEX idx_task_risk (task_id, risk_level),
    CONSTRAINT fk_comment_task
        FOREIGN KEY (task_id) REFERENCES review_tasks(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评审意见表';
