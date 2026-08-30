package com.lhs.common.util;

/**
 * Redis Key 统一构建工具
 *
 * <p>统一管理 key 的拼接规则，避免散落各处导致冲突或格式不一致。
 * 注意：所有方法返回的 key 字符串与历史硬编码完全一致，保证线上数据兼容。</p>
 *
 * @author UserCenter
 */
public final class RedisKeyUtil {

    // ==================== 认证/会话 ====================

    /** 用户登录 token → uid 的映射 key 前缀：loginToken:{token} */
    private static final String PREFIX_LOGIN_TOKEN = "loginToken:";

    /** OAuth2 授权 state → code_verifier 缓存 key 前缀：oauth2:auth:state:{state} */
    private static final String PREFIX_OAUTH2_STATE = "oauth2:auth:state:";

    /** OpenAPI 访问令牌数据 key 前缀：open-api-token:{token} */
    private static final String PREFIX_OPEN_API_TOKEN = "open-api-token:";

    // ==================== 验证码 ====================

    /** 邮件服务邮箱验证码 key 前缀：CODE:CODE.{email} */
    private static final String PREFIX_EMAIL_CODE = "CODE:CODE.";

    /** 管理员邮箱验证码 key 格式：CODE:{email}CODE（历史格式保留） */
    private static final String PREFIX_ADMIN_EMAIL_CODE = "CODE:";
    private static final String SUFFIX_ADMIN_EMAIL_CODE = "CODE";

    // ==================== 限流/防重 ====================

    /** 问卷接口 IP 频控 key 前缀：IP:{encryptedIp} */
    private static final String PREFIX_IP_RATE = "IP:";

    /** 问卷提交频控 key 前缀：SurveySubmitterIP:{ip} */
    private static final String PREFIX_SURVEY_SUBMITTER = "SurveySubmitterIP:";

    /** 干员数据上传间隔防重 key 前缀：SurveyOperatorInfoUploadInterval:{uid} */
    private static final String PREFIX_OPERATOR_UPLOAD_INTERVAL = "SurveyOperatorInfoUploadInterval:";

    /** 掉落数据防重锁 key 前缀：StageDropLimit:{penguinId} */
    private static final String PREFIX_STAGE_DROP_LOCK = "StageDropLimit:";

    /** 1-7 每日上传上限计数 key：1-7_MAX_UPLOADS_PER_DAY */
    private static final String KEY_STAGE_ONE_SEVEN_DAILY_UPLOAD = "1-7_MAX_UPLOADS_PER_DAY";

    // ==================== 数据缓存 ====================

    /** 干员标签数据缓存 key：Tag:OperatorData */
    private static final String KEY_OPERATOR_DATA_TAG = "Tag:OperatorData";

    /** 礼包数据缓存 key：Item:PackInfo（供 @RedisCacheable 注解引用） */
    public static final String PACK_INFO_KEY = "Item:PackInfo";

    /** 礼包数据版本 key：Item:PackInfoVersion */
    private static final String KEY_PACK_INFO_VERSION = "Item:PackInfoVersion";

    /** 活动商店缓存 key：Item:ActStoreInfo（供 @RedisCacheable 注解引用） */
    public static final String ACT_STORE_INFO_KEY = "Item:ActStoreInfo";

    /** 关卡数据缓存 key：StageInfoMap */
    private static final String KEY_STAGE_INFO_MAP = "StageInfoMap";

    /** 干员表数据缓存 key 前缀：CharacterTable:{version} */
    private static final String PREFIX_CHARACTER_TABLE = "CharacterTable:";

    /** 公招统计时间 key：LastRecruitStatisticsTime */
    private static final String KEY_LAST_RECRUIT_STATISTICS_TIME = "LastRecruitStatisticsTime";

    // ==================== 统计/游标 ====================

    /** 每日邮件计数 key 前缀：email:daily:{date} */
    private static final String PREFIX_EMAIL_DAILY = "email:daily:";

    /** 小时访问统计回填游标 key：BACKFILL:HOUR:ACCESS2:STATS:CURSOR */
    private static final String KEY_BACKFILL_HOUR_CURSOR = "BACKFILL:HOUR:ACCESS2:STATS:CURSOR";

    /** 日访问统计回填游标 key：BACKFILL:DAY:URL:ACCESS2:STATS:CURSOR */
    private static final String KEY_BACKFILL_DAY_CURSOR = "BACKFILL:DAY:URL:ACCESS2:STATS:CURSOR";

    /** 访问数据迁移最近同步日期 key：migrate:lastSyncedDate */
    private static final String KEY_MIGRATE_LAST_SYNCED_DATE = "migrate:lastSyncedDate";

    private RedisKeyUtil() {
    }

    /**
     * 用户登录 token → uid 的映射 key
     *
     * @param token 登录 token
     * @return Redis key
     */
    public static String loginToken(String token) {
        return PREFIX_LOGIN_TOKEN + token;
    }

    /**
     * OAuth2 授权 state 缓存 key（存放 code_verifier）
     *
     * @param state 授权 state
     * @return Redis key
     */
    public static String oauth2State(String state) {
        return PREFIX_OAUTH2_STATE + state;
    }

    /**
     * OpenAPI 访问令牌数据 key
     *
     * @param token OpenAPI token
     * @return Redis key
     */
    public static String openApiToken(String token) {
        return PREFIX_OPEN_API_TOKEN + token;
    }

    /**
     * 邮件服务邮箱验证码 key
     *
     * @param email 邮箱地址
     * @return Redis key
     */
    public static String emailCode(String email) {
        return PREFIX_EMAIL_CODE + email;
    }

    /**
     * 管理员邮箱验证码 key（历史格式 CODE:{email}CODE 保留）
     *
     * @param email 管理员邮箱
     * @return Redis key
     */
    public static String adminEmailCode(String email) {
        return PREFIX_ADMIN_EMAIL_CODE + email + SUFFIX_ADMIN_EMAIL_CODE;
    }

    /**
     * 问卷接口 IP 频控 key
     *
     * @param encryptedIp 加密后的 IP
     * @return Redis key
     */
    public static String ipRate(String encryptedIp) {
        return PREFIX_IP_RATE + encryptedIp;
    }

    /**
     * 问卷提交频控 key
     *
     * @param ip 提交者 IP
     * @return Redis key
     */
    public static String surveySubmitterIp(String ip) {
        return PREFIX_SURVEY_SUBMITTER + ip;
    }

    /**
     * 干员数据上传间隔防重 key
     *
     * @param uid 用户 uid
     * @return Redis key
     */
    public static String surveyOperatorUploadInterval(Long uid) {
        return PREFIX_OPERATOR_UPLOAD_INTERVAL + uid;
    }

    /**
     * 掉落数据 5 秒防重锁 key
     *
     * @param penguinId 企鹅物流用户 ID
     * @return Redis key
     */
    public static String stageDropLock(String penguinId) {
        return PREFIX_STAGE_DROP_LOCK + penguinId;
    }

    /**
     * 1-7 每日上传上限计数 key
     *
     * @return Redis key
     */
    public static String stageOneSevenDailyUpload() {
        return KEY_STAGE_ONE_SEVEN_DAILY_UPLOAD;
    }

    /**
     * 干员标签数据缓存 key
     *
     * @return Redis key
     */
    public static String operatorDataTag() {
        return KEY_OPERATOR_DATA_TAG;
    }

    /**
     * 礼包数据缓存 key
     *
     * @return Redis key
     */
    public static String packInfo() {
        return PACK_INFO_KEY;
    }

    /**
     * 礼包数据版本 key
     *
     * @return Redis key
     */
    public static String packInfoVersion() {
        return KEY_PACK_INFO_VERSION;
    }

    /**
     * 活动商店缓存 key
     *
     * @return Redis key
     */
    public static String actStoreInfo() {
        return ACT_STORE_INFO_KEY;
    }

    /**
     * 关卡数据缓存 key
     *
     * @return Redis key
     */
    public static String stageInfoMap() {
        return KEY_STAGE_INFO_MAP;
    }

    /**
     * 干员表数据缓存 key
     *
     * @param version 干员表版本（如 2026-07-08 14:20）
     * @return Redis key
     */
    public static String characterTable(String version) {
        return PREFIX_CHARACTER_TABLE + version;
    }

    /**
     * 公招统计时间 key
     *
     * @return Redis key
     */
    public static String lastRecruitStatisticsTime() {
        return KEY_LAST_RECRUIT_STATISTICS_TIME;
    }

    /**
     * 每日邮件计数 key
     *
     * @param date 日期（yyyy-MM-dd）
     * @return Redis key
     */
    public static String emailDaily(String date) {
        return PREFIX_EMAIL_DAILY + date;
    }

    /**
     * 小时访问统计回填游标 key
     *
     * @return Redis key
     */
    public static String backfillHourlyCursor() {
        return KEY_BACKFILL_HOUR_CURSOR;
    }

    /**
     * 日访问统计回填游标 key
     *
     * @return Redis key
     */
    public static String backfillDayCursor() {
        return KEY_BACKFILL_DAY_CURSOR;
    }

    /**
     * 访问数据迁移最近同步日期 key
     *
     * @return Redis key
     */
    public static String migrateLastSyncedDate() {
        return KEY_MIGRATE_LAST_SYNCED_DATE;
    }
}
