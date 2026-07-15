-- V9: 为核心业务表新增 create_by/update_by 审计列,记录数据创建/最后修改者的账号 id,便于追溯谁改了数据。
-- 涉及表:doctor/drug/medical_policy/company_policy/material/sale。
-- 幂等:仅当列不存在时新增。需用 mysql 命令行客户端执行(DELIMITER 语法)。
SET NAMES utf8mb4;
USE `medicine`;

-- ============ 幂等辅助存储过程(末尾清理)============
DELIMITER $$
DROP PROCEDURE IF EXISTS _v9_add_column $$
CREATE PROCEDURE _v9_add_column(p_tbl VARCHAR(64), p_col VARCHAR(64), p_def TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = p_tbl AND column_name = p_col
  ) THEN
    SET @s = CONCAT('ALTER TABLE `', p_tbl, '` ADD COLUMN `', p_col, '` ', p_def);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END $$
DELIMITER ;

-- doctor
CALL _v9_add_column('doctor', 'create_by', "bigint NULL DEFAULT NULL COMMENT '创建者账号 id'");
CALL _v9_add_column('doctor', 'update_by', "bigint NULL DEFAULT NULL COMMENT '最后修改者账号 id'");
-- drug
CALL _v9_add_column('drug', 'create_by', "bigint NULL DEFAULT NULL COMMENT '创建者账号 id'");
CALL _v9_add_column('drug', 'update_by', "bigint NULL DEFAULT NULL COMMENT '最后修改者账号 id'");
-- medical_policy
CALL _v9_add_column('medical_policy', 'create_by', "bigint NULL DEFAULT NULL COMMENT '创建者账号 id'");
CALL _v9_add_column('medical_policy', 'update_by', "bigint NULL DEFAULT NULL COMMENT '最后修改者账号 id'");
-- company_policy
CALL _v9_add_column('company_policy', 'create_by', "bigint NULL DEFAULT NULL COMMENT '创建者账号 id'");
CALL _v9_add_column('company_policy', 'update_by', "bigint NULL DEFAULT NULL COMMENT '最后修改者账号 id'");
-- material
CALL _v9_add_column('material', 'create_by', "bigint NULL DEFAULT NULL COMMENT '创建者账号 id'");
CALL _v9_add_column('material', 'update_by', "bigint NULL DEFAULT NULL COMMENT '最后修改者账号 id'");
-- sale
CALL _v9_add_column('sale', 'create_by', "bigint NULL DEFAULT NULL COMMENT '创建者账号 id'");
CALL _v9_add_column('sale', 'update_by', "bigint NULL DEFAULT NULL COMMENT '最后修改者账号 id'");

-- ============ 清理辅助存储过程 ============
DROP PROCEDURE IF EXISTS _v9_add_column;
