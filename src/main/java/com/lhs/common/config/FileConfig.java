package com.lhs.common.config;


import com.lhs.common.util.FileUtil;
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
    @Value("${filePath.operatorBox}")
    private String operatorBox;
    @Value("${filePath.secret}")
    private String secret;


    public static String Penguin;
    public static String Item;
    public static String FrontEnd;
    public static String Backup;
    public static String Schedule;
    public static String OperatorBox;
    public static String Secret;


    @Override
    public void afterPropertiesSet() throws Exception {
        Penguin = penguin;
        Item = item;
        FrontEnd = frontEnd;
        Backup = backup;
        Schedule = schedule;
        OperatorBox = operatorBox;
        Secret = secret;
    }
}
