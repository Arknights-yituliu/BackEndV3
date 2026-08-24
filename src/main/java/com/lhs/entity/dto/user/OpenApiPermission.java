package com.lhs.entity.dto.user;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 权限枚举，每个权限对应唯一的数字 code
 */
public enum OpenApiPermission {

    operatorDataReadAccess(10001, "operatorDataReadAccess", "干员数据读取"),
    operatorDataWriteAccess(10002, "operatorDataWriteAccess", "干员数据写入"),
    ;

    private final int code;
    private final String key;
    private final String desc;

    OpenApiPermission(int code, String key, String desc) {
        this.code = code;
        this.key = key;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 key 查找对应的 code，未找到返回 null
     */
    public static Integer codeByKey(String key) {
        for (OpenApiPermission perm : values()) {
            if (perm.key.equals(key)) {
                return perm.code;
            }
        }
        return null;
    }

    /**
     * 返回所有权限列表，供前端展示
     */
    public static List<Map<String, Object>> listAll() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OpenApiPermission perm : values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key", perm.key);
            map.put("code", perm.code);
            map.put("desc", perm.desc);
            list.add(map);
        }
        return list;
    }
}
