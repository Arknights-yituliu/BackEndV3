package com.lhs.common.util;

import com.google.common.base.CaseFormat;

import java.lang.reflect.Field;

public class SqlXmlFormWork {

    static String  indentation4 = "    ";
    static String indentation8 = "        ";
    static String indentation12 = "            ";



    public static <T> void insertBatch(T entity) {

        String packageAndClassName = entity.getClass().getTypeName();
        String[] split = packageAndClassName.split("\\.");
        String className = split[split.length - 1];

        Field[] fields = entity.getClass().getDeclaredFields();
        String tableName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className);

        String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"\n" +
                indentation8 + "\"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n" +
                "<mapper namespace=\"com.lhs.mapper." + className + "Mapper\">\n\n";

        StringBuilder insertBatchSql = new StringBuilder(indentation4 + "<insert id=\"insertBatch\">\n" +
                indentation8 + "INSERT INTO " + tableName + " \n");

        StringBuilder selectSql = new StringBuilder(indentation4 + "<select id=\"selectList\" resultType=\"" + packageAndClassName + "\">\n" +
                indentation8 + "SELECT\n");



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
                insertBatchSql.append(indentation12).append("(").append(fieldList);
                selectSql.append(indentation12).append(fieldList);
            } else {
                //查询语句拼接
                selectSql.append(fieldList);
                //插入语句拼接
                insertBatchSql.append(fieldList);
            }

            //更新语句拼接  数据库列名 = 对象属性名

            //列名间拼接 ,
            if (count < fields.length) {
                insertBatchSql.append(",");
                selectSql.append(",");
            }

            count++;
        }

        insertBatchSql.append(")\n")
                .append(indentation8).append("VALUES\n")
                .append(indentation8).append("<foreach collection=\"list\" item=\"item\" separator=\",\">\n");



        selectSql.append("\n")
                .append(indentation8).append("FROM tableName\n")
                .append(indentation8).append("WHERE\n")
                .append(indentation12).append("id = #{id}\n")
                .append(indentation4).append("</select>\n\n");

        count = 1;

        for (Field field : fields) {
            String name = field.getName();
            String attribute = "#{item." + name + "}";

            if (count == 1) {
                //插入语句第一个列名前面有
                insertBatchSql.append(indentation12).append("(").append(attribute);
            } else {
                //插入语句拼接
                insertBatchSql.append(attribute);
            }

            if (count < fields.length) {
                insertBatchSql.append(",");
            }else {
                insertBatchSql.append(")");
            }
            count++;
        }

        insertBatchSql.append("\n")
                .append(indentation8).append("</foreach>").append("\n")
                .append(indentation4).append("</insert>\n\n");


        String xmlFooter = "\n</mapper>";


        String xmlFile = xmlHeader + insertBatchSql + xmlFooter;
        System.out.println(xmlFile);

//        FileUtil.save("src/main/resources/mapper/", className + ".xml", xmlFile);
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

        String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"\n" +
                indentation8 + "\"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n" +
                "<mapper namespace=\"com.lhs.mapper." + className + "Mapper\">\n\n";

        StringBuilder updateSql = new StringBuilder(indentation4 + "<update id=\"updateById\">\n" +
                indentation8 + "UPDATE " + tableName + " \n" +
                indentation8 + "SET\n");

        int count = 1;
        for (Field field : fields) {
            String name = field.getName();
            String lowerUnderscore = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);

            //数据库列名
            String fieldList = "`" + lowerUnderscore + "`";
            //对象属性名
            String attribute = "#{item." + name + "}";

            updateSql.append(indentation12).append(fieldList).append(" = ").append(attribute);

            if (count < fields.length) {
                updateSql.append(",\n");
            }
        }

        updateSql.append("\n")
                .append(indentation8).append("WHERE\n")
                .append(indentation12).append("id = #{id}\n")
                .append(indentation4).append("</update>\n\n");

        String xmlFooter = "\n</mapper>";

        String xmlFile = xmlHeader +  updateSql +  xmlFooter;

        System.out.println(xmlFile);


    }
}
