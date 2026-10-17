package com.lhs.service.maa;

import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.entity.dto.drop.RecycleDropDTO;
import com.lhs.entity.dto.drop.StageDropDetailDTO;
import com.lhs.entity.po.material.RecycleDrop;
import com.lhs.mapper.material.RecycleDropMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
 *   <li>产出明细以 JSON 字符串整体存入 recycle_drop.drops（MySQL json 列），不做拆行存储；</li>
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
     * 单次上报的产出条目数上限。
     * <p>
     * 回收站一次结算的物品数量有限，超过该值视为异常报文，防止恶意请求写入超大 JSON。
     */
    private static final int MAX_DROP_ITEMS = 100;

    /**
     * 单个产出条目的数量上限。
     * <p>
     * 防止数量字段被填入极大值导致后续统计计算溢出或出现明显失真的数据。
     */
    private static final int MAX_ITEM_QUANTITY = 1_000_000;

    /** 回收站产出数据访问层，使用 MyBatis-Plus 自带 insert */
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
     *   <li>组装 RecycleDrop 实体，明细序列化为 JSON 后落库。</li>
     * </ol>
     * 参数或鉴权信息不合法时静默丢弃，不抛异常；同一 uid 在 5 秒内重复上报只保留第一次。
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

        // 组装落库实体：drops 以 JSON 字符串写入 json 列，由 MySQL 负责格式校验
        Date now = new Date();
        RecycleDrop recycleDrop = new RecycleDrop();
        recycleDrop.setId(idGenerator.nextId());
        recycleDrop.setDrops(JsonMapper.toJSONString(dto.getDrops()));
        recycleDrop.setServer(dto.getServer());
        recycleDrop.setSource(dto.getSource());
        recycleDrop.setUid(uid);
        recycleDrop.setVersion(dto.getVersion());
        recycleDrop.setCreateTime(now);

        recycleDropMapper.insert(recycleDrop);
    }

    /**
     * 校验产出明细列表是否合法。
     * <p>
     * 校验规则：
     * <ul>
     *   <li>列表不能为 null 或空，且条目数不超过 {@link #MAX_DROP_ITEMS}；</li>
     *   <li>每个条目不能为 null；</li>
     *   <li>itemId 非空白且长度不超过 50（与数据库列宽一致）；</li>
     *   <li>quantity 非 null、大于 0 且不超过 {@link #MAX_ITEM_QUANTITY}。</li>
     * </ul>
     *
     * @param drops 待校验的产出明细
     * @return 全部校验通过返回 true，否则返回 false
     */
    private boolean validDrops(List<StageDropDetailDTO> drops) {
        if (drops == null || drops.isEmpty() || drops.size() > MAX_DROP_ITEMS) {
            return false;
        }
        for (StageDropDetailDTO drop : drops) {
            if (drop == null || !hasText(drop.getItemId()) || drop.getItemId().length() > 50
                    || drop.getQuantity() == null || drop.getQuantity() <= 0
                    || drop.getQuantity() > MAX_ITEM_QUANTITY) {
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
