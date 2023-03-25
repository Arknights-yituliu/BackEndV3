/*
 Navicat Premium Data Transfer

 Source Server         : yituliuV3
 Source Server Type    : MySQL
 Source Server Version : 50734
 Source Host           : 121.4.17.235:3306
 Source Schema         : yituliu

 Target Server Type    : MySQL
 Target Server Version : 50734
 File Encoding         : 65001

 Date: 25/03/2023 12:00:34
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for store_perm
-- ----------------------------
DROP TABLE IF EXISTS `store_perm`;
CREATE TABLE `store_perm`  (
  `id` int(10) NOT NULL,
  `item_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `store_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `cost` double(11, 3) NULL DEFAULT NULL,
  `quantity` int(10) NULL DEFAULT NULL,
  `rarity` int(10) NULL DEFAULT NULL,
  `cost_per` double(11, 3) NULL DEFAULT NULL,
  `item_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of store_perm
-- ----------------------------
INSERT INTO `store_perm` VALUES (1, 'RMA70-24', 'orange', 80.000, 1, 4, 1.446, '30104');
INSERT INTO `store_perm` VALUES (2, '五水研磨石', 'orange', 75.000, 1, 4, 1.520, '30094');
INSERT INTO `store_perm` VALUES (3, '三水锰矿', 'orange', 80.000, 1, 4, 1.522, '30084');
INSERT INTO `store_perm` VALUES (4, '白马醇', 'orange', 65.000, 1, 4, 1.536, '30074');
INSERT INTO `store_perm` VALUES (5, '改量装置', 'orange', 85.000, 1, 4, 1.405, '30064');
INSERT INTO `store_perm` VALUES (6, '酮阵列', 'orange', 85.000, 1, 4, 1.476, '30054');
INSERT INTO `store_perm` VALUES (7, '聚酸酯块', 'orange', 80.000, 1, 4, 1.428, '30034');
INSERT INTO `store_perm` VALUES (8, '异铁块', 'orange', 90.000, 1, 4, 1.526, '30044');
INSERT INTO `store_perm` VALUES (9, '糖聚块', 'orange', 75.000, 1, 4, 1.596, '30024');
INSERT INTO `store_perm` VALUES (10, '提纯源岩', 'orange', 60.000, 1, 4, 1.246, '30014');
INSERT INTO `store_perm` VALUES (11, '聚合凝胶', 'orange', 65.000, 1, 4, 1.555, '31014');
INSERT INTO `store_perm` VALUES (12, '炽合金块', 'orange', 75.000, 1, 4, 1.497, '31024');
INSERT INTO `store_perm` VALUES (13, '晶体电路', 'orange', 90.000, 1, 4, 1.300, '31034');
INSERT INTO `store_perm` VALUES (14, '精炼溶剂', 'orange', 70.000, 1, 4, 1.515, '31044');
INSERT INTO `store_perm` VALUES (15, '切削原液', 'orange', 70.000, 1, 4, 1.462, '31054');
INSERT INTO `store_perm` VALUES (16, '转质盐聚块', 'orange', 90.000, 1, 4, 1.133, '31064');
INSERT INTO `store_perm` VALUES (17, '轻锰矿', 'orange', 45.000, 2, 3, 1.528, '30083');
INSERT INTO `store_perm` VALUES (18, 'RMA70-12', 'orange', 60.000, 2, 3, 1.554, '30103');
INSERT INTO `store_perm` VALUES (19, '研磨石', 'orange', 50.000, 2, 3, 1.529, '30093');
INSERT INTO `store_perm` VALUES (20, '扭转醇', 'orange', 40.000, 2, 3, 1.483, '30073');
INSERT INTO `store_perm` VALUES (21, '全新装置', 'orange', 60.000, 2, 3, 1.529, '30063');
INSERT INTO `store_perm` VALUES (22, '酮凝集组', 'orange', 45.000, 2, 3, 1.501, '30053');
INSERT INTO `store_perm` VALUES (23, '聚酸酯组', 'orange', 35.000, 2, 3, 1.569, '30033');
INSERT INTO `store_perm` VALUES (24, '异铁组', 'orange', 45.000, 2, 3, 1.514, '30043');
INSERT INTO `store_perm` VALUES (25, '糖组', 'orange', 35.000, 2, 3, 1.581, '30023');
INSERT INTO `store_perm` VALUES (26, '固源岩组', 'orange', 30.000, 2, 3, 1.314, '30013');
INSERT INTO `store_perm` VALUES (27, '凝胶', 'orange', 50.000, 2, 3, 1.552, '31013');
INSERT INTO `store_perm` VALUES (28, '炽合金', 'orange', 40.000, 2, 3, 1.616, '31023');
INSERT INTO `store_perm` VALUES (29, '晶体元件', 'orange', 40.000, 2, 3, 1.251, '31033');
INSERT INTO `store_perm` VALUES (30, '半自然溶剂', 'orange', 50.000, 2, 3, 1.462, '31043');
INSERT INTO `store_perm` VALUES (31, '化合切削液', 'orange', 50.000, 2, 3, 1.393, '31053');
INSERT INTO `store_perm` VALUES (32, '转质盐组', 'orange', 55.000, 2, 3, 1.522, '31063');
INSERT INTO `store_perm` VALUES (33, '装置', 'orange', 40.000, 4, 2, 1.193, '30062');
INSERT INTO `store_perm` VALUES (34, '酮凝集', 'orange', 30.000, 4, 2, 1.187, '30052');
INSERT INTO `store_perm` VALUES (35, '异铁', 'orange', 30.000, 4, 2, 1.197, '30042');
INSERT INTO `store_perm` VALUES (36, '聚酸酯', 'orange', 25.000, 4, 2, 1.172, '30032');
INSERT INTO `store_perm` VALUES (37, '糖', 'orange', 25.000, 4, 2, 1.181, '30022');
INSERT INTO `store_perm` VALUES (38, '固源岩', 'orange', 15.000, 4, 2, 1.174, '30012');
INSERT INTO `store_perm` VALUES (39, '破损装置', 'orange', 40.000, 8, 1, 0.796, '30061');
INSERT INTO `store_perm` VALUES (40, '双酮', 'orange', 30.000, 8, 1, 0.793, '30051');
INSERT INTO `store_perm` VALUES (41, '异铁碎片', 'orange', 30.000, 8, 1, 0.799, '30041');
INSERT INTO `store_perm` VALUES (42, '代糖', 'orange', 25.000, 8, 1, 0.788, '30021');
INSERT INTO `store_perm` VALUES (43, '酯原料', 'orange', 25.000, 8, 1, 0.783, '30031');
INSERT INTO `store_perm` VALUES (44, '源岩', 'orange', 15.000, 8, 1, 0.785, '30011');
INSERT INTO `store_perm` VALUES (100, '轻锰矿', 'green', 35.000, 1, 3, 0.982, '30083');
INSERT INTO `store_perm` VALUES (101, 'RMA70-12', 'green', 45.000, 1, 3, 1.036, '30103');
INSERT INTO `store_perm` VALUES (102, '研磨石', 'green', 40.000, 1, 3, 0.955, '30093');
INSERT INTO `store_perm` VALUES (103, '扭转醇', 'green', 30.000, 1, 3, 0.989, '30073');
INSERT INTO `store_perm` VALUES (104, '全新装置', 'green', 45.000, 1, 3, 1.019, '30063');
INSERT INTO `store_perm` VALUES (105, '酮凝集组', 'green', 35.000, 1, 3, 0.965, '30053');
INSERT INTO `store_perm` VALUES (106, '聚酸酯组', 'green', 30.000, 1, 3, 0.915, '30033');
INSERT INTO `store_perm` VALUES (107, '异铁组', 'green', 35.000, 1, 3, 0.973, '30043');
INSERT INTO `store_perm` VALUES (108, '糖组', 'green', 30.000, 1, 3, 0.922, '30023');
INSERT INTO `store_perm` VALUES (109, '固源岩组', 'green', 25.000, 1, 3, 0.788, '30013');
INSERT INTO `store_perm` VALUES (110, '半自然溶剂', 'green', 40.000, 1, 3, 0.914, '31043');
INSERT INTO `store_perm` VALUES (111, '化合切削液', 'green', 40.000, 1, 3, 0.870, '31053');
INSERT INTO `store_perm` VALUES (112, '凝胶', 'green', 40.000, 1, 3, 0.970, '31013');
INSERT INTO `store_perm` VALUES (113, '炽合金', 'green', 35.000, 1, 3, 0.923, '31023');
INSERT INTO `store_perm` VALUES (114, '晶体元件', 'green', 30.000, 1, 3, 0.834, '31033');
INSERT INTO `store_perm` VALUES (115, '转质盐组', 'green', 45.000, 1, 3, 0.930, '31063');
INSERT INTO `store_perm` VALUES (116, '招聘许可', 'green', 15.000, 1, 4, 2.006, '7001');
INSERT INTO `store_perm` VALUES (117, '寻访凭证', 'green', 450.000, 1, 5, 1.250, '7003');
INSERT INTO `store_perm` VALUES (200, '合成玉', 'purple', 20.000, 100, 5, 4.690, '4003');
INSERT INTO `store_perm` VALUES (201, '改量装置', 'purple', 85.000, 1, 4, 1.405, '30064');
INSERT INTO `store_perm` VALUES (202, '提纯源岩', 'purple', 50.000, 1, 4, 1.495, '30014');
INSERT INTO `store_perm` VALUES (203, '酮阵列', 'purple', 65.000, 1, 4, 1.931, '30054');
INSERT INTO `store_perm` VALUES (204, '糖聚块', 'purple', 60.000, 1, 4, 1.994, '30024');
INSERT INTO `store_perm` VALUES (205, '异铁块', 'purple', 70.000, 1, 4, 1.962, '30044');
INSERT INTO `store_perm` VALUES (206, '聚酸酯块', 'purple', 60.000, 1, 4, 1.904, '30034');
INSERT INTO `store_perm` VALUES (207, '五水研磨石', 'purple', 60.000, 1, 4, 1.900, '30094');
INSERT INTO `store_perm` VALUES (208, '三水锰矿', 'purple', 60.000, 1, 4, 2.030, '30084');
INSERT INTO `store_perm` VALUES (209, '炽合金块', 'purple', 60.000, 1, 4, 1.871, '31024');
INSERT INTO `store_perm` VALUES (210, '聚合凝胶', 'purple', 50.000, 1, 4, 2.022, '31014');
INSERT INTO `store_perm` VALUES (211, '晶体电路', 'purple', 70.000, 1, 4, 1.672, '31034');
INSERT INTO `store_perm` VALUES (212, 'RMA70-24', 'purple', 65.000, 1, 4, 1.780, '30104');
INSERT INTO `store_perm` VALUES (213, '白马醇', 'purple', 50.000, 1, 4, 1.997, '30074');
INSERT INTO `store_perm` VALUES (214, '龙门币', 'purple', 15.000, 2000, 4, 0.600, '4001');
INSERT INTO `store_perm` VALUES (215, '精炼溶剂', 'purple', 55.000, 1, 4, 1.929, '31044');
INSERT INTO `store_perm` VALUES (216, '切削原液', 'purple', 55.000, 1, 4, 1.861, '31054');
INSERT INTO `store_perm` VALUES (217, '高级作战记录', 'purple', 15.000, 1, 5, 0.375, '2004');
INSERT INTO `store_perm` VALUES (218, '技巧概要·卷3', 'purple', 20.000, 1, 4, 0.660, '3303');
INSERT INTO `store_perm` VALUES (300, '改量装置', 'yellow', 20.000, 1, 4, 5.970, '30064');
INSERT INTO `store_perm` VALUES (301, '提纯源岩', 'yellow', 10.000, 1, 4, 7.473, '30014');
INSERT INTO `store_perm` VALUES (302, '酮阵列', 'yellow', 15.000, 1, 4, 8.366, '30054');
INSERT INTO `store_perm` VALUES (303, '糖聚块', 'yellow', 10.000, 1, 4, 11.967, '30024');
INSERT INTO `store_perm` VALUES (304, '异铁块', 'yellow', 15.000, 1, 4, 9.155, '30044');
INSERT INTO `store_perm` VALUES (305, '聚酸酯块', 'yellow', 10.000, 1, 4, 11.426, '30034');
INSERT INTO `store_perm` VALUES (306, '五水研磨石', 'yellow', 10.000, 1, 4, 11.403, '30094');
INSERT INTO `store_perm` VALUES (307, '三水锰矿', 'yellow', 10.000, 1, 4, 12.178, '30084');
INSERT INTO `store_perm` VALUES (308, '炽合金块', 'yellow', 15.000, 1, 4, 7.486, '31024');
INSERT INTO `store_perm` VALUES (309, '聚合凝胶', 'yellow', 15.000, 1, 4, 6.738, '31014');
INSERT INTO `store_perm` VALUES (310, 'RMA70-24', 'yellow', 15.000, 1, 4, 7.714, '30104');
INSERT INTO `store_perm` VALUES (311, '晶体电路', 'yellow', 15.000, 1, 4, 7.803, '31034');
INSERT INTO `store_perm` VALUES (312, '白马醇', 'yellow', 10.000, 1, 4, 9.985, '30074');
INSERT INTO `store_perm` VALUES (313, '模组数据块', 'yellow', 20.000, 1, 5, 10.200, 'mod_unlock_token');
INSERT INTO `store_perm` VALUES (314, '芯片助剂', 'yellow', 15.000, 1, 4, 10.199, '32001');
INSERT INTO `store_perm` VALUES (400, '龙门币', 'grey', 100.000, 1800, 4, 8.100, '4001');
INSERT INTO `store_perm` VALUES (401, '基础作战记录', 'grey', 100.000, 9, 2, 5.058, '2001');
INSERT INTO `store_perm` VALUES (402, '初级作战记录', 'grey', 200.000, 9, 3, 5.062, '2002');
INSERT INTO `store_perm` VALUES (403, '招聘许可', 'grey', 160.000, 1, 4, 18.803, '7001');
INSERT INTO `store_perm` VALUES (404, '赤金', 'grey', 160.000, 6, 4, 4.688, '3003');
INSERT INTO `store_perm` VALUES (405, '技巧概要·卷1', 'grey', 160.000, 5, 2, 6.597, '3301');
INSERT INTO `store_perm` VALUES (406, '技巧概要·卷2', 'grey', 200.000, 3, 3, 7.917, '3302');
INSERT INTO `store_perm` VALUES (407, '源岩', 'grey', 80.000, 2, 1, 3.681, '30011');
INSERT INTO `store_perm` VALUES (408, '代糖', 'grey', 100.000, 2, 1, 4.928, '30021');
INSERT INTO `store_perm` VALUES (409, '酯原料', 'grey', 100.000, 2, 1, 4.894, '30031');
INSERT INTO `store_perm` VALUES (410, '异铁碎片', 'grey', 120.000, 2, 1, 4.994, '30041');
INSERT INTO `store_perm` VALUES (411, '双酮', 'grey', 120.000, 2, 1, 4.955, '30051');
INSERT INTO `store_perm` VALUES (412, '破损装置', 'grey', 80.000, 1, 1, 4.975, '30061');
INSERT INTO `store_perm` VALUES (413, '固源岩', 'grey', 200.000, 3, 2, 6.605, '30012');
INSERT INTO `store_perm` VALUES (414, '糖', 'grey', 200.000, 2, 2, 7.378, '30022');
INSERT INTO `store_perm` VALUES (415, '聚酸酯', 'grey', 200.000, 2, 2, 7.327, '30032');
INSERT INTO `store_perm` VALUES (416, '异铁', 'grey', 240.000, 2, 2, 7.480, '30042');
INSERT INTO `store_perm` VALUES (417, '酮凝集', 'grey', 240.000, 2, 2, 7.421, '30052');
INSERT INTO `store_perm` VALUES (418, '装置', 'grey', 160.000, 1, 2, 7.454, '30062');

SET FOREIGN_KEY_CHECKS = 1;
