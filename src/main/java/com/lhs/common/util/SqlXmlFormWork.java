package com.lhs.common.util;



import com.google.common.base.CaseFormat;

import java.lang.reflect.Field;

public class SqlXmlFormWork {



    static final String  XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"\n" +
             "\"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n" +
            "<mapper namespace=\"com.lhs.mapper.>\n\n";

    static final String XML_FOOTER = "\n</mapper>";
    public static <T> void insertBatch(T entity) {

        String packageAndClassName = entity.getClass().getTypeName();
        String[] split = packageAndClassName.split("\\.");
        String className = split[split.length - 1];

        Field[] fields = entity.getClass().getDeclaredFields();
        String tableName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className);

        StringBuilder insertBatchSql = new StringBuilder( "<insert id=\"insertBatch\">\n" +
                 "INSERT INTO " + tableName + " \n");

        int count = 1;
        for (Field field : fields) {
            String name = field.getName();
            String lowerUnderscore = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);

            //数据库列名
            String fieldList = "`" + lowerUnderscore + "`";
            //对象属性名
            String attribute = "#{item." + name + "}";

            if (count == 1) {
                //插入语句第一个列名前面有 (
                insertBatchSql.append("(").append(fieldList);
            } else {
                //插入语句拼接
                insertBatchSql.append(fieldList);
            }

            //更新语句拼接  数据库列名 = 对象属性名

            //列名间拼接 ,
            if (count < fields.length) {
                insertBatchSql.append(",");
            }

            count++;
        }

        insertBatchSql.append(")\n").append("VALUES\n").append("<foreach collection=\"list\" item=\"item\" separator=\",\">\n");

        count = 1;

        for (Field field : fields) {
            String name = field.getName();
            String attribute = "#{item." + name + "}";

            if (count == 1) {
                //插入语句第一个列名前面有
                insertBatchSql.append("(").append(attribute);
            } else {
                //插入语句拼接
                insertBatchSql.append(attribute);
            }

            if (count < fields.length) {
                insertBatchSql.append(",");
            }
            count++;
        }

        insertBatchSql.append(")\n").append("</foreach>").append("\n").append("</insert>\n\n");



        System.out.println(insertBatchSql);


    }



    public static <T> void update(T entity) {
        //带包名的类名
        String packageAndClassName = entity.getClass().getTypeName();
        String[] split = packageAndClassName.split("\\.");
        //类名
        String className = split[split.length - 1];

        Field[] fields = entity.getClass().getDeclaredFields();
        //表名  类名的_写法
        String tableName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className);



        StringBuilder updateSql = new StringBuilder("<update id=\"updateById\">\n" +
                 "UPDATE " + tableName + " \n" +
                 "SET\n");

        int count = 1;
        for (Field field : fields) {
            String name = field.getName();
            String lowerUnderscore = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);

            //数据库列名
            String fieldList = "`" + lowerUnderscore + "`";
            //对象属性名
            String attribute = "#{item." + name + "}";

            updateSql.append(fieldList).append(" = ").append(attribute);


            if (count < fields.length) {
                updateSql.append(",\n");
            }
            count++;
        }

        updateSql.append("\n").append("WHERE\n").append("id = #{id}\n").append("</update>\n\n");


        System.out.println(updateSql);


    }
}
