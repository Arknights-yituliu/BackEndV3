package com.lhs.service.user;

import com.lhs.entity.po.user.RiicSchedule;

import java.util.List;

/**
 * 基建排班表服务
 * <p>
 * 提供排班表的保存/覆盖、列表查询、指定查询；
 * 保存要求登录状态（uid 由调用方从 UserContext 获取），并限制每用户最多 10 条
 */
public interface RiicScheduleService {

    /**
     * 保存或覆盖排班表
     * <p>
     * id 为空时新增（校验每用户 10 条上限）；id 非空时覆盖（校验记录归属当前用户）
     *
     * @param uid          当前登录用户 uid
     * @param id           排班表 id（覆盖时必传）
     * @param scheduleJson 排班表 JSON 文本
     * @return 排班表 id
     */
    Long saveSchedule(Long uid, Long id, String scheduleJson);

    /**
     * 查询用户名下全部排班表（按更新时间倒序）
     *
     * @param uid 当前登录用户 uid
     * @return 排班表列表
     */
    List<RiicSchedule> listByUid(Long uid);

    /**
     * 查询指定 id 的排班表（不限本人，登录用户均可查看）
     *
     * @param id 排班表 id
     * @return 排班表
     */
    RiicSchedule getSchedule(Long id);
}
