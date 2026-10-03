package com.lhs.service.util;

import com.lhs.entity.dto.survey.CharacterTableOperatorDTO;

import java.util.Map;

/**
 * 干员角色表数据服务
 *
 * <p>负责读取并解析 character_table_simple.v2.json，解析结果由
 * {@link com.lhs.common.annotation.RedisCacheable} 缓存到 Redis，
 * 避免每次请求都读盘并重复解析 JSON。</p>
 */
public interface CharacterTableService {

    /**
     * 获取干员角色表（读取文件、解析 JSON 的过程整体由 @RedisCacheable 缓存）
     *
     * @return 干员角色表，key 为 charId，value 为精简后的干员数据；
     *         文件读取或解析失败时返回空 Map，不会抛错
     */
    Map<String, CharacterTableOperatorDTO> getCharacterTable();
}
