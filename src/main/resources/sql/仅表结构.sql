/*
 Navicat Premium Data Transfer

 Source Server         : localhostMysql
 Source Server Type    : MySQL
 Source Server Version : 50735
 Source Host           : localhost:3306
 Source Schema         : yituliu

 Target Server Type    : MySQL
 Target Server Version : 50735
 File Encoding         : 65001

 Date: 17/05/2023 23:02:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for recruit_data_1
-- ----------------------------
DROP TABLE IF EXISTS `recruit_data_1`;
CREATE TABLE `recruit_data_1`  (
  `id` bigint(20) NOT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `level` int(2) NULL DEFAULT NULL,
  `server` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `source` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `tag` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `uid` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `version` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for recruit_statistics
-- ----------------------------
DROP TABLE IF EXISTS `recruit_statistics`;
CREATE TABLE `recruit_statistics`  (
  `statistical_item` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `statistical_result` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`statistical_item`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for recruit_statistics_config
-- ----------------------------
DROP TABLE IF EXISTS `recruit_statistics_config`;
CREATE TABLE `recruit_statistics_config`  (
  `config_key` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `config_value` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for schedule
-- ----------------------------
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule`  (
  `uid` bigint(20) NOT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `nick_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `schedule_id` bigint(20) NULL DEFAULT NULL,
  `schedule` varchar(20000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`uid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stage_result
-- ----------------------------
DROP TABLE IF EXISTS `stage_result`;
CREATE TABLE `stage_result`  (
  `id` bigint(20) NOT NULL,
  `stage_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `sample_size` int(11) NULL DEFAULT NULL,
  `item_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `item_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `stage_code` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `knock_rating` double(11, 3) NULL DEFAULT NULL,
  `result` double(11, 3) NULL DEFAULT NULL,
  `ap_expect` double(11, 3) NULL DEFAULT NULL,
  `main` varchar(55) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `item_type` varchar(55) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `secondary` varchar(55) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `ap_cost` double(10, 3) NULL DEFAULT NULL,
  `is_show` int(11) NULL DEFAULT NULL,
  `is_value` int(11) NULL DEFAULT NULL,
  `stage_efficiency` double(11, 3) NULL DEFAULT NULL,
  `item_rarity` int(11) NULL DEFAULT NULL,
  `stage_color` int(11) NULL DEFAULT NULL,
  `sample_confidence` double(11, 3) NULL DEFAULT NULL,
  `spm` double(11, 3) NULL DEFAULT NULL,
  `zone_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `zone_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `secondary_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `efficiency` double(11, 3) NULL DEFAULT NULL,
  `exp_coefficient` double(11, 3) NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `open_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for survey_config
-- ----------------------------
DROP TABLE IF EXISTS `survey_config`;
CREATE TABLE `survey_config`  (
  `config_key` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `config_value` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for survey_character_1
-- ----------------------------
DROP TABLE IF EXISTS `survey_character_1`;
CREATE TABLE `survey_character_1`  (
  `id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `uid` bigint(10) NULL DEFAULT NULL,
  `char_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `phase` int(2) NULL DEFAULT NULL,
  `level` int(3) NULL DEFAULT NULL,
  `potential` int(2) NULL DEFAULT NULL,
  `rarity` int(2) NULL DEFAULT NULL,
  `skill1` int(2) NULL DEFAULT NULL,
  `skill2` int(2) NULL DEFAULT NULL,
  `skill3` int(2) NULL DEFAULT NULL,
  `mod_x` int(2) NULL DEFAULT NULL,
  `mod_y` int(2) NULL DEFAULT NULL,
  `own` tinyint(1) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `uid`(`uid`) USING BTREE COMMENT '用户名'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for survey_statistics_character
-- ----------------------------
DROP TABLE IF EXISTS `survey_statistics_character`;
CREATE TABLE `survey_statistics_character`  (
  `char_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `rarity` int(10) NULL DEFAULT NULL,
  `own` int(10) NULL DEFAULT NULL,
  `phase2` int(10) NULL DEFAULT NULL,
  `skill1` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `skill2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `skill3` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `mod_x` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `mod_y` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `potential` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`char_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for survey_user
-- ----------------------------
DROP TABLE IF EXISTS `survey_user`;
CREATE TABLE `survey_user`  (
  `id` bigint(10) NOT NULL,
  `user_name` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `ip` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `status` int(1) NOT NULL,
  `char_table` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for visits
-- ----------------------------
DROP TABLE IF EXISTS `visits`;
CREATE TABLE `visits`  (
  `date` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `visits` int(10) NULL DEFAULT NULL,
  `visits_bot` int(10) NULL DEFAULT NULL,
  `visits_index` int(10) NULL DEFAULT NULL,
  `visits_schedule` int(10) NULL DEFAULT NULL,
  `visits_gacha` int(10) NULL DEFAULT NULL,
  `visits_pack` int(10) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
