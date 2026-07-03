-- 为已有数据库增加证据图片字段（OSS 接入）
-- 执行：mysql -u root -p teammatch < sql/migration/V20250610_add_evidence_urls.sql
-- 若列已存在会报错，可忽略

ALTER TABLE `report`
    ADD COLUMN `evidence_urls` JSON NULL COMMENT 'Evidence image URLs' AFTER `reason`;

ALTER TABLE `appeal`
    ADD COLUMN `evidence_urls` JSON NULL COMMENT 'Evidence image URLs' AFTER `reason`;
