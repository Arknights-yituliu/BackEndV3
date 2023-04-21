package com.lhs.common.config;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileConfig implements InitializingBean {

    @Value("${filePath.penguin}")
    private String penguin;  //    企鹅物流数据文件位置
    @Value("${filePath.frontEnd}")
    private String frontEnd;  //    前端数据文件位置
    @Value("${filePath.item}")
    private String item;  //    材料相关数据文件位置
    @Value("${filePath.backup}")
    private String backup;  //    备份文件路径
    @Value("${filePath.schedule}")
    private String schedule;


    public static String Penguin;
    public static String Item;
    public static String FrontEnd;
    public static String Backup;
    public static String Schedule;


    @Override
    public void afterPropertiesSet() throws Exception {
        Penguin = penguin;
        Item = item;
        FrontEnd = frontEnd;
        Backup = backup;
        Schedule = schedule;


    }
}
