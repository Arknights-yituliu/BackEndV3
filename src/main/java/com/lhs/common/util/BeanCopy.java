package com.lhs.common.util;

import com.google.common.base.CaseFormat;

import java.lang.reflect.Field;
import java.util.HashMap;

public class BeanCopy {

    public static  <T> void copy(T source, T target) {
        String sourceName = source.getClass().getSimpleName();
        String targetName = target.getClass().getSimpleName();
        String sourceNameLowerCamel = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, sourceName);
        String targetNameLowerCamel = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, targetName);
        System.out.println(sourceNameLowerCamel);
        System.out.println(targetNameLowerCamel);

        String result = "public void copyBy"+sourceName+"("+sourceName+" "+ sourceNameLowerCamel+"){\n";

        Field[] targetFields = target.getClass().getDeclaredFields();
        Field[] sourceFields = source.getClass().getDeclaredFields();

        HashMap<String, String>  propertyMap = new HashMap<>();
        for(Field field:sourceFields){
            propertyMap.put(field.getName(), CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, field.getName()));
            System.out.println(field.getName()+"————"+ CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, field.getName()));
        }

        for(Field field:targetFields){
            if(propertyMap.get(field.getName())==null) continue;
            result += "  this."+field.getName() + " = " +
                    sourceNameLowerCamel + ".get" + propertyMap.get(field.getName()) + "();\n";
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
