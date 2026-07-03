-- ============================================================
-- T-149: M4 模块演示数据（完整主链路场景）
-- 用户说明（需在 user 表中预置或由 M3 提供）：
--   user_id=10  队长张三（所有项目的 creator/leader）
--   user_id=20  成员李四（项目A active，用于互评演示）
--   user_id=30  成员王五（项目A 主动退出 self_exit）
--   user_id=40  成员赵六（项目A 被投票踢出 exit_vote）
--   user_id=50  用户钱七（项目B 申请中 pending）
--   user_id=60  用户孙八（项目B 受邀待处理 pending）
-- ============================================================

-- ① 清空顺序：先子表再主表
DELETE FROM exit_vote_record WHERE vote_id IN (SELECT id FROM exit_vote WHERE project_id IN (1,2));
DELETE FROM exit_vote         WHERE project_id IN (1,2);
DELETE FROM credit_change     WHERE project_id IN (1,2);
DELETE FROM team_member       WHERE project_id IN (1,2);
DELETE FROM team_request      WHERE project_id IN (1,2);
DELETE FROM project_skill     WHERE project_id IN (1,2);
DELETE FROM project           WHERE id IN (1,2);

-- ============================================================
-- 场景1: 项目A ——已结束，互评窗口内（覆盖退出全流程）
-- ============================================================
INSERT INTO `project` (id, creator_id, board_id, title, description, max_members, status,
                        deadline, eval_deadline, ended_at, created_at, updated_at)
VALUES (1, 10, 1,
        '演示项目A——已结束（互评窗口内）',
        'TeamMatch M4 演示：完整项目生命周期；含成员主动退出与投票踢人样例',
        5,
        'ended',
        '2026-04-01 18:00:00',
        DATE_ADD(NOW(), INTERVAL 5 DAY),
        '2026-04-01 18:00:00',
        '2026-03-01 10:00:00',
        NOW());

INSERT INTO `project_skill` (project_id, skill_tag_id, created_at) VALUES
(1, 101, '2026-03-01 10:00:00'),
(1, 102, '2026-03-01 10:00:00');

-- 项目A 成员：
--  B(10) 队长 active   — 仍在队
--  C(20) 成员 active   — 仍在队，可参与互评
--  D(30) 成员 exited   — 主动退出（self_exit）
--  E(40) 成员 exited   — 被投票踢出（exit_vote pass）
INSERT INTO `team_member`
  (project_id, user_id, role,     status,   exit_mode,    joined_at,              left_at,                 created_at, updated_at)
VALUES
  (1, 10, 'leader', 'active',  NULL,         '2026-03-01 10:00:00', NULL,                    NOW(), NOW()),
  (1, 20, 'member', 'active',  NULL,         '2026-03-02 09:00:00', NULL,                    NOW(), NOW()),
  (1, 30, 'member', 'exited',  'self_exit',  '2026-03-03 11:00:00', '2026-03-10 15:00:00',   NOW(), NOW()),
  (1, 40, 'member', 'exited',  'exit_vote',  '2026-03-04 08:00:00', '2026-03-20 12:00:00',   NOW(), NOW());

-- 已关闭（pass）的退出投票：队长踢出成员 E(40)
INSERT INTO `exit_vote`
  (id, project_id, target_user_id, initiator_id, status,   penalty_level,  result, reason,
   total_voters, agree_count, disagree_count, deadline_at,           closed_at,              created_at,             updated_at)
VALUES
  (1,  1,          40,             10,           'closed', 'negotiated',   'pass', '长期缺席项目会议，无法完成分配任务',
   2, 2, 0, '2026-03-19 10:00:00', '2026-03-20 12:00:00', '2026-03-18 10:00:00', NOW());

-- 投票记录：B(10) agree，C(20) agree
INSERT INTO `exit_vote_record` (vote_id, voter_id, choice, created_at) VALUES
  (1, 10, 'agree', '2026-03-18 11:00:00'),
  (1, 20, 'agree', '2026-03-19 08:30:00');

-- credit_change 记录：
--  王五(30) self_exit -10
--  赵六(40) exit_vote pass -10
INSERT INTO `credit_change`
  (user_id, project_id, change_type,  change_value, effective, source_type,   source_id, description,              created_at, updated_at)
VALUES
  (30, 1, 'self_exit',  -10, 1, 'team_member', (SELECT id FROM team_member WHERE project_id=1 AND user_id=30), '主动退出项目A，扣除信誉分',     '2026-03-10 15:00:00', NOW()),
  (40, 1, 'exit_vote',  -10, 1, 'exit_vote',   1,                                                              '被投票踢出项目A（pass），扣除信誉分', '2026-03-20 12:00:00', NOW());

-- ============================================================
-- 场景2: 项目B ——招募中，组队请求演示
-- ============================================================
INSERT INTO `project` (id, creator_id, board_id, title, description, max_members, status,
                        deadline, eval_deadline, ended_at, created_at, updated_at)
VALUES (2, 10, 1,
        '演示项目B——招募中',
        'TeamMatch M4 演示：招募阶段，展示邀请/申请流程',
        4,
        'recruiting',
        DATE_ADD(NOW(), INTERVAL 30 DAY),
        NULL, NULL,
        NOW(), NOW());

INSERT INTO `project_skill` (project_id, skill_tag_id, created_at) VALUES
(2, 103, NOW());

-- 项目B 成员：队长 B(10)
INSERT INTO `team_member`
  (project_id, user_id, role, status, exit_mode, joined_at, left_at, created_at, updated_at)
VALUES
  (2, 10, 'leader', 'active', NULL, NOW(), NULL, NOW(), NOW());

-- 用户 E(50) 申请加入项目B（pending）
INSERT INTO `team_request`
  (project_id, from_user_id, to_user_id, request_type, status, message, created_at, updated_at)
VALUES
  (2, 50, 10, 'apply', 'pending', '我擅长 Vue，对贵项目感兴趣，希望加入', NOW(), NOW());

-- 队长 B(10) 邀请用户 F(60)（pending）
INSERT INTO `team_request`
  (project_id, from_user_id, to_user_id, request_type, status, message, created_at, updated_at)
VALUES
  (2, 10, 60, 'invite', 'pending', '欢迎加入，你的技能契合项目需求', NOW(), NOW());

-- 已被拒绝的邀请（用于展示请求历史）
INSERT INTO `team_request`
  (project_id, from_user_id, to_user_id, request_type, status, message, handled_at, created_at, updated_at)
VALUES
  (2, 10, 70, 'invite', 'rejected', '邀请你参与后端开发', NOW(), '2026-05-25 09:00:00', NOW());