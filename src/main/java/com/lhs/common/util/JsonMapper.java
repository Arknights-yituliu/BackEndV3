package com.lhs.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class JsonMapper {


    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 时间日期格式
    private static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";


//    //以静态代码块初始化
//    static {
//        //对象的所有字段全部列入序列化
//        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
//        //取消默认转换timestamps形式
//        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//        //忽略空Bean转json的错误
//        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//        //所有的日期格式都统一为以下的格式，即yyyy-MM-dd HH:mm:ss
//        objectMapper.setDateFormat(new SimpleDateFormat(STANDARD_FORMAT));
//        //忽略 在json字符串中存在，但在java对象中不存在对应属性的情况。防止错误
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//    }

    /**===========================以下是从JSON中获取对象====================================*/
    public static <T> T parseObject(String jsonString, Class<T> object) {
        T t = null;
        try {
            t = objectMapper.readValue(jsonString, object);
        } catch (JsonProcessingException e) {
            Log.error("JsonString转为自定义对象失败：{}"+ e.getMessage());
        }
        return t;
    }

    public static <T> T parseObject(File file, Class<T> object) {
        T t = null;
        try {
            t = objectMapper.readValue(file, object);
        } catch (IOException e) {
            Log.error("从文件中读取json字符串转为自定义对象失败：{}"+ e.getMessage());
        }
        return t;
    }

    //将json数组字符串转为指定对象List列表或者Map集合
    public static <T> T parseJSONArray(String jsonArray, TypeReference<T> reference) {
        T t = null;
        try {
            t = objectMapper.readValue(jsonArray, reference);
        } catch (JsonProcessingException e) {
            Log.error("JSONArray转为List列表或者Map集合失败：{}"+e.getMessage());
        }
        return t;
    }

    /**=================================以下是将对象转为JSON=====================================*/
    public static String toJSONString(Object object) {
        String jsonString = null;
        try {
            jsonString = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            Log.error("Object转JSONString失败：{}"+ e.getMessage());
        }
        return jsonString;
    }

    public static byte[] toByteArray(Object object) {
        byte[] bytes = null;
        try {
            bytes = objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            Log.error("Object转ByteArray失败：{}"+ e.getMessage());
        }
        return bytes;
    }

    public static void objectToFile(File file, Object object) {
        try {
            objectMapper.writeValue(file, object);
        } catch (JsonProcessingException e) {
            Log.error("Object写入文件失败：{}"+ e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**=============================以下是与JsonNode相关的=======================================*/
    public static JsonNode parseJSONObject(String jsonString) {
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            Log.error("JSONString转为JsonNode失败：{}"+ e.getMessage());
        }
        return jsonNode;
    }

    public static JsonNode parseJSONObject(Object object) {
        return objectMapper.valueToTree(object);
    }

    public static String toJSONString(JsonNode jsonNode) {
        String jsonString = null;
        try {
            jsonString = objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            Log.error("JsonNode转JSONString失败：{}"+ e.getMessage());
        }
        return jsonString;
    }

    //JsonNode是一个抽象类，不能实例化，创建JSON树形模型，得用JsonNode的子类ObjectNode，用法和JSONObject大同小异
    public static ObjectNode newJSONObject() {
        return objectMapper.createObjectNode();
    }

    //创建JSON数组对象，就像JSONArray一样用
    public static ArrayNode newJSONArray() {
        return objectMapper.createArrayNode();
    }


}
