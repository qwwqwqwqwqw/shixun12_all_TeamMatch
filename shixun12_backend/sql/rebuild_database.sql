-- ============================================
-- TeamMatch 数据库重建脚本
-- 用途：删除旧数据库，重建并插入示例数据
-- 管理员账号：
-- 用户名：admin
-- 密码：admin123
-- OpenID：admin_openid_001
-- 普通用户账号：
-- 张三 - zhangsan / 123456 (完整资料，清华计算机)
-- 李四 - lisi / 123456 (完整资料，北大软件工程)
-- 王五 - wangwu (无密码，复旦AI研究生)
-- 赵六 - zhaoliu (未完善资料)
-- 孙七 - sunqi (被封禁状态)
-- Mock 微信登录 Code：
-- mock_wx_code_user_a → 张三
-- mock_wx_code_user_b → 李四
-- mock_wx_code_user_c → 王五
-- mock_wx_code_user_d → 赵六
-- mock_wx_code_user_e → 孙七
-- # 先登录 MySQL
-- mysql -u root -p
# 然后在 MySQL 命令行中执行
-- source d:/shixun12/backend/shixun12_backend/sql/rebuild_database.sql;
-- ============================================

-- 1. 删除并重建数据库
DROP DATABASE IF EXISTS `teammatch`;
CREATE DATABASE `teammatch` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `teammatch`;

SET NAMES utf8mb4;

-- ============================================
-- 2. 执行 schema.sql 创建表结构
-- ============================================

-- user 表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL COMMENT 'Primary key（随机ID，非自增）',
  `openid` VARCHAR(128) NULL COMMENT 'WeChat openid for mini-program users',
  `nickname` VARCHAR(64) NULL COMMENT 'User nickname',
  `avatar_url` VARCHAR(512) NULL COMMENT 'Avatar URL',
  `email` VARCHAR(128) NULL COMMENT 'School email',
  `email_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'School email verified flag: 0/1',
  `school` VARCHAR(128) NULL COMMENT 'School name',
  `major` VARCHAR(128) NULL COMMENT 'Major',
  `grade` VARCHAR(32) NULL COMMENT 'Grade',
  `bio` VARCHAR(500) NULL COMMENT 'Profile bio',
  `github_username` VARCHAR(64) NULL COMMENT 'GitHub username for P0 cold-start claim',
  `github_claimed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'GitHub profile claimed flag: 0/1',
  `gitee_username` VARCHAR(64) NULL COMMENT 'Gitee username for cold-start claim',
  `gitee_claimed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Gitee profile claimed flag: 0/1',
  `formal_profile_completed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Formal profile completed flag: 0/1',
  `credit_score` INT NOT NULL DEFAULT 100 COMMENT 'Cached credit score, derived from effective credit_change rows',
  `role` VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT 'Role: user/admin',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'Status: active/banned',
  `username` VARCHAR(64) NULL COMMENT 'Admin username',
  `password_hash` VARCHAR(255) NULL COMMENT 'Admin password hash',
  `tech_profile_id` BIGINT NULL COMMENT 'Linked tech profile id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_openid` (`openid`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_status` (`status`),
  KEY `idx_user_role` (`role`),
  KEY `idx_user_formal_profile` (`formal_profile_completed`),
  KEY `idx_user_github_username` (`github_username`),
  KEY `idx_user_tech_profile` (`tech_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M3 user authentication and profile base';

-- tech_profile 表
CREATE TABLE IF NOT EXISTS `tech_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `github_username` VARCHAR(64) NOT NULL COMMENT 'GitHub username',
  `claimed_by_user_id` BIGINT NULL COMMENT 'The user who claimed this profile, NULL if unclaimed',
  `total_stars` INT NOT NULL DEFAULT 0 COMMENT 'Total GitHub stars across all repos',
  `total_repos` INT NOT NULL DEFAULT 0 COMMENT 'Total public repositories',
  `total_commits` INT NOT NULL DEFAULT 0 COMMENT 'Total commits (approximate from repo stats)',
  `total_prs` INT NOT NULL DEFAULT 0 COMMENT 'Total pull requests',
  `total_contributions` INT NOT NULL DEFAULT 0 COMMENT 'Total contributions in the last year',
  `top_languages` VARCHAR(500) NULL COMMENT 'Top programming languages, JSON array',
  `tech_score` INT NOT NULL DEFAULT 0 COMMENT 'Computed tech score: stars*10+commits*2+prs*5+repos*3+contributions',
  `bio` VARCHAR(500) NULL COMMENT 'GitHub profile bio',
  `avatar_url` VARCHAR(512) NULL COMMENT 'GitHub avatar URL',
  `source` VARCHAR(32) NOT NULL DEFAULT 'github' COMMENT 'Data source: github/gitee',
  `sync_status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'Sync status: pending/synced/failed',
  `last_synced_at` DATETIME NULL COMMENT 'Last time profile was synced from GitHub',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tech_profile_username_source` (`github_username`, `source`),
  KEY `idx_tech_profile_claimed_user` (`claimed_by_user_id`),
  KEY `idx_tech_profile_tech_score` (`tech_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M3 tech profile from GitHub analysis for cold-start leaderboard';

-- skill_tag 表
CREATE TABLE IF NOT EXISTS `skill_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `name` VARCHAR(64) NOT NULL COMMENT 'Skill tag name',
  `category` VARCHAR(32) NOT NULL COMMENT 'Category: language/framework/tool/soft_skill',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'Status: active/inactive',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_tag_category_name` (`category`, `name`),
  KEY `idx_skill_tag_category_status` (`category`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M3 preset skill tag dictionary';

-- user_skill 表
CREATE TABLE IF NOT EXISTS `user_skill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `skill_tag_id` BIGINT NOT NULL COMMENT 'Skill tag id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_skill` (`user_id`, `skill_tag_id`),
  KEY `idx_user_skill_tag` (`skill_tag_id`),
  KEY `idx_user_skill_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M3 user-skill relation';

-- board 表
CREATE TABLE IF NOT EXISTS `board` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `name` VARCHAR(64) NOT NULL COMMENT 'Board name',
  `description` VARCHAR(255) NULL COMMENT 'Board description',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'Status: active/inactive',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Display order',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_name` (`name`),
  KEY `idx_board_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M6 board/category';

-- project 表
CREATE TABLE IF NOT EXISTS `project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `creator_id` BIGINT NOT NULL COMMENT 'Project creator / leader user id',
  `board_id` BIGINT NOT NULL COMMENT 'Board id',
  `title` VARCHAR(128) NOT NULL COMMENT 'Project title',
  `description` TEXT NOT NULL COMMENT 'Project description',
  `max_members` INT NOT NULL COMMENT 'Maximum member count',
  `status` VARCHAR(32) NOT NULL DEFAULT 'recruiting' COMMENT 'Status: recruiting/in_progress/ended/eval_closed',
  `deadline` DATETIME NULL COMMENT 'Recruiting deadline',
  `eval_deadline` DATETIME NULL COMMENT 'Evaluation deadline, ended_at + 7 days',
  `ended_at` DATETIME NULL COMMENT 'Project ended time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_project_creator` (`creator_id`),
  KEY `idx_project_board_status` (`board_id`, `status`),
  KEY `idx_project_status_deadline` (`status`, `deadline`),
  KEY `idx_project_eval_deadline` (`status`, `eval_deadline`),
  FULLTEXT KEY `ft_project_title_description` (`title`, `description`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M4 project lifecycle';

-- project_skill 表
CREATE TABLE IF NOT EXISTS `project_skill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `project_id` BIGINT NOT NULL COMMENT 'Project id',
  `skill_tag_id` BIGINT NOT NULL COMMENT 'Required skill tag id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_skill` (`project_id`, `skill_tag_id`),
  KEY `idx_project_skill_tag` (`skill_tag_id`),
  KEY `idx_project_skill_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M4 project-skill relation';

-- team_request 表
CREATE TABLE IF NOT EXISTS `team_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `project_id` BIGINT NOT NULL COMMENT 'Project id',
  `from_user_id` BIGINT NOT NULL COMMENT 'Request sender user id',
  `to_user_id` BIGINT NOT NULL COMMENT 'Request receiver user id',
  `request_type` VARCHAR(32) NOT NULL COMMENT 'Type: invite/apply',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'Status: pending/accepted/rejected/cancelled/expired',
  `message` VARCHAR(500) NULL COMMENT 'Request message',
  `handled_at` DATETIME NULL COMMENT 'Handled time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `pending_dedupe_key` VARCHAR(255) GENERATED ALWAYS AS (
    CASE
      WHEN `status` = 'pending' THEN CONCAT(`project_id`, ':', `from_user_id`, ':', `to_user_id`, ':', `request_type`)
      ELSE NULL
    END
  ) STORED COMMENT 'Generated unique key for pending request dedupe',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_request_pending` (`pending_dedupe_key`),
  KEY `idx_team_request_project_status` (`project_id`, `status`),
  KEY `idx_team_request_from_status` (`from_user_id`, `status`),
  KEY `idx_team_request_to_status` (`to_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M4 unified team invite/apply request';

-- team_member 表
CREATE TABLE IF NOT EXISTS `team_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `project_id` BIGINT NOT NULL COMMENT 'Project id',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `role` VARCHAR(32) NOT NULL DEFAULT 'member' COMMENT 'Role: leader/member',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'Status: active/exited',
  `exit_mode` VARCHAR(32) NULL COMMENT 'Exit mode: self_exit/exit_vote; NULL when active',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Joined time',
  `left_at` DATETIME NULL COMMENT 'Left time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_member_project_user` (`project_id`, `user_id`),
  KEY `idx_team_member_project_status` (`project_id`, `status`),
  KEY `idx_team_member_user_status` (`user_id`, `status`),
  KEY `idx_team_member_project_role` (`project_id`, `role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M4 project member relation';

-- exit_vote 表
CREATE TABLE IF NOT EXISTS `exit_vote` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `project_id` BIGINT NOT NULL COMMENT 'Project id',
  `target_user_id` BIGINT NOT NULL COMMENT 'Target member user id',
  `initiator_id` BIGINT NOT NULL COMMENT 'Leader who initiates vote',
  `status` VARCHAR(32) NOT NULL DEFAULT 'voting' COMMENT 'Status: voting/closed',
  `penalty_level` VARCHAR(32) NOT NULL COMMENT 'Penalty level: negotiated/malicious',
  `result` VARCHAR(32) NULL COMMENT 'Result: pass/reject/NULL when voting or target self_exit',
  `reason` TEXT NULL COMMENT 'Exit vote reason',
  `total_voters` INT NOT NULL DEFAULT 0 COMMENT 'Eligible voters count, excluding target',
  `agree_count` INT NOT NULL DEFAULT 0 COMMENT 'Agree vote count',
  `disagree_count` INT NOT NULL DEFAULT 0 COMMENT 'Disagree vote count',
  `deadline_at` DATETIME NOT NULL COMMENT 'Vote deadline, created_at + 24 hours',
  `closed_at` DATETIME NULL COMMENT 'Closed time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `active_vote_key` VARCHAR(128) GENERATED ALWAYS AS (
    CASE
      WHEN `status` = 'voting' THEN CONCAT(`project_id`, ':', `target_user_id`)
      ELSE NULL
    END
  ) STORED COMMENT 'Generated unique key for active vote dedupe',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exit_vote_active_target` (`active_vote_key`),
  KEY `idx_exit_vote_project_status` (`project_id`, `status`),
  KEY `idx_exit_vote_target_status` (`target_user_id`, `status`),
  KEY `idx_exit_vote_deadline` (`status`, `deadline_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M4 exit vote';

-- exit_vote_record 表
CREATE TABLE IF NOT EXISTS `exit_vote_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `vote_id` BIGINT NOT NULL COMMENT 'Exit vote id',
  `voter_id` BIGINT NOT NULL COMMENT 'Voter user id',
  `choice` VARCHAR(32) NOT NULL COMMENT 'Choice: agree/disagree',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exit_vote_record_voter` (`vote_id`, `voter_id`),
  KEY `idx_exit_vote_record_voter` (`voter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M4 exit vote record';

-- evaluation 表
CREATE TABLE IF NOT EXISTS `evaluation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `project_id` BIGINT NOT NULL COMMENT 'Project id',
  `evaluator_id` BIGINT NOT NULL COMMENT 'Evaluator user id',
  `target_id` BIGINT NOT NULL COMMENT 'Target user id',
  `communication_score` TINYINT NOT NULL COMMENT 'Communication score, 1-5',
  `task_score` TINYINT NOT NULL COMMENT 'Task completion score, 1-5',
  `skill_score` TINYINT NOT NULL COMMENT 'Technical skill score, 1-5',
  `responsibility_score` TINYINT NOT NULL COMMENT 'Responsibility score, 1-5',
  `average_score` DECIMAL(3,2) NULL COMMENT 'Cached average of four scores',
  `comment` TEXT NULL COMMENT 'Evaluation comment',
  `status` VARCHAR(32) NOT NULL DEFAULT 'normal' COMMENT 'Status: normal/pending_review/voided/kept_no_credit',
  `reviewer_id` BIGINT NULL COMMENT 'Admin reviewer id for evaluation review',
  `review_note` VARCHAR(500) NULL COMMENT 'Review note',
  `reviewed_at` DATETIME NULL COMMENT 'Reviewed time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_evaluator_target` (`project_id`, `evaluator_id`, `target_id`),
  KEY `idx_evaluation_project_target` (`project_id`, `target_id`),
  KEY `idx_evaluation_project_evaluator` (`project_id`, `evaluator_id`),
  KEY `idx_evaluation_status` (`status`),
  KEY `idx_evaluation_reviewer` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M5 peer evaluation';

-- evaluation_tag 表
CREATE TABLE IF NOT EXISTS `evaluation_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `evaluation_id` BIGINT NOT NULL COMMENT 'Evaluation id',
  `tag_name` VARCHAR(64) NOT NULL COMMENT 'Tag name',
  `tag_type` VARCHAR(32) NOT NULL COMMENT 'Tag type: positive/negative',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_evaluation_tag` (`evaluation_id`, `tag_name`, `tag_type`),
  KEY `idx_evaluation_tag_type` (`tag_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M5 evaluation reason tag';

-- skill_endorse 表
CREATE TABLE IF NOT EXISTS `skill_endorse` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `evaluation_id` BIGINT NOT NULL COMMENT 'Evaluation id',
  `project_id` BIGINT NOT NULL COMMENT 'Project id',
  `endorser_id` BIGINT NOT NULL COMMENT 'Endorser user id',
  `target_id` BIGINT NOT NULL COMMENT 'Target user id',
  `skill_tag_id` BIGINT NOT NULL COMMENT 'Endorsed skill tag id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_endorse_project_user_skill` (`project_id`, `endorser_id`, `target_id`, `skill_tag_id`),
  KEY `idx_skill_endorse_evaluation` (`evaluation_id`),
  KEY `idx_skill_endorse_target_skill` (`target_id`, `skill_tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M5 skill endorsement display detail';

-- credit_change 表
CREATE TABLE IF NOT EXISTS `credit_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User whose credit changes',
  `project_id` BIGINT NULL COMMENT 'Related project id when applicable',
  `change_type` VARCHAR(32) NOT NULL COMMENT 'Type: evaluation/exit_vote/self_exit/penalty/penalty_restore/appeal_restore',
  `change_value` INT NOT NULL COMMENT 'Delta value, positive or negative',
  `effective` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Effective flag: 1 effective, 0 suspended or not counted',
  `source_type` VARCHAR(32) NOT NULL COMMENT 'Source type: evaluation/exit_vote/team_member/penalty/appeal',
  `source_id` BIGINT NOT NULL COMMENT 'Source record id',
  `description` VARCHAR(500) NULL COMMENT 'Human-readable summary',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_credit_change_source_user_type` (`source_type`, `source_id`, `user_id`, `change_type`),
  KEY `idx_credit_change_user_effective` (`user_id`, `effective`, `created_at`),
  KEY `idx_credit_change_user_project` (`user_id`, `project_id`, `change_type`),
  KEY `idx_credit_change_source` (`source_type`, `source_id`),
  KEY `idx_credit_change_type` (`change_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M5 credit change ledger';

-- report 表
CREATE TABLE IF NOT EXISTS `report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `reporter_id` BIGINT NOT NULL COMMENT 'Reporter user id',
  `target_type` VARCHAR(32) NOT NULL COMMENT 'Target type: user/project',
  `target_id` BIGINT NOT NULL COMMENT 'Target id',
  `reason` TEXT NOT NULL COMMENT 'Report reason',
  `evidence_urls` JSON NULL COMMENT 'Evidence image URLs',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'Status: pending/resolved/dismissed',
  `handler_id` BIGINT NULL COMMENT 'Admin handler id',
  `handle_result` VARCHAR(500) NULL COMMENT 'Handle result',
  `handled_at` DATETIME NULL COMMENT 'Handled time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_report_target` (`target_type`, `target_id`),
  KEY `idx_report_reporter` (`reporter_id`),
  KEY `idx_report_status` (`status`, `created_at`),
  KEY `idx_report_handler` (`handler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M6 report record';

-- appeal 表
CREATE TABLE IF NOT EXISTS `appeal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'Appealing user id',
  `target_type` VARCHAR(32) NOT NULL COMMENT 'Target type: evaluation/penalty',
  `target_id` BIGINT NOT NULL COMMENT 'Target id',
  `reason` TEXT NOT NULL COMMENT 'Appeal reason',
  `evidence_urls` JSON NULL COMMENT 'Evidence image URLs',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'Status: pending/approved/rejected',
  `handler_id` BIGINT NULL COMMENT 'Admin handler id',
  `handle_result` VARCHAR(500) NULL COMMENT 'Handle result',
  `handled_at` DATETIME NULL COMMENT 'Handled time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appeal_target_user` (`target_type`, `target_id`, `user_id`),
  KEY `idx_appeal_user_status` (`user_id`, `status`),
  KEY `idx_appeal_target` (`target_type`, `target_id`),
  KEY `idx_appeal_status` (`status`, `created_at`),
  KEY `idx_appeal_handler` (`handler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M6 appeal record';

-- penalty 表
CREATE TABLE IF NOT EXISTS `penalty` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'Penalized user id',
  `type` VARCHAR(32) NOT NULL COMMENT 'Penalty type: credit_deduct/function_limit',
  `credit_deduct_value` INT NULL COMMENT 'Positive deduct value, required for credit_deduct',
  `reason` TEXT NOT NULL COMMENT 'Penalty reason',
  `admin_id` BIGINT NOT NULL COMMENT 'Admin user id who executes penalty',
  `related_report_id` BIGINT NULL COMMENT 'Related report id, nullable',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'Status: active/revoked',
  `revoked_at` DATETIME NULL COMMENT 'Revoked time when appeal approves',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_penalty_user_status` (`user_id`, `status`),
  KEY `idx_penalty_type_status` (`type`, `status`),
  KEY `idx_penalty_admin` (`admin_id`),
  KEY `idx_penalty_report` (`related_report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='M6 penalty record';

-- ============================================
-- 3. 插入示例数据
-- ============================================

-- 3.1 插入管理员用户（密码：admin123，BCrypt 加密）
INSERT INTO `user` (`id`, `openid`, `nickname`, `username`, `password_hash`, `email`, `email_verified`, `school`, `role`, `status`, `credit_score`, `formal_profile_completed`) VALUES
(1, 'admin_openid_001', '系统管理员', 'admin', '$2b$10$rKRygML6RzRxmaLjiRQMOufNGpg9jdMiQHJqJJ0t6gdcf2vPwwdkK', 'admin@teammatch.edu.cn', 1, 'TeamMatch大学', 'admin', 'active', 100, 1);

-- 3.2 插入普通用户
INSERT INTO `user` (`id`, `openid`, `nickname`, `username`, `password_hash`, `email`, `email_verified`, `school`, `major`, `grade`, `bio`, `github_username`, `github_claimed`, `gitee_username`, `gitee_claimed`, `credit_score`, `formal_profile_completed`, `status`) VALUES
(2, 'mock_wx_code_user_a', '张三', 'zhangsan', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'zhangsan@example.edu.cn', 1, '清华大学', '计算机科学与技术', '2023级', '热爱编程，寻找队友一起做项目', 'zhangsan-github', 1, NULL, 0, 100, 1, 'active'),
(3, 'mock_wx_code_user_b', '李四', 'lisi', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'lisi@example.edu.cn', 1, '北京大学', '软件工程', '2022级', '前端开发爱好者', 'lisi-github', 1, NULL, 0, 95, 1, 'active'),
(4, 'mock_wx_code_user_c', '王五', 'wangwu', NULL, 'wangwu@example.edu.cn', 1, '复旦大学', '人工智能', '2023级', 'AI方向研究生', 'wangwu-github', 1, NULL, 0, 100, 1, 'active'),
(5, 'mock_wx_code_user_d', '赵六', 'zhaoliu', NULL, NULL, 0, NULL, 0, NULL, NULL, NULL, 0, NULL, 0, 100, 0, 'active'),
(6, 'mock_wx_code_user_e', '孙七', 'sunqi', NULL, NULL, 0, NULL, 0, NULL, NULL, NULL, 0, NULL, 0, 80, 0, 'banned');

-- 3.2a 插入技术画像数据（供排行榜使用）
INSERT INTO `tech_profile` (`github_username`, `source`, `claimed_by_user_id`, `total_stars`, `total_repos`, `total_commits`, `total_prs`, `total_contributions`, `top_languages`, `tech_score`, `bio`, `avatar_url`, `sync_status`, `last_synced_at`) VALUES
('zhangsan-github', 'github', 2, 245, 12, 1523, 38, 450, '["Java","Python","TypeScript"]', (245*10+1523*2+38*5+12*3+450*1), 'Full-stack developer passionate about open source', 'https://avatars.githubusercontent.com/u/zhangsan', 'synced', NOW()),
('lisi-github', 'github', 3, 780, 28, 3200, 85, 920, '["JavaScript","Vue.js","CSS","TypeScript"]', (780*10+3200*2+85*5+28*3+920*1), 'Frontend enthusiast & UI/UX lover', 'https://avatars.githubusercontent.com/u/lisi', 'synced', NOW()),
('wangwu-github', 'github', 4, 120, 8, 890, 15, 310, '["Python","C++","CUDA"]', (120*10+890*2+15*5+8*3+310*1), 'AI researcher, open-source contributor', 'https://avatars.githubusercontent.com/u/wangwu', 'synced', NOW()),
('torvalds', 'github', NULL, 999999, 6, 50000, 200, 9999, '["C","Assembly","Shell"]', (999999*10+50000*2+200*5+6*3+9999*1), 'Creator of Linux and Git', 'https://avatars.githubusercontent.com/u/1024025', 'synced', NOW()),
('ruanyf', 'github', NULL, 15000, 45, 8000, 120, 3500, '["JavaScript","Markdown","CSS"]', (15000*10+8000*2+120*5+45*3+3500*1), 'Blogger, author, open-source evangelist', 'https://avatars.githubusercontent.com/u/905434', 'synced', NOW());

-- Gitee 技术画像（与 GitHub 使用相同评分规则）
INSERT INTO `tech_profile` (`github_username`, `source`, `claimed_by_user_id`, `total_stars`, `total_repos`, `total_commits`, `total_prs`, `total_contributions`, `top_languages`, `tech_score`, `bio`, `avatar_url`, `sync_status`, `last_synced_at`) VALUES
('gitee-zhangsan', 'gitee', 2, 156, 8, 920, 22, 280, '["Java","Spring Boot","MySQL"]', (156*10+920*2+22*5+8*3+280*1), 'Java后端开发者，热爱开源', 'https://avatars.githubusercontent.com/u/gitee-zhangsan', 'synced', NOW()),
('gitee-opensource', 'gitee', NULL, 560, 15, 2100, 48, 620, '["Python","Go","Rust"]', (560*10+2100*2+48*5+15*3+620*1), 'Gitee 开源贡献者', 'https://avatars.githubusercontent.com/u/gitee-opensource', 'synced', NOW()),
('gitee-fe-dev', 'gitee', NULL, 320, 10, 1450, 30, 480, '["JavaScript","Vue.js","TypeScript"]', (320*10+1450*2+30*5+10*3+480*1), '前端开发工程师', 'https://avatars.githubusercontent.com/u/gitee-fe-dev', 'synced', NOW());

-- 3.2b 更新用户关联技术画像
UPDATE `user` SET `tech_profile_id` = 1 WHERE `id` = 2;
UPDATE `user` SET `tech_profile_id` = 2 WHERE `id` = 3;
UPDATE `user` SET `tech_profile_id` = 3 WHERE `id` = 4;
-- 张三同时绑定 Gitee（tech_profile_id 指向 GitHub，gitee_username 记录 Gitee 账号）
UPDATE `user` SET `gitee_username` = 'gitee-zhangsan', `gitee_claimed` = 1 WHERE `id` = 2;

-- 3.3 插入技能标签
INSERT INTO `skill_tag` (`name`, `category`, `status`) VALUES
('Java', 'language', 'active'),
('Python', 'language', 'active'),
('JavaScript', 'language', 'active'),
('TypeScript', 'language', 'active'),
('C++', 'language', 'active'),
('Spring Boot', 'framework', 'active'),
('Vue.js', 'framework', 'active'),
('React', 'framework', 'active'),
('Django', 'framework', 'active'),
('MySQL', 'tool', 'active'),
('Redis', 'tool', 'active'),
('Docker', 'tool', 'active'),
('Git', 'tool', 'active'),
('Linux', 'tool', 'active'),
('沟通能力', 'soft_skill', 'active'),
('团队协作', 'soft_skill', 'active'),
('问题解决', 'soft_skill', 'active'),
('时间管理', 'soft_skill', 'active');

-- 3.4 插入用户技能关联
INSERT INTO `user_skill` (`user_id`, `skill_tag_id`) VALUES
(2, 1), (2, 6), (2, 10),  -- 张三: Java, Spring Boot, MySQL
(2, 13), (2, 15),          -- 张三: Git, 沟通能力
(3, 3), (3, 7), (3, 11),  -- 李四: JavaScript, Vue.js, Redis
(3, 14), (3, 16),          -- 李四: Linux, 团队协作
(4, 2), (4, 9), (4, 12);  -- 王五: Python, Django, Docker

-- 3.5 插入板块
INSERT INTO `board` (`name`, `description`, `status`, `sort_order`) VALUES
('课程项目', '各类课程相关的团队项目', 'active', 1),
('竞赛组队', '各类编程竞赛、创新创业大赛组队', 'active', 2),
('志愿服务', '公益志愿类项目', 'active', 3),
('科研合作', '学术研究、论文合作项目', 'active', 4),
('其他', '其他类型的项目', 'active', 5);

-- 3.6 插入项目
INSERT INTO `project` (`creator_id`, `board_id`, `title`, `description`, `max_members`, `status`, `deadline`) VALUES
(2, 1, '基于Spring Boot的在线学习平台', '开发一个功能完善的在线学习平台，支持视频播放、作业提交、在线测试等功能。技术栈：Spring Boot + Vue.js + MySQL', 5, 'recruiting', DATE_ADD(NOW(), INTERVAL 7 DAY)),
(3, 2, 'ACM竞赛集训队招募', '准备参加区域赛，需要算法能力强的队友一起训练。要求：熟悉数据结构与算法，有竞赛经验者优先', 4, 'recruiting', DATE_ADD(NOW(), INTERVAL 5 DAY)),
(2, 3, '校园环保志愿者管理系统', '为校园环保社团开发志愿者管理系统，包括活动发布、报名、签到等功能', 3, 'in_progress', NULL);

-- 3.7 插入项目技能需求
INSERT INTO `project_skill` (`project_id`, `skill_tag_id`) VALUES
(1, 1), (1, 6), (1, 3), (1, 7), (1, 10),  -- 项目1需要: Java, Spring Boot, JavaScript, Vue.js, MySQL
(2, 5), (2, 17),                           -- 项目2需要: C++, 问题解决
(3, 3), (3, 7), (3, 10);                   -- 项目3需要: JavaScript, Vue.js, MySQL

-- 3.8 插入团队成员
INSERT INTO `team_member` (`project_id`, `user_id`, `role`, `status`, `joined_at`) VALUES
(1, 2, 'leader', 'active', NOW()),
(1, 3, 'member', 'active', NOW()),
(2, 3, 'leader', 'active', NOW()),
(3, 2, 'leader', 'active', NOW()),
(3, 4, 'member', 'active', NOW());

-- 3.9 插入团队申请（待处理）
INSERT INTO `team_request` (`project_id`, `from_user_id`, `to_user_id`, `request_type`, `status`, `message`, `created_at`) VALUES
(1, 4, 2, 'apply', 'pending', '我对这个项目很感兴趣，希望能加入团队', NOW()),
(2, 2, 3, 'invite', 'pending', '你的算法能力很强，邀请你加入我们的竞赛队伍', NOW());

-- 3.10 插入评价（已结束项目的互评）
INSERT INTO `evaluation` (`project_id`, `evaluator_id`, `target_id`, `communication_score`, `task_score`, `skill_score`, `responsibility_score`, `average_score`, `comment`, `status`, `created_at`) VALUES
(3, 2, 4, 4, 5, 4, 5, 4.50, '工作认真负责，完成质量很高', 'normal', NOW()),
(3, 4, 2, 5, 5, 5, 5, 5.00, '队长领导能力强，项目推进顺利', 'normal', NOW());

-- 3.11 插入评价标签
INSERT INTO `evaluation_tag` (`evaluation_id`, `tag_name`, `tag_type`) VALUES
(1, '责任心强', 'positive'),
(1, '代码质量高', 'positive'),
(2, '领导力强', 'positive'),
(2, '沟通顺畅', 'positive');

-- 3.12 插入信誉分变动记录
INSERT INTO `credit_change` (`user_id`, `project_id`, `change_type`, `change_value`, `effective`, `source_type`, `source_id`, `description`, `created_at`) VALUES
(4, 3, 'evaluation', 5, 1, 'evaluation', 1, '收到正面评价，信誉分+5', NOW()),
(2, 3, 'evaluation', 5, 1, 'evaluation', 2, '收到正面评价，信誉分+5', NOW());

-- 3.13 插入举报记录
INSERT INTO `report` (`reporter_id`, `target_type`, `target_id`, `reason`, `status`, `created_at`) VALUES
(3, 'user', 5, '该用户存在恶意行为，多次违规', 'pending', NOW()),
(2, 'project', 2, '项目描述与实际不符，存在虚假信息', 'pending', NOW());

-- 3.14 插入申诉记录
INSERT INTO `appeal` (`user_id`, `target_type`, `target_id`, `reason`, `status`, `created_at`) VALUES
(5, 'penalty', 1, '处罚过重，请求复核', 'pending', NOW());

-- 3.15 插入处罚记录
INSERT INTO `penalty` (`user_id`, `type`, `credit_deduct_value`, `reason`, `admin_id`, `related_report_id`, `status`, `created_at`) VALUES
(5, 'credit_deduct', 20, '违规行为处罚', 1, NULL, 'active', NOW());

-- ============================================
-- 4. 完成提示
-- ============================================
SELECT '✅ 数据库重建完成！' AS message;
SELECT '📊 数据统计：' AS info;
SELECT COUNT(*) AS user_count FROM `user`;
SELECT COUNT(*) AS skill_tag_count FROM `skill_tag`;
SELECT COUNT(*) AS board_count FROM `board`;
SELECT COUNT(*) AS project_count FROM `project`;
SELECT COUNT(*) AS evaluation_count FROM `evaluation`;
SELECT COUNT(*) AS report_count FROM `report`;
SELECT COUNT(*) AS penalty_count FROM `penalty`;
