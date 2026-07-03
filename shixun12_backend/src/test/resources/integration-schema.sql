-- TeamMatch database schema v0.1
-- MySQL 8.0+
-- Source of truth:
-- 1. docs/design/system-design-v2.1.md
-- 2. docs/database/database-baseline.md
-- 3. AI_CONTEXT_INDEX.md routing rules
-- 4. TeamMatch_任务分配_V5_发布候选版_CSV/任务总表.csv
--
-- Excluded legacy and P1/P2 fields/tables are intentionally not defined here.

SET NAMES utf8mb4;

-- 1. user: M3 user authentication/profile base, shared by M4/M5/M6.
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
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
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `tech_profile_id` BIGINT NULL COMMENT 'Linked tech profile id',
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

-- 2. tech_profile: M3 tech profile from GitHub analysis for cold-start leaderboard.
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

-- 3. skill_tag: M3 preset skill dictionary (was #2).
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

-- 3. user_skill: M3 user skill relation used by recommendation/search.
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

-- 4. board: M6 board/category management, read by M4 project.
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

-- 5. project: M4 project lifecycle base.
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

-- 6. project_skill: M4 project required skills, used by recommendation/search.
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

-- 7. team_request: M4 unified invite/apply request.
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

-- 8. team_member: M4 project membership and final exit state.
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

-- 9. exit_vote: M4 leader-initiated member exit vote.
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

-- 10. exit_vote_record: M4 per-voter record for exit_vote.
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

-- 11. evaluation: M5 four-dimensional peer evaluation.
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

-- 12. evaluation_tag: M5 positive/negative reason tags attached to evaluation.
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

-- 13. skill_endorse: M5 skill endorsement detail, display only.
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

-- 14. credit_change: M5 credit score change ledger, written by M4/M5/M6 flows.
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

-- 15. report: M6 report records for user/project only.
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

-- 16. appeal: M6 appeal records for evaluation/penalty only.
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

-- 17. penalty: M6 penalty record, with credit_change side effect handled by service.
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
