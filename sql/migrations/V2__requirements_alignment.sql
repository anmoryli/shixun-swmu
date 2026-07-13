-- Requirements-alignment migration for the imported medicine schema.
-- Apply exactly once after sql/medical.sql.
USE `medicine`;

-- Account state and login audit support.
ALTER TABLE `account`
  ADD COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled' AFTER `utype`,
  ADD COLUMN `last_login_time` datetime(6) NULL AFTER `status`,
  ADD CONSTRAINT `chk_account_status` CHECK (`status` IN (0, 1));

-- Sales-location map extension required by the specification.
ALTER TABLE `sale`
  ADD COLUMN `address` varchar(255) NULL COMMENT 'sales location address' AFTER `sale_phone`,
  ADD COLUMN `longitude` decimal(10,6) NULL COMMENT 'WGS84 longitude' AFTER `address`,
  ADD COLUMN `latitude` decimal(9,6) NULL COMMENT 'WGS84 latitude' AFTER `longitude`,
  ADD CONSTRAINT `chk_sale_longitude` CHECK (`longitude` IS NULL OR (`longitude` BETWEEN -180 AND 180)),
  ADD CONSTRAINT `chk_sale_latitude` CHECK (`latitude` IS NULL OR (`latitude` BETWEEN -90 AND 90));

-- Give representative seed locations map-ready coordinates without changing names/phones.
UPDATE `sale` SET `address`='上海市黄浦区南京东路', `longitude`=121.490317, `latitude`=31.222771
WHERE `sale_id`=12635265;
UPDATE `sale` SET `address`='山东省青岛市市南区香港中路', `longitude`=120.382665, `latitude`=36.066938
WHERE `sale_id`=12635266;
UPDATE `sale` SET `address`='广东省广州市越秀区北京路', `longitude`=113.270793, `latitude`=23.125321
WHERE `sale_id`=12635267;

-- Persist data-quality findings before removing relation rows that cannot be served safely.
CREATE TABLE `data_quality_issue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `issue_type` varchar(64) NOT NULL,
  `source_table` varchar(64) NOT NULL,
  `source_id` bigint NULL,
  `details` json NOT NULL,
  `detected_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `resolved` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_quality_type` (`issue_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `data_quality_issue` (`issue_type`, `source_table`, `source_id`, `details`, `resolved`)
SELECT 'ORPHAN_DRUG_SALE', 'drug_sale', ds.`id`,
       JSON_OBJECT('drugId', ds.`drug_id`, 'saleId', ds.`sale_id`), 1
FROM `drug_sale` ds
LEFT JOIN `drug` d ON d.`drug_id`=ds.`drug_id`
LEFT JOIN `sale` s ON s.`sale_id`=ds.`sale_id`
WHERE d.`drug_id` IS NULL OR s.`sale_id` IS NULL;

DELETE ds FROM `drug_sale` ds
LEFT JOIN `drug` d ON d.`drug_id`=ds.`drug_id`
LEFT JOIN `sale` s ON s.`sale_id`=ds.`sale_id`
WHERE d.`drug_id` IS NULL OR s.`sale_id` IS NULL;

INSERT INTO `data_quality_issue` (`issue_type`, `source_table`, `source_id`, `details`)
SELECT 'INVALID_CITY_REGION', 'city', c.`city_id`, JSON_OBJECT('cityNumber', c.`city_number`)
FROM `city` c LEFT JOIN `china` r ON r.`id`=c.`city_number`
WHERE r.`id` IS NULL;

INSERT INTO `data_quality_issue` (`issue_type`, `source_table`, `source_id`, `details`)
SELECT 'REGION_SELF_LOOP', 'china', c.`id`, JSON_OBJECT('parentId', c.`parent_id`)
FROM `china` c WHERE c.`id`=c.`parent_id` AND c.`id`<>0;

INSERT INTO `data_quality_issue` (`issue_type`, `source_table`, `source_id`, `details`)
SELECT 'INVALID_PHONE', 'sale', s.`sale_id`, JSON_OBJECT('phone', s.`sale_phone`)
FROM `sale` s WHERE s.`sale_phone` IS NOT NULL AND s.`sale_phone` NOT REGEXP '^1[0-9]{10}$';

-- Doctors are read-only users for company, sales location and city per the requirement.
INSERT INTO `role_permission` (`roleName`, `per_id`)
SELECT 'ROLE_2', p.`id` FROM `permission` p
WHERE p.`id` IN (3,4,5)
  AND NOT EXISTS (
    SELECT 1 FROM `role_permission` rp
    WHERE rp.`roleName`='ROLE_2' AND rp.`per_id`=p.`id`
  );

-- The repository has no 404 component; keep the permission record for audit but do not assign it.
DELETE FROM `role_permission` WHERE `per_id`=11;

-- Dashboard/news source required by the extension scope.
CREATE TABLE `news` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `summary` varchar(1000) NOT NULL,
  `published_at` datetime(6) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_news_published` (`status`, `published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `news` (`title`, `summary`, `published_at`) VALUES
('系统完成基础数据初始化', '医生、药品、政策和销售地点等基础数据已完成导入。', NOW(6)),
('药品图片安全上传能力上线', '系统将校验文件类型、大小与图片内容，并使用随机文件名保存。', NOW(6)),
('角色权限矩阵完成校准', '管理员保留维护权限，医生按需求获得基础信息只读权限。', NOW(6));

CREATE TABLE `password_reset_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `operator_account_id` bigint NOT NULL,
  `reset_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_reset_account` (`account_id`, `reset_at`),
  CONSTRAINT `fk_reset_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_reset_operator` FOREIGN KEY (`operator_account_id`) REFERENCES `account` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Search and relationship indexes/constraints after orphan cleanup.
ALTER TABLE `account` ADD UNIQUE KEY `uk_account_phone` (`phonenumber`);
ALTER TABLE `city` ADD UNIQUE KEY `uk_city_number` (`city_number`);
ALTER TABLE `company_policy`
  ADD KEY `idx_company_policy_company` (`company_id`),
  ADD CONSTRAINT `fk_company_policy_company` FOREIGN KEY (`company_id`) REFERENCES `drugcompany` (`company_id`) ON DELETE RESTRICT;
ALTER TABLE `doctor`
  ADD UNIQUE KEY `uk_doctor_account` (`account_id`),
  ADD KEY `idx_doctor_level` (`level_id`),
  ADD KEY `idx_doctor_type` (`type_id`),
  ADD CONSTRAINT `fk_doctor_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_doctor_level` FOREIGN KEY (`level_id`) REFERENCES `doctor_level` (`id`) ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_doctor_type` FOREIGN KEY (`type_id`) REFERENCES `treat_type` (`id`) ON DELETE RESTRICT;
ALTER TABLE `drug_sale`
  ADD UNIQUE KEY `uk_drug_sale` (`drug_id`, `sale_id`),
  ADD KEY `idx_drug_sale_sale` (`sale_id`),
  ADD CONSTRAINT `fk_drug_sale_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug` (`drug_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_drug_sale_sale` FOREIGN KEY (`sale_id`) REFERENCES `sale` (`sale_id`) ON DELETE RESTRICT;
ALTER TABLE `medical_policy`
  ADD KEY `idx_medical_policy_city` (`city_id`),
  ADD CONSTRAINT `fk_medical_policy_city` FOREIGN KEY (`city_id`) REFERENCES `city` (`city_id`) ON DELETE RESTRICT;
ALTER TABLE `permission` ADD KEY `idx_permission_pid` (`pid`);
ALTER TABLE `role_permission`
  ADD UNIQUE KEY `uk_role_permission` (`roleName`, `per_id`),
  ADD KEY `idx_role_permission_permission` (`per_id`),
  ADD CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`per_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE;
ALTER TABLE `sale` ADD KEY `idx_sale_name` (`sale_name`);
ALTER TABLE `drugcompany` ADD KEY `idx_company_name` (`company_name`);
ALTER TABLE `drug` ADD KEY `idx_drug_name` (`drug_name`);

SET FOREIGN_KEY_CHECKS=1;
