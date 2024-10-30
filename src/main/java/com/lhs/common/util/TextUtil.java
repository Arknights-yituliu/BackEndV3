package com.lhs.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TextUtil {

    public static List<String> textToArray(String text){
        return Arrays.stream(text.split(","))
                .collect(Collectors.toList());
    }
}
