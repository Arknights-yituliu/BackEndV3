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

 Date: 27/10/2023 20:41:57
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for developer
-- ----------------------------
DROP TABLE IF EXISTS `developer`;
CREATE TABLE `developer`  (
  `developer` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `email` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `level` int(11) NOT NULL,
  PRIMARY KEY (`email`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for honey_cake
-- ----------------------------
DROP TABLE IF EXISTS `honey_cake`;
CREATE TABLE `honey_cake`  (
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `originium` int(11) NULL DEFAULT NULL,
  `orundum` int(11) NULL DEFAULT NULL,
  `permit` int(11) NULL DEFAULT NULL,
  `permit10` int(11) NULL DEFAULT NULL,
  `start` datetime NULL DEFAULT NULL,
  `end` datetime NULL DEFAULT NULL,
  `reward_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `module` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for item
-- ----------------------------
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item`  (
  `id` bigint(20) NOT NULL,
  `item_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `rarity` int(11) NULL DEFAULT NULL,
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `card_num` int(255) NULL DEFAULT NULL,
  `weight` double(11, 6) NULL DEFAULT NULL,
  `item_value_ap` double(11, 6) NULL DEFAULT NULL,
  `version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `item_value` double(11, 6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for item_iteration_value
-- ----------------------------
DROP TABLE IF EXISTS `item_iteration_value`;
CREATE TABLE `item_iteration_value`  (
  `id` bigint(11) NOT NULL,
  `item_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `item_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `iteration_value` double NULL DEFAULT NULL,
  `version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for operator_table
-- ----------------------------
DROP TABLE IF EXISTS `operator_table`;
CREATE TABLE `operator_table`  (
  `char_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `obtain_approach` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `rarity` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`char_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for page_visits
-- ----------------------------
DROP TABLE IF EXISTS `page_visits`;
CREATE TABLE `page_visits`  (
  `redis_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `page_path` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `visits_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `visits_count` int(11) NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`redis_key`) USING BTREE,
  INDEX `page_path`(`page_path`) USING BTREE,
  INDEX `create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quantile_table
-- ----------------------------
DROP TABLE IF EXISTS `quantile_table`;
CREATE TABLE `quantile_table`  (
  `section` double NOT NULL,
  `value` double NULL DEFAULT NULL,
  PRIMARY KEY (`section`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

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
-- Table structure for stage
-- ----------------------------
DROP TABLE IF EXISTS `stage`;
CREATE TABLE `stage`  (
  `stage_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stage_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `zone_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `zone_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `ap_cost` int(11) NULL DEFAULT NULL,
  `stage_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `start_time` datetime NULL DEFAULT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  `spm` double(10, 2) NULL DEFAULT NULL,
  `min_clear_time` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`stage_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stage_result
-- ----------------------------
DROP TABLE IF EXISTS `stage_result`;
CREATE TABLE `stage_result`  (
  `id` bigint(20) NOT NULL,
  `stage_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stage_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `item_series` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `item_series_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `secondary_item_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stage_efficiency` double NULL DEFAULT NULL,
  `version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `le_t5_efficiency` double NULL DEFAULT NULL,
  `le_t4_efficiency` double NULL DEFAULT NULL,
  `le_t3_efficiency` double NULL DEFAULT NULL,
  `spm` double NULL DEFAULT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stage_result_detail
-- ----------------------------
DROP TABLE IF EXISTS `stage_result_detail`;
CREATE TABLE `stage_result_detail`  (
  `id` bigint(20) NOT NULL,
  `stage_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `item_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `knock_rating` double NULL DEFAULT NULL,
  `result` double NULL DEFAULT NULL,
  `ap_expect` double NULL DEFAULT NULL,
  `ratio` double NULL DEFAULT NULL,
  `sample_size` int(11) NULL DEFAULT NULL,
  `ratio_rank` int(11) NULL DEFAULT NULL,
  `sample_confidence` double NULL DEFAULT NULL,
  `version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for store_act
-- ----------------------------
DROP TABLE IF EXISTS `store_act`;
CREATE TABLE `store_act`  (
  `act_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  `result` varchar(16000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`act_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

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
-- Table structure for survey_character_1
-- ----------------------------
DROP TABLE IF EXISTS `survey_character_1`;
CREATE TABLE `survey_character_1`  (
  `id` bigint(20) NOT NULL,
  `uid` bigint(10) NULL DEFAULT NULL,
  `char_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `elite` int(2) NULL DEFAULT NULL,
  `level` int(3) NULL DEFAULT NULL,
  `potential` int(2) NULL DEFAULT NULL,
  `rarity` int(2) NULL DEFAULT NULL,
  `skill1` int(2) NULL DEFAULT NULL,
  `skill2` int(2) NULL DEFAULT NULL,
  `skill3` int(2) NULL DEFAULT NULL,
  `mod_x` int(2) NULL DEFAULT NULL,
  `mod_y` int(2) NULL DEFAULT NULL,
  `own` tinyint(1) NULL DEFAULT NULL,
  `main_skill` int(2) NULL DEFAULT NULL,
  `mod_d` int(2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `uid`(`uid`) USING BTREE COMMENT '用户名'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for survey_operator_upload_log
-- ----------------------------
DROP TABLE IF EXISTS `survey_operator_upload_log`;
CREATE TABLE `survey_operator_upload_log`  (
  `id` bigint(20) NOT NULL,
  `delete_flag` bit(1) NOT NULL,
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_time` bigint(20) NULL DEFAULT NULL,
  `uid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for survey_recruit_5
-- ----------------------------
DROP TABLE IF EXISTS `survey_recruit_5`;
CREATE TABLE `survey_recruit_5`  (
  `id` bigint(20) NOT NULL,
  `create_time` bigint(20) NULL DEFAULT NULL,
  `level` int(2) NULL DEFAULT NULL,
  `server` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `source` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `tag` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `uid` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `version` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for survey_score_1
-- ----------------------------
DROP TABLE IF EXISTS `survey_score_1`;
CREATE TABLE `survey_score_1`  (
  `id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `uid` bigint(20) NULL DEFAULT NULL,
  `char_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `rarity` int(1) NULL DEFAULT NULL,
  `daily` int(2) NULL DEFAULT NULL,
  `rogue` int(2) NULL DEFAULT NULL,
  `security_service` int(2) NULL DEFAULT NULL,
  `hard` int(2) NULL DEFAULT NULL,
  `universal` int(2) NULL DEFAULT NULL,
  `counter` int(2) NULL DEFAULT NULL,
  `building` int(2) NULL DEFAULT NULL,
  `comprehensive` int(2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `uid`(`uid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for survey_statistics_operator
-- ----------------------------
DROP TABLE IF EXISTS `survey_statistics_operator`;
CREATE TABLE `survey_statistics_operator`  (
  `char_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `rarity` int(11) NULL DEFAULT NULL,
  `own` int(11) NULL DEFAULT NULL,
  `elite` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `skill1` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `skill2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `skill3` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `mod_x` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `mod_y` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `potential` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`char_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for survey_statistics_score
-- ----------------------------
DROP TABLE IF EXISTS `survey_statistics_score`;
CREATE TABLE `survey_statistics_score`  (
  `charId` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `rarity` int(1) NULL DEFAULT NULL,
  `daily` int(11) NULL DEFAULT NULL,
  `sample_size_daily` int(11) NULL DEFAULT NULL,
  `rogue` int(11) NULL DEFAULT NULL,
  `sample_size_rogue` int(11) NULL DEFAULT NULL,
  `security_service` int(11) NULL DEFAULT NULL,
  `sample_size_security_service` int(11) NULL DEFAULT NULL,
  `hard` int(11) NULL DEFAULT NULL,
  `sample_size_hard` int(11) NULL DEFAULT NULL,
  `universal` int(11) NULL DEFAULT NULL,
  `sample_size_universal` int(11) NULL DEFAULT NULL,
  `counter` int(11) NULL DEFAULT NULL,
  `sample_size_counter` int(11) NULL DEFAULT NULL,
  `building` int(11) NULL DEFAULT NULL,
  `sample_size_building` int(11) NULL DEFAULT NULL,
  `comprehensive` int(11) NULL DEFAULT NULL,
  `sample_size_comprehensive` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`charId`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for survey_statistics_user
-- ----------------------------
DROP TABLE IF EXISTS `survey_statistics_user`;
CREATE TABLE `survey_statistics_user`  (
  `id` bigint(20) NOT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `operator_count` int(11) NOT NULL,
  `pass_word` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int(11) NULL DEFAULT NULL,
  `uid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for survey_user
-- ----------------------------
DROP TABLE IF EXISTS `survey_user`;
CREATE TABLE `survey_user`  (
  `id` bigint(20) NOT NULL,
  `user_name` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `uid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `pass_word` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int(1) NOT NULL,
  `update_time` datetime NOT NULL,
  `create_time` datetime NOT NULL,
  `ip` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `delete_flag` tinyint(4) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_name`(`user_name`) USING BTREE,
  UNIQUE INDEX `uid`(`uid`) USING BTREE
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

-- ----------------------------
-- Table structure for work_shop_products
-- ----------------------------
DROP TABLE IF EXISTS `work_shop_products`;
CREATE TABLE `work_shop_products`  (
  `id` bigint(11) NOT NULL,
  `rank` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `expect_value` double NULL DEFAULT NULL,
  `version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
