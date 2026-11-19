package com.lhs.service.maa;

import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.entity.dto.drop.RecycleDropDTO;
import com.lhs.entity.dto.drop.RecycleDropItemDTO;
import com.lhs.entity.po.material.RecycleDrop;
import com.lhs.mapper.material.RecycleDropMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 回收站产出上报服务。
 * <p>
 * 处理流程：校验请求体 → 解析 authorization 头中的玩家 uid → 通过 Redis 做防重复提交
 * → 组装实体并写入 recycle_drop 表。
 * <p>
 * 设计约定：
 * <ul>
 *   <li>任何校验不通过的情况均静默返回，不抛异常、不返回错误码，避免上报端因异常反复重试；</li>
 *   <li>产出明细按物品拆分为多条记录写入 recycle_drop，每条记录只保存一个物品 id 及其产出时间；</li>
 *   <li>主键由应用层雪花算法生成，不依赖数据库自增。</li>
 * </ul>
 */
@Service
public class RecycleDropUploadService {

    /**
     * 同一玩家两次上报的最小间隔（秒），用于防重复提交。
     * <p>
     * 该值对应 Redis 锁的过期时间：锁写入后 5 秒内相同 uid 的后续请求会被直接丢弃。
     */
    private static final long UPLOAD_LOCK_SECONDS = 5;

    /**
     * 单条产出明细的去重窗口（小时）。
     * <p>
     * 以 uid + 物品 id + 产出时间作为唯一键，窗口内相同明细只落库一次，
     * 用于抵消客户端重传或多次上报同一批数据造成的重复记录。
     */
    private static final long DEDUP_EXPIRE_HOURS = 24;

    /**
     * 单次上报的产出条目数上限。
     * <p>
     * 回收站一次结算的物品数量有限，超过该值视为异常报文，防止一次写入过多记录。
     */
    private static final int MAX_DROP_ITEMS = 100;

    /**
     * 单条产出明细字符串的长度上限。
     * <p>
     * 与 recycle_drop.item_id 列宽（varchar(100)）保持一致，超长会导致写入失败。
     */
    private static final int MAX_ITEM_LENGTH = 100;

    /** 回收站产出数据访问层，批量插入走自定义 SQL */
    private final RecycleDropMapper recycleDropMapper;

    /** Redis 操作模板，仅用于 setIfAbsent 实现防重复提交锁 */
    private final RedisTemplate<String, Object> redisTemplate;

    /** 雪花 ID 生成器，workerId 固定为 1，主键在应用层生成而非数据库自增 */
    private final IdGenerator idGenerator;

    /**
     * 构造回收站产出上报服务。
     *
     * @param recycleDropMapper 回收站产出数据访问层
     * @param redisTemplate     Redis 操作模板，用于实现防重复提交锁
     */
    public RecycleDropUploadService(RecycleDropMapper recycleDropMapper,
                                    RedisTemplate<String, Object> redisTemplate) {
        this.recycleDropMapper = recycleDropMapper;
        this.redisTemplate = redisTemplate;
        // workerId 固定为 1：当前仅单实例部署，若后续多实例需按实例编号分配，避免同毫秒内 ID 冲突
        this.idGenerator = new IdGenerator(1L);
    }

    /**
     * 保存一次回收站产出上报。
     * <p>
     * 处理步骤：
     * <ol>
     *   <li>校验 server、source、version 非空，且产出明细合法；任一不满足直接返回；</li>
     *   <li>从 authorization 请求头解析 uid：按空白字符切分，取第二个片段（形如 "Bearer xxx"）；</li>
     *   <li>以 uid 为键写入 Redis 锁，写入失败说明 5 秒内已上报过，直接丢弃本次请求；</li>
     *   <li>按物品拆分组装 RecycleDrop 实体，每个物品写入一条记录。</li>
     * </ol>
     * 参数或鉴权信息不合法时静默丢弃，不抛异常；同一 uid 在 5 秒内重复上报只保留第一次。
     * 单条明细以 uid + 物品 id + 产出时间为唯一键，24 小时内重复出现时跳过该条。
     *
     * @param request HTTP 请求，从 authorization 请求头的第二个片段解析玩家 uid
     * @param dto     上报内容，包含产出明细、服务器、来源与客户端版本
     */
    public void saveRecycleDrop(HttpServletRequest request, RecycleDropDTO dto) {
        // 基础字段与明细校验：server/source/version 用于区分数据来源，缺失则数据无法归因
        if (dto == null || !hasText(dto.getServer()) || !hasText(dto.getSource())
                || !hasText(dto.getVersion()) || !validDrops(dto.getDrops())) {
            return;
        }

        // 解析鉴权头：约定格式为 "Bearer {uid}"，无鉴权头则无法定位玩家，直接丢弃
        String authorization = request.getHeader("authorization");
        if (authorization == null) {
            return;
        }
        String[] auth = authorization.trim().split("\\s+");
        // 至少两段且 uid 非空；长度限制 50 与 recycle_drop.uid 字段长度保持一致
        if (auth.length < 2 || !hasText(auth[1]) || auth[1].length() > 50) {
            return;
        }

        String uid = auth[1];
        // 防重复提交：setIfAbsent 原子写入，键不存在才写入成功；值为时间戳便于排查锁写入时间
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                RedisKeyUtil.recycleDropLock(uid), System.currentTimeMillis(),
                UPLOAD_LOCK_SECONDS, TimeUnit.SECONDS);
        // locked 为 false 表示键已存在，说明该 uid 在 5 秒锁定期内已提交过，本次忽略
        if (Boolean.FALSE.equals(locked)) {
            return;
        }

        // 组装落库实体：列表按物品拆分为多条记录，每条记录只保存一个物品 id 及其产出时间
        Date now = new Date();
        List<RecycleDrop> records = new ArrayList<>(dto.getDrops().size());
        for (RecycleDropItemDTO drop : dto.getDrops()) {
            // 明细级去重：uid + 物品 id + 产出时间 在 24 小时内只写入一次，
            // setIfAbsent 原子写入，键已存在说明该明细已上报过，跳过本条
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(
                    RedisKeyUtil.recycleDropDedup(uid, drop.getItemId(), drop.getDropTime()),
                    System.currentTimeMillis(), DEDUP_EXPIRE_HOURS, TimeUnit.HOURS);
            if (!Boolean.TRUE.equals(absent)) {
                continue;
            }
            RecycleDrop recycleDrop = new RecycleDrop();
            recycleDrop.setId(idGenerator.nextId());
            recycleDrop.setItemId(drop.getItemId());
            recycleDrop.setDropTime(new Date(drop.getDropTime()));
            // 干员列表以 JSON 数组字符串写入 json 列，未上报时存空数组便于查询
            recycleDrop.setOperators(drop.getOperators() == null ? "[]" : JsonMapper.toJSONString(drop.getOperators()));
            recycleDrop.setServer(dto.getServer());
            recycleDrop.setSource(dto.getSource());
            recycleDrop.setUid(uid);
            recycleDrop.setVersion(dto.getVersion());
            recycleDrop.setCreateTime(now);
            records.add(recycleDrop);
        }
        // 全部明细都在去重窗口内出现过时无需写库，避免执行空值 INSERT
        if (records.isEmpty()) {
            return;
        }
        // 单条 INSERT 批量写入，减少与数据库的往返次数
        recycleDropMapper.insertBatch(records);
    }

    /**
     * 校验产出明细列表是否合法。
     * <p>
     * 校验规则：
     * <ul>
     *   <li>列表不能为 null 或空，且条目数不超过 {@link #MAX_DROP_ITEMS}；</li>
     *   <li>每条明细不能为 null，其 itemId 非空白且长度不超过 {@link #MAX_ITEM_LENGTH}；</li>
     *   <li>每条明细的 dropTime（毫秒时间戳）必须为正数。</li>
     * </ul>
     *
     * @param drops 待校验的产出明细
     * @return 全部校验通过返回 true，否则返回 false
     */
    private boolean validDrops(List<RecycleDropItemDTO> drops) {
        if (drops == null || drops.isEmpty() || drops.size() > MAX_DROP_ITEMS) {
            return false;
        }
        for (RecycleDropItemDTO drop : drops) {
            if (drop == null || !hasText(drop.getItemId()) || drop.getItemId().length() > MAX_ITEM_LENGTH
                    || drop.getDropTime() == null || drop.getDropTime() <= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否包含非空白内容。
     *
     * @param value 待判断的字符串
     * @return 非 null 且去除首尾空白后非空返回 true
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
