-- V6: 统一历史业务表字符集排序规则,并补齐 medical_policy 系统审计时间列。
--
-- 背景:
-- 1. medical_policy 等遗留表 CHARSET=utf8mb4 但未显式指定 COLLATE,与 V5 RBAC 表
--    (utf8mb4_unicode_ci) 不一致,跨表 JOIN 比较时可能触发 Illegal mix of collations。
-- 2. medical_policy.create_time/update_time 为业务展示用的 varchar(历史导入数据),
--    不宜直接改 datetime 破坏后端 dateOrToday 逻辑;此处新增 created_at/updated_at
--    系统审计列(datetime),供后续审计追溯,不影响现有读写。
--
-- 幂等:可重复执行;仅改表默认 COLLATE 元数据与新增列,不动现有数据。
-- 需用 mysql 命令行客户端执行(DELIMITER 语法)。
SET @v6_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = REPLACE(@@SESSION.sql_mode, 'ANSI_QUOTES', '');
SET NAMES utf8mb4;
USE `medicine`;

-- ============ 幂等辅助存储过程(末尾清理)============
DELIMITER $$
DROP PROCEDURE IF EXISTS _v6_add_column $$
CREATE PROCEDURE _v6_add_column(p_tbl VARCHAR(64), p_col VARCHAR(64), p_def TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name=p_tbl AND column_name=p_col
  ) THEN
    SET @s=CONCAT('ALTER TABLE `',p_tbl,'` ADD COLUMN `',p_col,'` ',p_def);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END $$
DELIMITER ;

-- ============ 统一业务表默认 COLLATE ============
ALTER TABLE `medical_policy` COLLATE = utf8mb4_unicode_ci;
ALTER TABLE `company_policy` COLLATE = utf8mb4_unicode_ci;
ALTER TABLE `drug` COLLATE = utf8mb4_unicode_ci;
ALTER TABLE `drugcompany` COLLATE = utf8mb4_unicode_ci;
ALTER TABLE `city` COLLATE = utf8mb4_unicode_ci;
ALTER TABLE `doctor` COLLATE = utf8mb4_unicode_ci;
ALTER TABLE `sale` COLLATE = utf8mb4_unicode_ci;
ALTER TABLE `material` COLLATE = utf8mb4_unicode_ci;

-- ============ medical_policy 系统审计时间列(不影响现有 create_time/update_time varchar)============
CALL _v6_add_column('medical_policy','created_at',
  "datetime(6) NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '系统审计:记录创建时间'");
CALL _v6_add_column('medical_policy','updated_at',
  "datetime(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '系统审计:记录更新时间'");

-- ============ 清理辅助存储过程 ============
DROP PROCEDURE IF EXISTS _v6_add_column;
SET SESSION sql_mode = @v6_sql_mode;
