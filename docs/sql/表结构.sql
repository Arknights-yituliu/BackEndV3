/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50735 (5.7.35-log)
 Source Host           : localhost:3306
 Source Schema         : yituliu

 Target Server Type    : MySQL
 Target Server Version : 50735 (5.7.35-log)
 File Encoding         : 65001

 Date: 02/02/2024 15:31:04
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` bigint(20) NOT NULL,
  `developer` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `email` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `level` int(11) NOT NULL,
  `token` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `expire` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ak_player_bind_info
-- ----------------------------
DROP TABLE IF EXISTS `ak_player_bind_info`;
CREATE TABLE `ak_player_bind_info`  (
  `id` bigint(20) NOT NULL,
  `delete_flag` bit(1) NOT NULL,
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_time` bigint(20) NULL DEFAULT NULL,
  `ak_uid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pack_info
-- ----------------------------
DROP TABLE IF EXISTS `pack_info`;
CREATE TABLE `pack_info`  (
  `id` bigint(11) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `display_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `price` double NOT NULL,
  `end` datetime(6) NULL DEFAULT NULL,
  `ticket_gacha` int(11) NULL DEFAULT NULL,
  `ticket_gacha10` int(11) NULL DEFAULT NULL,
  `originium` int(11) NULL DEFAULT NULL,
  `orundum` int(11) NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `state` int(11) NULL DEFAULT NULL,
  `start` datetime(6) NULL DEFAULT NULL,
  `sort_id` bigint(20) NULL DEFAULT NULL,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
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
  `nick_name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
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
  `is_reproduction` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`stage_id`) USING BTREE
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
-- Table structure for oauth_user_info
-- ----------------------------
DROP TABLE IF EXISTS `oauth_user_info`;
CREATE TABLE `oauth_user_info`  (
  `id` bigint(20) NOT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int(11) NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `delete_flag` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for token_record
-- ----------------------------
DROP TABLE IF EXISTS `token_record`;
CREATE TABLE `token_record`  (
  `id` bigint(20) NOT NULL,
  `uid` bigint(20) NULL DEFAULT NULL,
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scope` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uid`(`uid`) USING BTREE,
  INDEX `idx_token`(`token`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for access_log
-- ----------------------------
DROP TABLE IF EXISTS `access_log`;
CREATE TABLE `access_log`  (
  `id` bigint(20) NOT NULL,
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `region` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `referer` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `device` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_agent` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `access_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_access_time`(`access_time`) USING BTREE,
  INDEX `idx_url`(`url`(100)) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for access_log_hourly_stats
-- 每小时访问量统计表，由定时任务每小时统计上一完整小时写入
-- 同一小时重跑会生成新 task_id 的记录并将旧记录置为 EXPIRE，API 仅读 DISPLAY 记录
-- ----------------------------
DROP TABLE IF EXISTS `access_log_hourly_stats`;
CREATE TABLE `access_log_hourly_stats`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID，由应用层 IdGenerator 生成',
  `stat_hour` datetime NOT NULL COMMENT '统计的小时（整点，例如 2026-08-04 14:00:00）',
  `visit_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '该小时的总访问量',
  `task_id` bigint(20) NOT NULL COMMENT '本次统计的任务ID，关联 access_log_hourly_stats_task.task_id',
  `record_code` int(11) NOT NULL DEFAULT 1 COMMENT '记录状态：1=展示数据(DISPLAY)，-1=过期数据(EXPIRE)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_stat_hour_code`(`stat_hour`, `record_code`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '每小时访问量统计表';

-- ----------------------------
-- Table structure for access_log_hourly_stats_task
-- 小时访问量统计任务记录表，记录每个统计小时的执行情况与当前有效 task_id
-- ----------------------------
DROP TABLE IF EXISTS `access_log_hourly_stats_task`;
CREATE TABLE `access_log_hourly_stats_task`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID，由应用层 IdGenerator 生成',
  `stat_hour` datetime NOT NULL COMMENT '统计的小时（整点）',
  `task_id` bigint(20) NOT NULL COMMENT '该小时当前有效的任务ID，关联 access_log_hourly_stats.task_id',
  `data_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '该小时的访问量',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近一次重新统计时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stat_hour`(`stat_hour`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '小时访问量统计任务记录表';

-- ----------------------------
-- Table structure for access_log_url_daily_stats
-- 每个URL每日访问量统计表，由定时任务每天统计前一天写入
-- 同一天重跑会生成新 task_id 的记录并将旧记录置为 EXPIRE，API 仅读 DISPLAY 记录
-- ----------------------------
DROP TABLE IF EXISTS `access_log_url_daily_stats`;
CREATE TABLE `access_log_url_daily_stats`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID，由应用层 IdGenerator 生成',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'URL（已去除结尾斜杠）',
  `stat_day` datetime NOT NULL COMMENT '统计的日期（当天 00:00:00）',
  `visit_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '该URL当天的访问量',
  `task_id` bigint(20) NOT NULL COMMENT '本次统计的任务ID，关联 access_log_url_daily_stats_task.task_id',
  `record_code` int(11) NOT NULL DEFAULT 1 COMMENT '记录状态：1=展示数据(DISPLAY)，-1=过期数据(EXPIRE)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_stat_day_code`(`stat_day`, `record_code`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_url`(`url`(100)) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '每个URL每日访问量统计表';

-- ----------------------------
-- Table structure for access_log_url_daily_stats_task
-- URL每日访问量统计任务记录表，记录每个统计日的执行情况与当前有效 task_id
-- ----------------------------
DROP TABLE IF EXISTS `access_log_url_daily_stats_task`;
CREATE TABLE `access_log_url_daily_stats_task`  (
  `id` bigint(20) NOT NULL COMMENT '主键ID，由应用层 IdGenerator 生成',
  `stat_day` datetime NOT NULL COMMENT '统计的日期（当天 00:00:00）',
  `task_id` bigint(20) NOT NULL COMMENT '该日当前有效的任务ID，关联 access_log_url_daily_stats.task_id',
  `data_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '该日统计的URL数量',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近一次重新统计时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stat_day`(`stat_day`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = 'URL每日访问量统计任务记录表';

SET FOREIGN_KEY_CHECKS = 1;
