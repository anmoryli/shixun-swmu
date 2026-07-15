-- V7: 将历史区域表 china/sysregion 从 utf8mb3 转为 utf8mb4,与 V6 统一的业务表排序规则对齐,
-- 消除跨表 JOIN 比较时的 Illegal mix of collations 风险。
--
-- 背景:V6 已统一 drug/doctor/city 等 8 张业务表为 utf8mb4_unicode_ci,但 china/sysregion
-- 两张历史行政区划表仍为 utf8mb3_general_ci,与业务表 JOIN 比较字符串列时可能报错。
--
-- 幂等:仅在表当前字符集为 utf8mb3 时执行 CONVERT TO,否则跳过。需用 mysql 命令行客户端执行。
SET NAMES utf8mb4;
USE `medicine`;

-- ============ 幂等辅助存储过程(末尾清理)============
DELIMITER $$
DROP PROCEDURE IF EXISTS _v7_convert_if_utf8mb3 $$
CREATE PROCEDURE _v7_convert_if_utf8mb3(p_tbl VARCHAR(64))
BEGIN
  DECLARE v_charset VARCHAR(32) DEFAULT '';
  SELECT c.character_set_name INTO v_charset
  FROM information_schema.tables t
  JOIN information_schema.collations c ON c.collation_name = t.table_collation
  WHERE t.table_schema = DATABASE() AND t.table_name = p_tbl;
  IF v_charset = 'utf8mb3' THEN
    SET @s = CONCAT('ALTER TABLE `', p_tbl, '` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END $$
DELIMITER ;

CALL _v7_convert_if_utf8mb3('china');
CALL _v7_convert_if_utf8mb3('sysregion');

-- ============ 清理辅助存储过程 ============
DROP PROCEDURE IF EXISTS _v7_convert_if_utf8mb3;
