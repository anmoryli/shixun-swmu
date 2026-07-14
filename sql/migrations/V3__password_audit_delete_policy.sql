-- Password reset audit rows belong to the doctor account lifecycle.
-- Deleting a doctor/account removes those rows so the normal CRUD delete flow
-- stays atomic and test environments do not accumulate orphan audit records.
-- 幂等：可重复执行。需用 mysql 命令行客户端执行（DELIMITER 语法）。
SET NAMES utf8mb4;
USE `medicine`;

-- ============ 幂等辅助存储过程（末尾清理）============
DELIMITER $$
DROP PROCEDURE IF EXISTS _v3_drop_constraint $$
CREATE PROCEDURE _v3_drop_constraint(p_tbl VARCHAR(64), p_name VARCHAR(64))
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.table_constraints
    WHERE table_schema=DATABASE() AND table_name=p_tbl AND constraint_name=p_name) THEN
    SET @s=CONCAT('ALTER TABLE `',p_tbl,'` DROP FOREIGN KEY `',p_name,'`');
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END $$

DROP PROCEDURE IF EXISTS _v3_add_constraint $$
CREATE PROCEDURE _v3_add_constraint(p_tbl VARCHAR(64), p_name VARCHAR(64), p_def TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
    WHERE table_schema=DATABASE() AND table_name=p_tbl AND constraint_name=p_name) THEN
    SET @s=CONCAT('ALTER TABLE `',p_tbl,'` ADD ',p_def);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END $$
DELIMITER ;

-- 将 fk_reset_account 从 ON DELETE RESTRICT 改为 ON DELETE CASCADE
CALL _v3_drop_constraint('password_reset_audit','fk_reset_account');
CALL _v3_add_constraint('password_reset_audit','fk_reset_account',"CONSTRAINT `fk_reset_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE");

-- ============ 清理辅助存储过程 ============
DROP PROCEDURE IF EXISTS _v3_drop_constraint;
DROP PROCEDURE IF EXISTS _v3_add_constraint;
