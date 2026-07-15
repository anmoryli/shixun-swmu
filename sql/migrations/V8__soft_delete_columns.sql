-- V8: 为核心业务表新增 deleted_at/deleted_by 软删除列,误删可恢复(配合 service 层软删 + 查询过滤)。
-- 涉及表:doctor/drug/medical_policy/company_policy/material/sale。
-- 幂等:仅当列不存在时新增。需用 mysql 命令行客户端执行(DELIMITER 语法)。
SET NAMES utf8mb4;
USE `medicine`;

-- ============ 幂等辅助存储过程(末尾清理)============
DELIMITER $$
DROP PROCEDURE IF EXISTS _v8_add_column $$
CREATE PROCEDURE _v8_add_column(p_tbl VARCHAR(64), p_col VARCHAR(64), p_def TEXT)
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
CALL _v8_add_column('doctor', 'deleted_at', "datetime NULL DEFAULT NULL COMMENT '软删除时间,NULL 表示未删除'");
CALL _v8_add_column('doctor', 'deleted_by', "bigint NULL DEFAULT NULL COMMENT '执行软删除的账号 id'");
-- drug
CALL _v8_add_column('drug', 'deleted_at', "datetime NULL DEFAULT NULL COMMENT '软删除时间,NULL 表示未删除'");
CALL _v8_add_column('drug', 'deleted_by', "bigint NULL DEFAULT NULL COMMENT '执行软删除的账号 id'");
-- medical_policy
CALL _v8_add_column('medical_policy', 'deleted_at', "datetime NULL DEFAULT NULL COMMENT '软删除时间,NULL 表示未删除'");
CALL _v8_add_column('medical_policy', 'deleted_by', "bigint NULL DEFAULT NULL COMMENT '执行软删除的账号 id'");
-- company_policy
CALL _v8_add_column('company_policy', 'deleted_at', "datetime NULL DEFAULT NULL COMMENT '软删除时间,NULL 表示未删除'");
CALL _v8_add_column('company_policy', 'deleted_by', "bigint NULL DEFAULT NULL COMMENT '执行软删除的账号 id'");
-- material
CALL _v8_add_column('material', 'deleted_at', "datetime NULL DEFAULT NULL COMMENT '软删除时间,NULL 表示未删除'");
CALL _v8_add_column('material', 'deleted_by', "bigint NULL DEFAULT NULL COMMENT '执行软删除的账号 id'");
-- sale
CALL _v8_add_column('sale', 'deleted_at', "datetime NULL DEFAULT NULL COMMENT '软删除时间,NULL 表示未删除'");
CALL _v8_add_column('sale', 'deleted_by', "bigint NULL DEFAULT NULL COMMENT '执行软删除的账号 id'");

-- ============ 清理辅助存储过程 ============
DROP PROCEDURE IF EXISTS _v8_add_column;
