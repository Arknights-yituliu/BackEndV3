package com.lhs.common.config;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigUtil implements InitializingBean {

    @Value("${resourcesPath.penguin}")
    private String penguin;  //    企鹅物流数据文件路径
    @Value("${resourcesPath.config}")
    private String config;  //    材料相关数据文件路径

    @Value("${resourcesPath.schedule}")
    private String schedule;  //    排班文件路径

    @Value("${encryption.secret}")
    private String secret;
    @Value("${encryption.signKey}")
    private String signKey;

    @Value("${resourcesPath.resources}")
    private String resources;

    @Value("${penguin.auto}")
    private String penguinAuto;
    @Value("${penguin.global}")
    private String penguinGlobal;

    @Value("${skland.playerInfoAPI}")
    private String sklandPlayerInfoAPI;
    @Value("${skland.playerBindingAPI}")
    private String sklandPlayerBindingAPI;

    @Value("${tencent.secretId}")
    private String cosSecretId;

    @Value("${tencent.secretKey}")
    private String cosSecretKey;

    public static String Penguin;
    public static String Config;

    public static String Schedule;

    public static String Secret;
    public static String SignKey;

    public static String PenguinAuto;
    public static String PenguinGlobal;

    public static String SKLandPlayerBindingAPI;
    public static String SKLandPlayerInfoAPI;

    public static String Resources;

    public static String CosSecretId;

    public static String CosSecretKey;

    @Override
    public void afterPropertiesSet() {
        Penguin = penguin;
        Config = config;
        Resources = resources;


        Schedule = schedule;
        Secret = secret;
        SignKey = signKey;

        PenguinAuto = penguinAuto;
        PenguinGlobal = penguinGlobal;

        SKLandPlayerBindingAPI = sklandPlayerBindingAPI;
        SKLandPlayerInfoAPI = sklandPlayerInfoAPI;

        CosSecretId = cosSecretId;
        CosSecretKey = cosSecretKey;

    }
}
