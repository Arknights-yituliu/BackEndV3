package com.lhs.common.config;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationConfig implements InitializingBean {

    @Value("${resourcesPath.penguin}")
    private String penguin;  //    企鹅物流数据文件路径
    @Value("${resourcesPath.frontEnd}")
    private String frontEnd;  //    前端数据文件路径
    @Value("${resourcesPath.item}")
    private String item;  //    材料相关数据文件路径
    @Value("${resourcesPath.backup}")
    private String backup;  //    备份文件路径
    @Value("${resourcesPath.schedule}")
    private String schedule;  //    排班文件路径


    @Value("${encryption.secret}")
    private String secret;
    @Value("${encryption.signKey}")
    private String signKey;
    @Value("${encryption.machineId}")
    private String machineId;
    @Value("${penguin.auto}")
    private String penguinAuto;
    @Value("${penguin.global}")
    private String penguinGlobal;

    @Value("${aliyun.accessKeyID}")
    private static String ossAccessKeyId;
    @Value("${aliyun.accessKeySecret}")
    private static String ossAccessKeySecret;
    @Value("${aliyun.bakBucketName}")
    private static String  bakBucketName;


    public static String Penguin;
    public static String Item;
    public static String FrontEnd;
    public static String Backup;
    public static String Schedule;


    public static String Secret;
    public static String SignKey;
    public static String MachineId;


    public static String PenguinAuto;
    public static String PenguinGlobal;

    public static String OSSAccessKeyId;
    public static String OSSAccessKeySecret;
    public static String BakBucketName;


    @Override
    public void afterPropertiesSet() {
        Penguin = penguin;
        Item = item;
        FrontEnd = frontEnd;
        Backup = backup;
        Schedule = schedule;
        Secret = secret;
        SignKey = signKey;
        MachineId = machineId;
        PenguinAuto = penguinAuto;
        PenguinGlobal = penguinGlobal;
        OSSAccessKeyId = ossAccessKeyId;
        OSSAccessKeySecret = ossAccessKeySecret;
        BakBucketName = bakBucketName;
    }
}
