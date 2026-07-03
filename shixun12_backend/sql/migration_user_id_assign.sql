-- ============================================
-- 迁移脚本：user 表自增ID 改为 应用层随机ID
-- 适用场景：开发/测试/线上库从 AUTO_INCREMENT 切换到 ASSIGN_ID
-- ============================================

-- Step 1: 修改表结构，去掉 AUTO_INCREMENT
ALTER TABLE `user` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT 'Primary key（MP Snowflake 生成）';

-- Step 2: 验证现有数据不受影响（已有 ID 保持不变）
-- SELECT id, openid, nickname FROM `user`;

-- Step 3: 自增计数器重置（可选，清理后下次自增从1开始）
-- ALTER TABLE `user` AUTO_INCREMENT = 1;

-- ============================================
-- 注意：此变更只影响 user 表，其他表保持自增
-- 新用户注册时由 MyBatis-Plus Snowflake 生成 19 位唯一 ID
-- 不影响现有用户的关联关系（tech_profile.claimed_by_user_id 等外键）
-- ============================================
