-- V4: 新增"销售地点地图"独立菜单页 (component=SaleMap)
-- 用户需求:地图作为单独的 tab 页面,而不是 SaleManage 页内的开关
-- pid=1 挂在 Layout 根下,level=1 与其他一级菜单并列,ROLE_1(管理员)可见

INSERT INTO `permission` (`pid`, `name`, `path`, `component`, `level`, `title`)
SELECT 1, 'BaseSaleMap', '/base/salemap', 'SaleMap', 1, '销售地点地图'
WHERE NOT EXISTS (
  SELECT 1 FROM `permission` WHERE `name` = 'BaseSaleMap' AND `path` = '/base/salemap'
);

INSERT INTO `role_permission` (`roleName`, `per_id`)
SELECT 'ROLE_1', `id`
FROM `permission`
WHERE `name` = 'BaseSaleMap' AND `path` = '/base/salemap'
AND NOT EXISTS (
  SELECT 1 FROM `role_permission` rp
  WHERE rp.`roleName` = 'ROLE_1'
  AND rp.`per_id` = (SELECT `id` FROM `permission` WHERE `name` = 'BaseSaleMap' AND `path` = '/base/salemap' LIMIT 1)
);

-- ROLE_2 普通用户也可见地图 tab(只读,新增/修改按钮由前端 hasRole 隐藏)
INSERT INTO `role_permission` (`roleName`, `per_id`)
SELECT 'ROLE_2', `id`
FROM `permission`
WHERE `name` = 'BaseSaleMap' AND `path` = '/base/salemap'
AND NOT EXISTS (
  SELECT 1 FROM `role_permission` rp
  WHERE rp.`roleName` = 'ROLE_2'
  AND rp.`per_id` = (SELECT `id` FROM `permission` WHERE `name` = 'BaseSaleMap' AND `path` = '/base/salemap' LIMIT 1)
);
