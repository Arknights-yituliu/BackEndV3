/*
 * BackEndV3 数据库建表脚本（以 Java 实体类与 Mapper XML 的实际访问行为为准）
 *
 * 运行环境要求：
 *   MySQL 8.0+（依赖 JSON 类型与 JSON_TABLE 函数，见 StageDropMapper.xml）
 *   字符集：utf8mb4 / utf8mb4_general_ci
 *
 * 说明：
 *   1. 脚本使用 CREATE TABLE IF NOT EXISTS，可在空库直接执行；如需重建表请先手动 DROP。
 *   2. 主键 id 均为 bigint，由应用层 IdGenerator（雪花算法）生成，不使用 AUTO_INCREMENT。
 *   3. 实体类中类型为 Long 的时间字段（如 updateTime）按毫秒时间戳存 bigint，
 *      类型为 java.util.Date 的时间字段按 datetime 存储。
 *   4. 动态月表 stage_drop_YYYY_MM / recruit_drop_YYYY_MM 需由运维按月预先创建，
 *      脚本末尾给出模板语句。
 *   5. 项目未引入 Flyway / Liquibase，也未开启 spring.sql.init，本脚本不会随应用启动自动执行，
 *      需手动导入（如 mysql -u root -p < schema.sql）。
 */

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET FOREIGN_KEY_CHECKS = 0;

/* ============================================================
 * 一、用户与账号绑定
 * ============================================================ */

-- 后台管理员表
CREATE TABLE IF NOT EXISTS `admin` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `developer` varchar(50) NOT NULL COMMENT '开发者标识',
  `email` varchar(255) NOT NULL COMMENT '邮箱',
  `level` int NOT NULL COMMENT '权限等级',
  `token` varchar(255) DEFAULT NULL COMMENT '登录令牌',
  `expire` datetime DEFAULT NULL COMMENT '令牌过期时间',
  PRIMARY KEY (`id`),
  KEY `idx_developer` (`developer`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='后台管理员表';

-- 本地用户表（历史账号体系遗留，当前用户中心已迁移至 OAuth）
CREATE TABLE IF NOT EXISTS `user_info` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户名',
  `pass_word` varchar(255) DEFAULT NULL COMMENT '密码',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '最后一次上传数据时间',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `status` int DEFAULT 1 COMMENT '用户状态：1正常，0封禁',
  `avatar` varchar(255) DEFAULT NULL COMMENT '用户头像',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_user_name` (`user_name`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='本地用户表（历史遗留）';

-- OAuth2 用户中心资料缓存表，id 即用户中心 uid
CREATE TABLE IF NOT EXISTS `oauth_user_info` (
  `id` bigint NOT NULL COMMENT '用户中心 uid',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '用户头像',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` int DEFAULT 1 COMMENT '用户状态：1正常，0封禁',
  `create_time` datetime DEFAULT NULL COMMENT '首次登录时间',
  `update_time` datetime DEFAULT NULL COMMENT '资料更新时间',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='OAuth2 用户中心资料缓存表';

-- 明日方舟账号绑定信息表
CREATE TABLE IF NOT EXISTS `ak_player_bind_info` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `ak_uid` varchar(50) NOT NULL COMMENT '明日方舟玩家uid',
  `ak_nick_name` varchar(100) DEFAULT NULL COMMENT '明日方舟昵称',
  `channel_name` varchar(50) DEFAULT NULL COMMENT '频道名称',
  `channel_master_id` int DEFAULT NULL COMMENT '频道id',
  `warehouse_info_id` bigint DEFAULT NULL COMMENT '最新仓库数据的标记id',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间（毫秒时间戳）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ak_uid` (`ak_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='明日方舟账号绑定信息表';

-- 一图流用户与外部账号绑定表
CREATE TABLE IF NOT EXISTS `user_external_account_binding` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `uid` bigint NOT NULL COMMENT '一图流用户uid',
  `ak_uid` varchar(50) NOT NULL COMMENT '明日方舟玩家uid',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间（毫秒时间戳）',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间（毫秒时间戳）',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid_ak_uid` (`uid`, `ak_uid`),
  KEY `idx_ak_uid` (`ak_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户与外部账号绑定表';

-- 用户仓库物品表（每次导入生成一批记录，由 last_data_id 标识批次）
CREATE TABLE IF NOT EXISTS `warehouse_info` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `uid` bigint NOT NULL COMMENT '一图流用户uid',
  `ak_uid` varchar(50) NOT NULL COMMENT '明日方舟玩家uid',
  `item_id` varchar(50) NOT NULL COMMENT '物品id',
  `quantity` int NOT NULL DEFAULT 0 COMMENT '数量',
  `last_data_id` bigint NOT NULL COMMENT '批次标记id',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间（毫秒时间戳）',
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`),
  KEY `idx_ak_uid` (`ak_uid`),
  KEY `idx_last_data_id` (`last_data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户仓库物品表';

/* ============================================================
 * 二、关卡与材料
 * ============================================================ */

-- 关卡表
CREATE TABLE IF NOT EXISTS `stage` (
  `stage_id` varchar(50) NOT NULL COMMENT '关卡id',
  `stage_code` varchar(50) DEFAULT NULL COMMENT '关卡code',
  `zone_id` varchar(50) DEFAULT NULL COMMENT '区域id',
  `zone_name` varchar(50) DEFAULT NULL COMMENT '区域名称',
  `ap_cost` int DEFAULT NULL COMMENT '理智消耗',
  `stage_type` varchar(50) DEFAULT NULL COMMENT '关卡类型',
  `start_time` datetime DEFAULT NULL COMMENT '开放时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `spm` double DEFAULT NULL COMMENT '每分钟理智效率',
  `min_clear_time` int DEFAULT NULL COMMENT '最短通关时间（毫秒）',
  `is_reproduction` int DEFAULT NULL COMMENT '是否复刻：1是，0否',
  PRIMARY KEY (`stage_id`),
  KEY `idx_zone_id` (`zone_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='关卡表';

-- 礼包信息表
CREATE TABLE IF NOT EXISTS `pack_info` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `official_name` varchar(255) DEFAULT NULL COMMENT '官方名称',
  `display_name` varchar(255) DEFAULT NULL COMMENT '展示名称',
  `price` double NOT NULL DEFAULT 0 COMMENT '售价',
  `sale_type` varchar(50) DEFAULT NULL COMMENT '售卖类型',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
  `originium` int DEFAULT NULL COMMENT '至纯源石数量',
  `orundum` int DEFAULT NULL COMMENT '合成玉数量',
  `gacha_ticket` int DEFAULT NULL COMMENT '寻访凭证数量',
  `ten_gacha_ticket` int DEFAULT NULL COMMENT '十连寻访凭证数量',
  `start` datetime DEFAULT NULL COMMENT '上架时间',
  `end` datetime DEFAULT NULL COMMENT '下架时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `note` varchar(500) DEFAULT NULL COMMENT '备注',
  `content` text COMMENT '礼包内容',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='礼包信息表';

-- 活动商店数据表
CREATE TABLE IF NOT EXISTS `activity_store_data` (
  `act_name` varchar(255) NOT NULL COMMENT '活动名称',
  `end_time` datetime DEFAULT NULL COMMENT '活动结束时间',
  `store_data` longtext COMMENT '活动商店JSON数据',
  PRIMARY KEY (`act_name`),
  KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='活动商店数据表';

-- 图片资源表
CREATE TABLE IF NOT EXISTS `image_info` (
  `image_name` varchar(255) NOT NULL COMMENT '图片名称（业务主键）',
  `image_link` varchar(500) DEFAULT NULL COMMENT '图片链接',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`image_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='图片资源表';

/* ============================================================
 * 三、MAA 上传数据
 * ============================================================ */

/*
 * 关卡掉落上传月表模板 stage_drop_YYYY_MM
 * 表名由 StageDropUploadService 按上传时间动态生成，需按月预先创建。
 * 示例：2026 年 10 月表名为 stage_drop_2026_10
 */
CREATE TABLE IF NOT EXISTS `stage_drop_2026_10` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `stage_id` varchar(50) NOT NULL COMMENT '关卡id',
  `drops` json NOT NULL COMMENT '掉落明细JSON数组，元素包含 itemId、quantity',
  `times` int NOT NULL DEFAULT 1 COMMENT '作战次数',
  `server` varchar(20) NOT NULL DEFAULT 'CN' COMMENT '服务器地区',
  `source` varchar(100) DEFAULT NULL COMMENT '数据来源',
  `create_time` datetime(3) NOT NULL COMMENT '创建时间',
  `uid` varchar(50) NOT NULL COMMENT '玩家uid',
  `version` varchar(50) DEFAULT NULL COMMENT 'MAA版本号',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_stage_id` (`stage_id`),
  KEY `idx_stage_server` (`stage_id`, `server`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='关卡掉落上传月表（按月上表）';

/*
 * 公招数据上传月表模板 recruit_drop_YYYY_MM
 * 表名由 RecruitTagUploadService 按上传时间动态生成，需按月预先创建。
 * 示例：2026 年 10 月表名为 recruit_drop_2026_10
 */
CREATE TABLE IF NOT EXISTS `recruit_drop_2026_10` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `uid` varchar(50) NOT NULL COMMENT '企鹅物流id',
  `tag` varchar(255) DEFAULT NULL COMMENT 'tag组合',
  `level` int DEFAULT NULL COMMENT '本次招募最高星级',
  `create_time` bigint NOT NULL COMMENT '创建时间（毫秒时间戳）',
  `server` varchar(20) NOT NULL DEFAULT 'CN' COMMENT '服务器地区',
  `source` varchar(100) DEFAULT NULL COMMENT '数据来源',
  `version` varchar(50) DEFAULT NULL COMMENT 'MAA版本号',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公招数据上传月表（按月上表）';

-- 公招统计结果表
CREATE TABLE IF NOT EXISTS `recruit_statistics` (
  `statistical_item` varchar(100) NOT NULL COMMENT '统计项名称',
  `statistical_result` int DEFAULT NULL COMMENT '统计结果',
  PRIMARY KEY (`statistical_item`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公招统计结果表';

-- 回收站收获记录表
CREATE TABLE IF NOT EXISTS `recycle_drop` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `drops` json NOT NULL COMMENT '收获明细JSON',
  `server` varchar(20) NOT NULL COMMENT '服务器地区',
  `source` varchar(100) NOT NULL COMMENT '数据来源',
  `create_time` datetime(3) NOT NULL COMMENT '创建时间',
  `uid` varchar(50) NOT NULL COMMENT '玩家uid',
  `version` varchar(50) NOT NULL COMMENT 'MAA版本号',
  PRIMARY KEY (`id`),
  KEY `idx_recycle_drop_create_time` (`create_time`),
  KEY `idx_recycle_drop_uid_create_time` (`uid`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='回收站收获记录表';

/* ============================================================
 * 四、掉落统计
 * ============================================================ */

-- 关卡掉落日/周等粒度统计表
CREATE TABLE IF NOT EXISTS `stage_drop_statistics` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `stage_id` varchar(50) NOT NULL COMMENT '关卡id',
  `item_id` varchar(50) NOT NULL COMMENT '物品id',
  `times` bigint NOT NULL DEFAULT 0 COMMENT '作战次数',
  `quantity` bigint NOT NULL DEFAULT 0 COMMENT '掉落总数量',
  `start_time` datetime NOT NULL COMMENT '统计区间开始时间',
  `end_time` datetime NOT NULL COMMENT '统计区间结束时间',
  `time_granularity` int NOT NULL COMMENT '时间粒度，见 TimeGranularity 枚举',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `record_id` bigint DEFAULT NULL COMMENT '统计记录批次id',
  `record_code` int NOT NULL DEFAULT 1 COMMENT '记录状态：1=展示，-1=过期',
  PRIMARY KEY (`id`),
  KEY `idx_granularity_start` (`time_granularity`, `start_time`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_stage_id` (`stage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='关卡掉落统计表';

-- 关卡掉落小时统计表
CREATE TABLE IF NOT EXISTS `stage_drop_hour_statistics` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `stage_id` varchar(50) NOT NULL COMMENT '关卡id',
  `item_id` varchar(50) NOT NULL COMMENT '物品id',
  `times` bigint NOT NULL DEFAULT 0 COMMENT '作战次数',
  `quantity` bigint NOT NULL DEFAULT 0 COMMENT '掉落总数量',
  `start_time` datetime NOT NULL COMMENT '统计区间开始时间',
  `end_time` datetime NOT NULL COMMENT '统计区间结束时间',
  `time_granularity` int NOT NULL COMMENT '时间粒度，见 TimeGranularity 枚举',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `record_id` bigint DEFAULT NULL COMMENT '统计记录批次id',
  `record_code` int NOT NULL DEFAULT 1 COMMENT '记录状态：1=展示，-1=过期',
  PRIMARY KEY (`id`),
  KEY `idx_granularity_start` (`time_granularity`, `start_time`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_stage_id` (`stage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='关卡掉落小时统计表';

-- 掉落统计任务日志表
CREATE TABLE IF NOT EXISTS `stage_drop_statistics_task_log` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `time_granularity` int NOT NULL COMMENT '时间粒度',
  `data_count` int NOT NULL DEFAULT 0 COMMENT '统计产生或处理的数据条数',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `start_time` datetime NOT NULL COMMENT '统计区间开始时间',
  `end_time` datetime NOT NULL COMMENT '统计区间结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_granularity_start` (`time_granularity`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='掉落统计任务日志表';

/* ============================================================
 * 五、问卷与干员统计
 * ============================================================ */

-- 问卷原始结果表
CREATE TABLE IF NOT EXISTS `questionnaire_result` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `uid` bigint DEFAULT NULL COMMENT '提交用户uid',
  `content` longtext COMMENT '问卷内容JSON',
  `questionnaire_code` int NOT NULL COMMENT '问卷编号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `ip` varchar(50) DEFAULT NULL COMMENT '提交IP',
  PRIMARY KEY (`id`),
  KEY `idx_code_uid_create` (`questionnaire_code`, `uid`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='问卷原始结果表';

-- 干员携带率统计明细表
CREATE TABLE IF NOT EXISTS `operator_carry_rate` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `char_id` varchar(50) NOT NULL COMMENT '干员id',
  `carry_count` int NOT NULL DEFAULT 0 COMMENT '携带次数',
  `sample_size` int NOT NULL DEFAULT 0 COMMENT '样本量',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `questionnaire_code` int NOT NULL COMMENT '问卷编号',
  `start_time` datetime NOT NULL COMMENT '统计区间开始时间',
  `end_time` datetime NOT NULL COMMENT '统计区间结束时间',
  `record_type` int NOT NULL DEFAULT 1 COMMENT '记录状态',
  PRIMARY KEY (`id`),
  KEY `idx_code_start_time` (`questionnaire_code`, `start_time`),
  KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='干员携带率统计明细表';

-- 干员携带率统计结果表
CREATE TABLE IF NOT EXISTS `operator_carry_rate_statistics` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `questionnaire_code` int NOT NULL COMMENT '问卷编号',
  `questionnaire_type` varchar(50) DEFAULT NULL COMMENT '问卷类型',
  `start_time` datetime NOT NULL COMMENT '统计区间开始时间',
  `end_time` datetime NOT NULL COMMENT '统计区间结束时间',
  `result` longtext COMMENT '统计结果JSON',
  `time_granularity` int DEFAULT NULL COMMENT '时间粒度',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_code_period` (`questionnaire_code`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='干员携带率统计结果表';

-- 干员练度原始数据表
CREATE TABLE IF NOT EXISTS `operator_progression_data` (
  `ak_uid` varchar(255) NOT NULL COMMENT '明日方舟玩家uid',
  `operator_progression` longtext COMMENT '干员练度JSON',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`ak_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='干员练度原始数据表';

-- 干员练度人工修正数据表
CREATE TABLE IF NOT EXISTS `operator_progression_manual_data` (
  `ak_uid` varchar(255) NOT NULL COMMENT '明日方舟玩家uid',
  `operator_progression` longtext COMMENT '干员练度JSON',
  `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ak_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='干员练度人工修正数据表';

-- 干员练度统计结果表
CREATE TABLE IF NOT EXISTS `operator_progression_statistical_result` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `sample_size` int NOT NULL DEFAULT 0 COMMENT '样本量',
  `statistical_result` longtext COMMENT '统计结果JSON',
  `record_type` int NOT NULL DEFAULT 1 COMMENT '记录状态',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_type` (`record_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='干员练度统计结果表';

/* ============================================================
 * 六、基建排班
 * ============================================================ */

-- 基建排班表（旧版单条记录）
CREATE TABLE IF NOT EXISTS `schedule` (
  `uid` bigint NOT NULL COMMENT '用户uid',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `nick_name` varchar(30) DEFAULT NULL COMMENT '昵称',
  `schedule_id` bigint DEFAULT NULL COMMENT '排班表id',
  `schedule` mediumtext COMMENT '排班表JSON文本',
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='基建排班表（旧版）';

-- 基建排班表（RIIC，单用户多条）
CREATE TABLE IF NOT EXISTS `riic_schedule` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `uid` bigint NOT NULL COMMENT '所属用户uid',
  `schedule` text COMMENT '排班表JSON文本',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='基建排班表';

/* ============================================================
 * 七、访问统计
 * ============================================================ */

-- 访问日志明细表
CREATE TABLE IF NOT EXISTS `access_log` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `url` varchar(500) DEFAULT NULL COMMENT '访问URL',
  `ip` varchar(50) DEFAULT NULL COMMENT '访问IP',
  `region` varchar(100) DEFAULT NULL COMMENT 'IP归属地',
  `referer` varchar(500) DEFAULT NULL COMMENT '来源页面',
  `device` varchar(20) DEFAULT NULL COMMENT '设备类型',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `user_agent` text COMMENT 'User-Agent',
  `access_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_access_time` (`access_time`),
  KEY `idx_url` (`url`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='访问日志明细表';

-- 每小时访问量统计表（重跑生成新 task_id，旧记录置为 EXPIRE）
CREATE TABLE IF NOT EXISTS `access_log_hourly_stats` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `stat_hour` datetime NOT NULL COMMENT '统计的小时（整点）',
  `visit_count` bigint NOT NULL DEFAULT 0 COMMENT '该小时总访问量',
  `task_id` bigint NOT NULL COMMENT '统计任务ID',
  `record_code` int NOT NULL DEFAULT 1 COMMENT '记录状态：1=展示，-1=过期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_stat_hour_code` (`stat_hour`, `record_code`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每小时访问量统计表';

-- 小时访问量统计任务记录表
CREATE TABLE IF NOT EXISTS `access_log_hourly_stats_task` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `stat_hour` datetime NOT NULL COMMENT '统计的小时（整点）',
  `task_id` bigint NOT NULL COMMENT '该小时当前有效的任务ID',
  `data_count` bigint NOT NULL DEFAULT 0 COMMENT '该小时访问量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近一次重新统计时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_hour` (`stat_hour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='小时访问量统计任务记录表';

-- 每个URL每日访问量统计表
CREATE TABLE IF NOT EXISTS `access_log_url_daily_stats` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `url` varchar(500) DEFAULT NULL COMMENT 'URL（已去除结尾斜杠）',
  `stat_day` datetime NOT NULL COMMENT '统计日期（当天 00:00:00）',
  `visit_count` bigint NOT NULL DEFAULT 0 COMMENT '该URL当天访问量',
  `task_id` bigint NOT NULL COMMENT '统计任务ID',
  `record_code` int NOT NULL DEFAULT 1 COMMENT '记录状态：1=展示，-1=过期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_stat_day_code` (`stat_day`, `record_code`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_url` (`url`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每个URL每日访问量统计表';

-- URL每日访问量统计任务记录表
CREATE TABLE IF NOT EXISTS `access_log_url_daily_stats_task` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `stat_day` datetime NOT NULL COMMENT '统计日期（当天 00:00:00）',
  `task_id` bigint NOT NULL COMMENT '该日当前有效的任务ID',
  `data_count` bigint NOT NULL DEFAULT 0 COMMENT '该日统计的URL数量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近一次重新统计时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_day` (`stat_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='URL每日访问量统计任务记录表';

/* ============================================================
 * 八、运维日志
 * ============================================================ */

-- 开发日志表
CREATE TABLE IF NOT EXISTS `develop_log` (
  `id` bigint NOT NULL COMMENT '主键，一般取当前时间戳',
  `tag` varchar(50) DEFAULT NULL COMMENT '标签',
  `author` varchar(50) DEFAULT NULL COMMENT '作者',
  `text` text COMMENT '日志内容',
  `commit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开发日志表';

-- 系统运行日志表
CREATE TABLE IF NOT EXISTS `log_info` (
  `id` bigint NOT NULL COMMENT '主键，雪花ID',
  `message` text COMMENT '日志内容',
  `log_type` varchar(50) DEFAULT NULL COMMENT '日志类型',
  `api_path` varchar(255) DEFAULT NULL COMMENT '接口路径',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_log_type` (`log_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统运行日志表';

SET FOREIGN_KEY_CHECKS = 1;
