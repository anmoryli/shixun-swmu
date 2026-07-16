-- 修复药品图片无法显示：历史 drug_img 存的是 http://localhost:8080/image/xxx 绝对地址，
-- https 前端会因混合内容拦截 + localhost 指向用户本机而加载失败。
-- 改为同源相对路径 /image/medicine-*.jpg，由 nginx 本地服务（public/image 内置样例图，
-- 随 web 镜像分发）。样例图为通用展示图，非具体药品实拍，详见
-- medical-managerment-system/src/assets/medical-samples/SOURCES.md。

UPDATE `drug` SET `drug_img` = '/image/medicine-capsule.jpg'   WHERE `drug_id` = 12650466;
UPDATE `drug` SET `drug_img` = '/image/medicine-blister.jpg'  WHERE `drug_id` = 12650467;
UPDATE `drug` SET `drug_img` = '/image/medicine-bottle.jpg'   WHERE `drug_id` = 12650468;
UPDATE `drug` SET `drug_img` = '/image/medicine-assorted.jpg' WHERE `drug_id` = 12650469;
UPDATE `drug` SET `drug_img` = '/image/medicine-colorful.jpg' WHERE `drug_id` = 12650470;
UPDATE `drug` SET `drug_img` = '/image/medicine-hand.jpg'     WHERE `drug_id` = 12650471;
