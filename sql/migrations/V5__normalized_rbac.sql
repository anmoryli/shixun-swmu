-- V5: 规范化 RBAC（账号 <-> 角色 <-> 权限）并统一菜单/API 权限码。
--
-- 设计目标：
-- 1. account_role 支持一个账号拥有多个角色；
-- 2. rbac_role_permission 以外键关联角色与权限，替代 role_permission 的文本角色名；
-- 3. permission 同时承载 MENU（动态路由）与 ACTION（后端接口）权限；
-- 4. 保留 account.utype 与 role_permission，供历史数据/旧版本回滚读取，不做破坏性删除；
-- 5. 全部 DDL/DML 幂等，可在 V2、V3、V4 之后重复执行。
--
-- 需使用 MySQL CLI 执行（DELIMITER 为客户端指令）。
-- V2/V3 已使用双引号包裹动态 DDL；为兼容 ANSI_QUOTES 会话，暂时将其移除并在末尾恢复。
SET @v5_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = REPLACE(@@SESSION.sql_mode, 'ANSI_QUOTES', '');
SET NAMES utf8mb4;
USE `medicine`;

-- ============ 幂等辅助存储过程（文件末尾清理）============
DELIMITER $$
DROP PROCEDURE IF EXISTS _v5_add_column $$
CREATE PROCEDURE _v5_add_column(p_tbl VARCHAR(64), p_col VARCHAR(64), p_def TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name=p_tbl AND column_name=p_col
  ) THEN
    SET @s=CONCAT('ALTER TABLE `',p_tbl,'` ADD COLUMN `',p_col,'` ',p_def);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END $$

DROP PROCEDURE IF EXISTS _v5_add_index $$
CREATE PROCEDURE _v5_add_index(p_tbl VARCHAR(64), p_name VARCHAR(64), p_def TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name=p_tbl AND index_name=p_name
  ) THEN
    SET @s=CONCAT('ALTER TABLE `',p_tbl,'` ADD ',p_def);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END $$

DROP PROCEDURE IF EXISTS _v5_add_constraint $$
CREATE PROCEDURE _v5_add_constraint(p_tbl VARCHAR(64), p_name VARCHAR(64), p_def TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_schema=DATABASE() AND table_name=p_tbl AND constraint_name=p_name
  ) THEN
    SET @s=CONCAT('ALTER TABLE `',p_tbl,'` ADD CONSTRAINT `',p_name,'` ',p_def);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END $$

DROP PROCEDURE IF EXISTS _v5_assert_account_roles $$
CREATE PROCEDURE _v5_assert_account_roles()
BEGIN
  IF EXISTS (
    SELECT 1 FROM `account`
    WHERE `utype` IS NULL OR `utype` NOT IN ('ROLE_1', 'ROLE_2', 'ROLE_3')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='V5 aborted: account.utype contains an unsupported role';
  END IF;
END $$
DELIMITER ;

-- ============ 角色主数据 ============
CREATE TABLE IF NOT EXISTS `rbac_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL COMMENT '稳定角色编码，不含 Spring ROLE_ 前缀',
  `name` varchar(64) NOT NULL COMMENT '角色显示名称',
  `description` varchar(255) NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_role_code` (`code`),
  CONSTRAINT `chk_rbac_role_enabled` CHECK (`enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `rbac_role` (`code`, `name`, `description`, `enabled`)
SELECT 'ADMIN', '系统管理员', '维护全部业务数据、账号角色和权限配置', 1
WHERE NOT EXISTS (SELECT 1 FROM `rbac_role` WHERE `code`='ADMIN');

INSERT INTO `rbac_role` (`code`, `name`, `description`, `enabled`)
SELECT 'DOCTOR', '医生用户', '只读访问业务资料、政策、销售地点和首页统计', 1
WHERE NOT EXISTS (SELECT 1 FROM `rbac_role` WHERE `code`='DOCTOR');

INSERT INTO `rbac_role` (`code`, `name`, `description`, `enabled`)
SELECT 'PATIENT', '患者用户', '预留角色，当前业务端默认关闭', 0
WHERE NOT EXISTS (SELECT 1 FROM `rbac_role` WHERE `code`='PATIENT');

-- ============ 统一权限目录：既包含菜单，也包含后端动作 ============
CALL _v5_add_column('permission','code',
  "varchar(128) NULL COMMENT '稳定权限码，例如 drug:read' AFTER `id`");
CALL _v5_add_column('permission','permission_type',
  "varchar(16) NOT NULL DEFAULT 'MENU' COMMENT 'MENU 或 ACTION' AFTER `title`");
CALL _v5_add_column('permission','description',
  "varchar(255) NULL AFTER `permission_type`");
CALL _v5_add_column('permission','enabled',
  "tinyint NOT NULL DEFAULT 1 AFTER `description`");
CALL _v5_add_column('permission','sort_order',
  "int NOT NULL DEFAULT 0 AFTER `enabled`");

DELIMITER $$
DROP PROCEDURE IF EXISTS _v5_assert_permission_codes $$
CREATE PROCEDURE _v5_assert_permission_codes()
BEGIN
  IF EXISTS (
    SELECT 1 FROM `permission`
    GROUP BY `code`
    HAVING `code` IS NULL OR `code`='' OR COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='V5 aborted: permission.code is empty or duplicated';
  END IF;
END $$
DELIMITER ;

-- 为既有菜单分配稳定权限码；未知扩展菜单使用可追踪的 legacy 编码。
UPDATE `permission`
SET `code` = CASE `name`
  WHEN 'Layout' THEN 'menu:layout'
  WHEN 'Home' THEN 'menu:home'
  WHEN 'BaseCompany' THEN 'menu:company'
  WHEN 'BaseSale' THEN 'menu:sale'
  WHEN 'BaseCity' THEN 'menu:city'
  WHEN 'ManageDrug' THEN 'menu:drug'
  WHEN 'MedicalPolicy' THEN 'menu:medical-policy'
  WHEN 'CompanyPolicy' THEN 'menu:company-policy'
  WHEN 'DoctorManage' THEN 'menu:doctor'
  WHEN 'MaterialManage' THEN 'menu:material'
  WHEN 'BaseSaleMap' THEN 'menu:sale-map'
  WHEN 'error-404' THEN 'menu:error-404'
  ELSE CONCAT('menu:legacy:', `id`)
END
WHERE `code` IS NULL OR `code`='';

UPDATE `permission`
SET `permission_type`='MENU',
    `enabled`=CASE WHEN `name`='error-404' THEN 0 ELSE 1 END,
    `sort_order`=CASE WHEN `sort_order`=0 THEN `id` ELSE `sort_order` END
WHERE `path` IS NOT NULL OR `component` IS NOT NULL;

-- 后端资源/动作权限。name 仅用于运维识别，授权判断始终使用 code。
INSERT INTO `permission`
  (`code`, `pid`, `name`, `path`, `component`, `level`, `title`, `permission_type`, `description`, `enabled`, `sort_order`)
SELECT seed.code, 0, seed.name, NULL, NULL, 2, seed.title, 'ACTION', seed.description, 1, seed.sort_order
FROM (
  SELECT 'dashboard:read' code, 'DashboardRead' name, '查看首页统计' title, '读取首页统计与资讯' description, 100 sort_order
  UNION ALL SELECT 'company:read', 'CompanyRead', '查询医药公司', '读取医药公司资料', 110
  UNION ALL SELECT 'company:write', 'CompanyWrite', '维护医药公司', '新增、修改或删除医药公司资料', 111
  UNION ALL SELECT 'sale:read', 'SaleRead', '查询销售地点', '读取销售地点与坐标', 120
  UNION ALL SELECT 'sale:write', 'SaleWrite', '维护销售地点', '新增、修改或删除销售地点', 121
  UNION ALL SELECT 'sale-map:read', 'SaleMapRead', '查看销售地图', '读取地图及逆地理编码结果', 122
  UNION ALL SELECT 'city:read', 'CityRead', '查询城市', '读取城市资料', 130
  UNION ALL SELECT 'city:write', 'CityWrite', '维护城市', '新增或删除城市资料', 131
  UNION ALL SELECT 'drug:read', 'DrugRead', '查询药品', '读取药品资料', 140
  UNION ALL SELECT 'drug:write', 'DrugWrite', '维护药品', '新增、修改或删除药品资料', 141
  UNION ALL SELECT 'medical-policy:read', 'MedicalPolicyRead', '查询医保政策', '读取医保政策', 150
  UNION ALL SELECT 'medical-policy:write', 'MedicalPolicyWrite', '维护医保政策', '新增、修改或删除医保政策', 151
  UNION ALL SELECT 'company-policy:read', 'CompanyPolicyRead', '查询公司政策', '读取医药公司政策', 160
  UNION ALL SELECT 'company-policy:write', 'CompanyPolicyWrite', '维护公司政策', '新增、修改或删除医药公司政策', 161
  UNION ALL SELECT 'doctor:read', 'DoctorRead', '查询医生', '读取医生、级别与诊治类型资料', 170
  UNION ALL SELECT 'doctor:write', 'DoctorWrite', '维护医生', '新增、修改或删除医生账号和档案', 171
  UNION ALL SELECT 'doctor:reset-password', 'DoctorResetPassword', '重置医生密码', '重置医生账号密码', 172
  UNION ALL SELECT 'material:read', 'MaterialRead', '查询必备材料', '读取就医必备材料', 180
  UNION ALL SELECT 'material:write', 'MaterialWrite', '维护必备材料', '新增、修改或删除必备材料', 181
  UNION ALL SELECT 'file:upload', 'FileUpload', '上传药品图片', '上传并保存药品图片', 190
) seed
WHERE NOT EXISTS (SELECT 1 FROM `permission` p WHERE p.`code`=seed.code);

-- 所有权限码补齐后再建立唯一索引；兼容迁移前自行扩展过的菜单。
UPDATE `permission`
SET `code`=CONCAT('menu:legacy:', `id`)
WHERE `code` IS NULL OR `code`='';
CALL _v5_assert_permission_codes();
ALTER TABLE `permission`
  MODIFY COLUMN `code` varchar(128) NOT NULL COMMENT '稳定权限码，例如 drug:read' AFTER `id`;
CALL _v5_add_constraint('permission','chk_permission_type',
  "CHECK (`permission_type` IN ('MENU','ACTION'))");
CALL _v5_add_constraint('permission','chk_permission_enabled',
  "CHECK (`enabled` IN (0,1))");
CALL _v5_add_index('permission','uk_permission_code',"UNIQUE KEY `uk_permission_code` (`code`)");
CALL _v5_add_index('permission','idx_permission_type_enabled',
  "KEY `idx_permission_type_enabled` (`permission_type`, `enabled`, `sort_order`)");

-- ============ 账号-角色（多对多）与角色-权限（多对多） ============
CREATE TABLE IF NOT EXISTS `account_role` (
  `account_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `is_primary` tinyint NOT NULL DEFAULT 1 COMMENT '兼容 account.utype 的主角色标记',
  `primary_account_id` bigint GENERATED ALWAYS AS (IF(`is_primary`=1,`account_id`,NULL)) STORED,
  `assigned_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`account_id`, `role_id`),
  UNIQUE KEY `uk_account_role_primary` (`primary_account_id`),
  KEY `idx_account_role_role` (`role_id`, `account_id`),
  CONSTRAINT `chk_account_role_primary` CHECK (`is_primary` IN (0, 1)),
  CONSTRAINT `fk_account_role_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_account_role_role` FOREIGN KEY (`role_id`) REFERENCES `rbac_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rbac_role_permission` (
  `role_id` bigint NOT NULL,
  `permission_id` int NOT NULL,
  `granted_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_rbac_role_permission_permission` (`permission_id`, `role_id`),
  CONSTRAINT `fk_rbac_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `rbac_role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rbac_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 将历史 account.utype 单角色数据迁移为可扩展的多角色关系。
CALL _v5_assert_account_roles();
INSERT INTO `account_role` (`account_id`, `role_id`, `is_primary`)
SELECT DISTINCT a.`id`, r.`id`, 1
FROM `account` a
JOIN `rbac_role` r ON r.`code`=CASE a.`utype`
  WHEN 'ROLE_1' THEN 'ADMIN'
  WHEN 'ROLE_2' THEN 'DOCTOR'
  WHEN 'ROLE_3' THEN 'PATIENT'
  ELSE NULL
END
WHERE NOT EXISTS (
  SELECT 1 FROM `account_role` ar
  WHERE ar.`account_id`=a.`id` AND ar.`role_id`=r.`id`
);

-- 将历史菜单授权复制到规范化关系，保留现有可见菜单。
INSERT INTO `rbac_role_permission` (`role_id`, `permission_id`)
SELECT DISTINCT r.`id`, rp.`per_id`
FROM `role_permission` rp
JOIN `rbac_role` r ON r.`code`=CASE rp.`roleName`
  WHEN 'ROLE_1' THEN 'ADMIN'
  WHEN 'ROLE_2' THEN 'DOCTOR'
  ELSE NULL
END
JOIN `permission` p ON p.`id`=rp.`per_id`
  AND p.`enabled`=1 AND p.`permission_type`='MENU'
WHERE NOT EXISTS (
  SELECT 1 FROM `rbac_role_permission` x
  WHERE x.`role_id`=r.`id` AND x.`permission_id`=rp.`per_id`
);

-- 管理员拥有当前及以后本迁移已知的全部权限。
INSERT INTO `rbac_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `rbac_role` r CROSS JOIN `permission` p
WHERE r.`code`='ADMIN' AND p.`enabled`=1
  AND NOT EXISTS (
    SELECT 1 FROM `rbac_role_permission` x
    WHERE x.`role_id`=r.`id` AND x.`permission_id`=p.`id`
  );

-- 医生保持现有业务读取能力，写操作默认拒绝（最小权限原则）。
INSERT INTO `rbac_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `rbac_role` r
JOIN `permission` p ON p.`code` IN (
  'dashboard:read',
  'company:read',
  'sale:read',
  'sale-map:read',
  'city:read',
  'drug:read',
  'medical-policy:read',
  'company-policy:read',
  'doctor:read',
  'material:read'
)
 WHERE r.`code`='DOCTOR' AND p.`enabled`=1
   AND NOT EXISTS (
     SELECT 1 FROM `rbac_role_permission` x
     WHERE x.`role_id`=r.`id` AND x.`permission_id`=p.`id`
   );

-- ============ 清理辅助存储过程 ============
DROP PROCEDURE IF EXISTS _v5_add_column;
DROP PROCEDURE IF EXISTS _v5_add_index;
DROP PROCEDURE IF EXISTS _v5_add_constraint;
DROP PROCEDURE IF EXISTS _v5_assert_permission_codes;
DROP PROCEDURE IF EXISTS _v5_assert_account_roles;
SET SESSION sql_mode = @v5_sql_mode;
