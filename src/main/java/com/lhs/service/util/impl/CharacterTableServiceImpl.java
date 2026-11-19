package com.lhs.service.util.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.entity.dto.survey.CharacterTableOperatorDTO;
import com.lhs.service.util.CharacterTableService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class CharacterTableServiceImpl implements CharacterTableService {

    /** 角色表文件名 */
    private static final String CHARACTER_TABLE_FILE_NAME = "character_table_simple.v2.json";

    /** 角色表反序列化类型：charId -> 精简干员数据 */
    private static final TypeReference<Map<String, CharacterTableOperatorDTO>> TABLE_TYPE =
            new TypeReference<>() {
            };

    /** 缓存空结果时使用的占位 Map：RedisTemplate 不接受 null 值，避免把 null 写入缓存 */
    private static final Map<String, CharacterTableOperatorDTO> EMPTY_TABLE = Collections.emptyMap();

    /**
     * 获取干员角色表（读取文件、解析 JSON 的过程整体由 @RedisCacheable 按默认有效期缓存到 Redis）
     * <p>
     * 兜底策略：文件不存在、内容为空或 JSON 解析失败时，一律返回空 Map 并记录日志，
     * 保证下游按"干员不在角色表"处理，不会因缓存加载异常导致接口报错。
     *
     * @return 干员角色表，key 为 charId，value 为精简后的干员数据；异常时返回空 Map
     */
    @Override
    @RedisCacheable(key = RedisKeyUtil.CHARACTER_TABLE_KEY)
    public Map<String, CharacterTableOperatorDTO> getCharacterTable() {
        try {
            String jsonText = FileUtil.read(ConfigUtil.DataFilePath + CHARACTER_TABLE_FILE_NAME);
            if (jsonText == null || jsonText.isBlank()) {
                Logger.error("干员角色表文件内容为空，路径：" + ConfigUtil.DataFilePath + CHARACTER_TABLE_FILE_NAME);
                return EMPTY_TABLE;
            }

            Map<String, CharacterTableOperatorDTO> resultMap = JsonMapper.parseObject(jsonText, TABLE_TYPE);
            if (resultMap == null || resultMap.isEmpty()) {
                Logger.error("干员角色表 JSON 解析失败或内容为空，路径：" + ConfigUtil.DataFilePath + CHARACTER_TABLE_FILE_NAME);
                return EMPTY_TABLE;
            }
            return resultMap;
        } catch (Exception e) {
            // 任何读取/解析异常都不向上抛，避免缓存加载失败直接打断干员数据接口
            Logger.error("读取干员角色表失败：" + e.getMessage());
            return EMPTY_TABLE;
        }
    }
}
