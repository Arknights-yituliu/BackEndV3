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

 Date: 25/03/2023 11:58:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for item
-- ----------------------------
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item`  (
  `id` bigint(20) NOT NULL,
  `item_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `rarity` int(11) NULL DEFAULT NULL,
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `item_value_commendation_cert` double(11, 6) NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `card_num` int(255) NULL DEFAULT NULL,
  `exp_coefficient` double(11, 3) NULL DEFAULT NULL,
  `weight` double(11, 3) NULL DEFAULT NULL,
  `item_value_ap` double(11, 6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of item
-- ----------------------------
INSERT INTO `item` VALUES (0, '30135', 5, 'D32钢', 333.862022, 'orange', 1, 0.625, 0.000, 267.089618);
INSERT INTO `item` VALUES (1, '30125', 5, '双极纳米片', 301.499097, 'orange', 1, 0.625, 0.000, 241.199278);
INSERT INTO `item` VALUES (2, '30115', 5, '聚合剂', 319.662283, 'orange', 1, 0.625, 0.000, 255.729826);
INSERT INTO `item` VALUES (3, '30145', 5, '晶体电子单元', 412.876852, 'orange', 1, 0.625, 0.000, 330.301482);
INSERT INTO `item` VALUES (4, '30155', 5, '烧结核凝晶', 404.913810, 'orange', 1, 0.625, 0.000, 323.931048);
INSERT INTO `item` VALUES (5, '30064', 4, '改量装置', 119.690576, 'purple', 2, 0.625, 4.500, 95.752460);
INSERT INTO `item` VALUES (6, '30044', 4, '异铁块', 137.304176, 'purple', 2, 0.625, 6.000, 109.843341);
INSERT INTO `item` VALUES (7, '30084', 4, '三水锰矿', 121.728302, 'purple', 2, 0.625, 6.700, 97.382641);
INSERT INTO `item` VALUES (8, '31014', 4, '聚合凝胶', 100.876783, 'purple', 2, 0.625, 7.700, 80.701427);
INSERT INTO `item` VALUES (9, '30074', 4, '白马醇', 99.829155, 'purple', 2, 0.625, 7.700, 79.863324);
INSERT INTO `item` VALUES (10, '30054', 4, '酮阵列', 125.508076, 'purple', 2, 0.625, 6.000, 100.406461);
INSERT INTO `item` VALUES (11, '30104', 4, 'RMA70-24', 115.638683, 'purple', 2, 0.625, 6.000, 92.510947);
INSERT INTO `item` VALUES (12, '31024', 4, '炽合金块', 112.347923, 'purple', 2, 0.625, 6.700, 89.878338);
INSERT INTO `item` VALUES (13, '30094', 4, '五水研磨石', 114.344825, 'purple', 2, 0.625, 6.700, 91.475860);
INSERT INTO `item` VALUES (14, '30024', 4, '糖聚块', 119.791887, 'purple', 2, 0.625, 6.700, 95.833510);
INSERT INTO `item` VALUES (15, '30034', 4, '聚酸酯块', 114.167559, 'purple', 2, 0.625, 6.700, 91.334047);
INSERT INTO `item` VALUES (16, '31034', 4, '晶体电路', 116.625152, 'purple', 2, 0.625, 6.000, 93.300121);
INSERT INTO `item` VALUES (17, '30014', 4, '提纯源岩', 74.699820, 'purple', 2, 0.625, 9.000, 59.759856);
INSERT INTO `item` VALUES (18, '31044', 4, '精炼溶剂', 108.220969, 'purple', 2, 0.625, 6.700, 86.576775);
INSERT INTO `item` VALUES (19, '31054', 4, '切削原液', 104.672844, 'purple', 2, 0.625, 6.700, 83.738275);
INSERT INTO `item` VALUES (20, '31064', 4, '转质盐聚块', 101.648818, 'purple', 2, 0.625, 0.000, 81.319054);
INSERT INTO `item` VALUES (21, '30063', 3, '全新装置', 45.842442, 'blue', 3, 0.625, 4.900, 36.673954);
INSERT INTO `item` VALUES (22, '30043', 3, '异铁组', 34.068638, 'blue', 3, 0.625, 6.600, 27.254911);
INSERT INTO `item` VALUES (23, '30083', 3, '轻锰矿', 34.386499, 'blue', 3, 0.625, 6.600, 27.509199);
INSERT INTO `item` VALUES (24, '31013', 3, '凝胶', 38.865366, 'blue', 3, 0.625, 5.900, 31.092293);
INSERT INTO `item` VALUES (25, '30073', 3, '扭转醇', 29.630847, 'blue', 3, 0.625, 7.400, 23.704678);
INSERT INTO `item` VALUES (26, '30053', 3, '酮凝集组', 33.758840, 'blue', 3, 0.625, 6.600, 27.007072);
INSERT INTO `item` VALUES (27, '30103', 3, 'RMA70-12', 46.594412, 'blue', 3, 0.625, 4.900, 37.275529);
INSERT INTO `item` VALUES (28, '31023', 3, '炽合金', 32.071736, 'blue', 3, 0.625, 6.600, 25.657389);
INSERT INTO `item` VALUES (29, '30093', 3, '研磨石', 38.562702, 'blue', 3, 0.625, 5.900, 30.850162);
INSERT INTO `item` VALUES (30, '30023', 3, '糖组', 27.732854, 'blue', 3, 0.625, 8.200, 22.186283);
INSERT INTO `item` VALUES (31, '30033', 3, '聚酸酯组', 27.453414, 'blue', 3, 0.625, 8.200, 21.962731);
INSERT INTO `item` VALUES (32, '31033', 3, '晶体元件', 24.908503, 'blue', 3, 0.625, 6.600, 19.926803);
INSERT INTO `item` VALUES (33, '30013', 3, '固源岩组', 19.707194, 'blue', 3, 0.625, 9.900, 15.765755);
INSERT INTO `item` VALUES (34, '31043', 3, '半自然溶剂', 36.185673, 'blue', 3, 0.625, 5.900, 28.948539);
INSERT INTO `item` VALUES (35, '31053', 3, '化合切削液', 37.298886, 'blue', 3, 0.625, 5.900, 29.839109);
INSERT INTO `item` VALUES (36, '31063', 3, '转质盐组', 41.859248, 'blue', 3, 0.625, 0.000, 33.487398);
INSERT INTO `item` VALUES (37, '30012', 2, '固源岩', 4.402986, 'green', 4, 0.625, 26.300, 3.522389);
INSERT INTO `item` VALUES (38, '30022', 2, '糖', 7.394761, 'green', 4, 0.625, 17.500, 5.915809);
INSERT INTO `item` VALUES (39, '30032', 2, '聚酸酯', 7.324901, 'green', 4, 0.625, 17.500, 5.859921);
INSERT INTO `item` VALUES (40, '30042', 2, '异铁', 8.978707, 'green', 4, 0.625, 14.000, 7.182965);
INSERT INTO `item` VALUES (41, '30052', 2, '酮凝集', 8.901257, 'green', 4, 0.625, 14.000, 7.121006);
INSERT INTO `item` VALUES (42, '30062', 2, '装置', 11.922158, 'green', 4, 0.625, 10.500, 9.537726);
INSERT INTO `item` VALUES (43, '30011', 1, '源岩', 1.472362, 'grey', 5, 0.625, 26.300, 1.177889);
INSERT INTO `item` VALUES (44, '30021', 1, '代糖', 2.469620, 'grey', 5, 0.625, 17.500, 1.975696);
INSERT INTO `item` VALUES (45, '30031', 1, '酯原料', 2.446333, 'grey', 5, 0.625, 17.500, 1.957066);
INSERT INTO `item` VALUES (46, '30041', 1, '异铁碎片', 2.997602, 'grey', 5, 0.625, 14.000, 2.398081);
INSERT INTO `item` VALUES (47, '30051', 1, '双酮', 2.971785, 'grey', 5, 0.625, 14.000, 2.377428);
INSERT INTO `item` VALUES (48, '30061', 1, '破损装置', 3.978752, 'grey', 5, 0.625, 10.500, 3.183002);
INSERT INTO `item` VALUES (100, '3301', 2, '技巧概要·卷1', 2.111000, 'green', 6, 0.625, 0.000, 1.688800);
INSERT INTO `item` VALUES (101, '3302', 3, '技巧概要·卷2', 5.278000, 'blue', 6, 0.625, 0.000, 4.222400);
INSERT INTO `item` VALUES (102, '3303', 4, '技巧概要·卷3', 13.196000, 'purple', 6, 0.625, 0.000, 10.556800);
INSERT INTO `item` VALUES (103, '2004', 5, '高级作战记录', 5.625000, 'orange', 6, 0.625, 0.000, 4.500000);
INSERT INTO `item` VALUES (104, '2003', 4, '中级作战记录', 2.812000, 'purple', 6, 0.625, 0.000, 2.249600);
INSERT INTO `item` VALUES (105, '2002', 3, '初级作战记录', 1.125000, 'blue', 6, 0.625, 0.000, 0.900000);
INSERT INTO `item` VALUES (106, '2001', 2, '基础作战记录', 0.562000, 'green', 6, 0.625, 0.000, 0.449600);
INSERT INTO `item` VALUES (107, '4001', 4, '龙门币', 0.004500, 'purple', 6, 0.625, 0.000, 0.003600);
INSERT INTO `item` VALUES (108, '3261', 3, '医疗芯片', 17.843000, 'blue', 7, 0.625, 0.000, 14.274400);
INSERT INTO `item` VALUES (109, '3271', 3, '辅助芯片', 21.420000, 'blue', 7, 0.625, 0.000, 17.136000);
INSERT INTO `item` VALUES (110, '3211', 3, '先锋芯片', 21.420000, 'blue', 7, 0.625, 0.000, 17.136000);
INSERT INTO `item` VALUES (111, '3281', 3, '特种芯片', 17.843000, 'blue', 7, 0.625, 0.000, 14.274400);
INSERT INTO `item` VALUES (112, '3221', 3, '近卫芯片', 24.996000, 'blue', 7, 0.625, 0.000, 19.996800);
INSERT INTO `item` VALUES (113, '3231', 3, '重装芯片', 24.996000, 'blue', 7, 0.625, 0.000, 19.996800);
INSERT INTO `item` VALUES (114, '3241', 3, '狙击芯片', 21.420000, 'blue', 7, 0.625, 0.000, 17.136000);
INSERT INTO `item` VALUES (115, '3251', 3, '术师芯片', 21.420000, 'blue', 7, 0.625, 0.000, 17.136000);
INSERT INTO `item` VALUES (116, '3262', 3, '医疗芯片组', 35.685000, 'purple', 7, 0.625, 0.000, 28.548000);
INSERT INTO `item` VALUES (117, '3272', 3, '辅助芯片组', 42.840000, 'purple', 7, 0.625, 0.000, 34.272000);
INSERT INTO `item` VALUES (118, '3212', 3, '先锋芯片组', 42.840000, 'purple', 7, 0.625, 0.000, 34.272000);
INSERT INTO `item` VALUES (119, '3282', 3, '特种芯片组', 35.685000, 'purple', 7, 0.625, 0.000, 28.548000);
INSERT INTO `item` VALUES (120, '3222', 3, '近卫芯片组', 49.992000, 'purple', 7, 0.625, 0.000, 39.993600);
INSERT INTO `item` VALUES (121, '3232', 3, '重装芯片组', 49.992000, 'purple', 7, 0.625, 0.000, 39.993600);
INSERT INTO `item` VALUES (122, '3242', 3, '狙击芯片组', 42.840000, 'purple', 7, 0.625, 0.000, 34.272000);
INSERT INTO `item` VALUES (123, '3252', 3, '术师芯片组', 42.840000, 'purple', 7, 0.625, 0.000, 34.272000);
INSERT INTO `item` VALUES (124, '4003', 5, '合成玉', 0.938000, 'orange', 8, 0.625, 0.000, 0.750400);
INSERT INTO `item` VALUES (125, '7001', 4, '招聘许可', 30.085000, 'purple', 8, 0.625, 0.000, 24.068000);
INSERT INTO `item` VALUES (126, '4006', 3, '采购凭证', 1.700000, 'blue', 8, 0.625, 0.000, 1.360000);
INSERT INTO `item` VALUES (127, '7003', 5, '寻访凭证', 562.500000, 'orange', 8, 0.625, 0.000, 450.000000);
INSERT INTO `item` VALUES (128, '32001', 4, '芯片助剂', 152.990000, 'purple', 8, 0.625, 0.000, 122.392000);
INSERT INTO `item` VALUES (129, '3003', 4, '赤金', 1.250000, 'purple', 8, 0.625, 0.000, 1.000000);
INSERT INTO `item` VALUES (130, 'ap_supply_lt_010', 4, '应急理智小样', 0.000000, 'purple', 16, 0.625, 0.000, 0.000000);
INSERT INTO `item` VALUES (131, 'randomMaterial_7', 2, '罗德岛物资补给Ⅳ', 0.000000, 'green', 16, 0.625, 0.000, 0.000000);
INSERT INTO `item` VALUES (132, 'mod_unlock_token', 5, '模组数据块', 204.000000, 'orange', 8, 0.625, 0.000, 163.200000);
INSERT INTO `item` VALUES (133, 'STORY_REVIEW_COIN', 5, '事相碎片', 34.000000, 'orange', 8, 0.625, 0.000, 27.200000);
INSERT INTO `item` VALUES (134, '4002', 4, '至纯源石', 168.750000, 'purple', 8, 0.625, 0.000, 135.000000);
INSERT INTO `item` VALUES (135, 'base_ap', 4, '无人机', 0.047000, 'purple', 8, 0.625, 0.000, 0.037600);
INSERT INTO `item` VALUES (204, 'charm_coin_1', 1, '黄金筹码', 9.000000, 'grey', 16, 0.625, 0.000, 7.200000);
INSERT INTO `item` VALUES (205, 'charm_coin_2', 3, '错版硬币', 12.000000, 'blue', 16, 0.625, 0.000, 9.600000);
INSERT INTO `item` VALUES (206, 'charm_coin_3', 4, '双日城大乐透', 18.000000, 'purple', 16, 0.625, 0.000, 14.400000);
INSERT INTO `item` VALUES (207, 'charm_coin_4', 5, '翡翠庭院至臻', 100.000000, 'orange', 16, 0.625, 0.000, 80.000000);
INSERT INTO `item` VALUES (208, 'charm_r1', 1, '标志物 - 20代金券', 15.000000, 'grey', 16, 0.625, 0.000, 12.000000);
INSERT INTO `item` VALUES (209, 'charm_r2', 3, '标志物 - 40代金券', 30.000000, 'blue', 16, 0.625, 0.000, 24.000000);
INSERT INTO `item` VALUES (210, 'trap_oxygen_3', 5, '沙兹专业镀膜装置', 45.000000, 'orange', 16, 0.625, 0.000, 36.000000);
INSERT INTO `item` VALUES (211, 'act24side_melding_1', 1, '破碎的骨片', 2.000000, 'grey', 16, 0.625, 0.000, 1.600000);
INSERT INTO `item` VALUES (212, 'act24side_melding_2', 2, '源石虫的硬壳', 3.000000, 'green', 16, 0.625, 0.000, 2.400000);
INSERT INTO `item` VALUES (213, 'act24side_melding_3', 3, '鬣犄兽的尖锐齿', 5.000000, 'blue', 16, 0.625, 0.000, 4.000000);
INSERT INTO `item` VALUES (214, 'act24side_melding_4', 4, '凶豕兽的厚实皮', 10.000000, 'purple', 16, 0.625, 0.000, 8.000000);
INSERT INTO `item` VALUES (215, 'act24side_melding_5', 5, '兽之泪', 20.000000, 'orange', 16, 0.625, 0.000, 16.000000);
INSERT INTO `item` VALUES (216, 'act24side_gacha', 5, '炼金池', 0.000000, 'orange', 16, 0.625, 0.000, 0.000000);
INSERT INTO `item` VALUES (217, '0', 1, '0', 0.000000, 'grey', 16, 0.625, 0.000, 0.000000);

SET FOREIGN_KEY_CHECKS = 1;
