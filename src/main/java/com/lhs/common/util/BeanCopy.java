package com.lhs.common.util;

import com.lhs.entity.po.stage.StageResult;

import java.lang.reflect.Field;

public class BeanCopy {

    public static  <T> void copy(T source, T target) {
        System.out.println(source.getClass().getSimpleName());
        System.out.println(target.getClass().getSimpleName());

        String sourceName = source.getClass().getSimpleName();
        String targetName = target.getClass().getSimpleName();
        String sourceNamePascalCase = camelCase(sourceName);
        String targetNamePascalCase = camelCase(targetName);

        String result = "public void copy("+sourceName+" "+ sourceNamePascalCase+"){\n";

        Field[] fields = target.getClass().getDeclaredFields();
        for(Field field:fields){
            result += "  this."+field.getName() + " = " +
                    sourceNamePascalCase + ".get" + pascalCase(field.getName()) + "();\n";
        }

        result += "}";

        System.out.println(result);

    }

    public static String camelCase(String source){
        String[] words = source.split("(?=[A-Z])");
        String target = words[0].toLowerCase();
        for (int i = 1; i < words.length; i++) {
            target += words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        }
        return target;
    }

    public static String pascalCase(String source){
        String[] words = source.split("(?=[A-Z])");
        String target = "";
        for (int i = 0; i < words.length; i++) {
            target += words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        }
        return target;
    }

    public static String underline(String source){
        String[] words = source.split("(?=[A-Z])");
        String target = words[0].toLowerCase();
        for (int i = 1; i < words.length; i++) {
            target = target + "_" +words[i].toLowerCase();
        }

        return target;
    }
}
