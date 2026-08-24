package com.lhs.common.config;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigUtil implements InitializingBean {

    @Value("${resourcesPath.penguin}")
    private String penguin;  //
    @Value("${resourcesPath.config}")
    private String configFilePath;  //
    @Value("${resourcesPath.data}")
    private String dataFilePath;  //
    @Value("${resourcesPath.backup}")
    private String backupFilePath;

    @Value("${encryption.secret}")
    private String secret;
    @Value("${encryption.signKey}")
    private String signKey;



    @Value("${penguin.auto}")
    private String penguinAuto;


    @Value("${skland.playerInfoAPI}")
    private String sklandPlayerInfoAPI;
    @Value("${skland.playerBindingAPI}")
    private String sklandPlayerBindingAPI;


    public static String Penguin;
    public static String ConfigFilePath;

    public static String DataFilePath;

    public static String BackupFilePath;

    public static String Secret;
    public static String SignKey;

    public static String PenguinAuto;


    public static String SKLandPlayerBindingAPI;
    public static String SKLandPlayerInfoAPI;



    @Override
    public void afterPropertiesSet() {
        Penguin = penguin;
        ConfigFilePath = configFilePath;
        DataFilePath = dataFilePath;
        BackupFilePath = backupFilePath;

        Secret = secret;
        SignKey = signKey;

        PenguinAuto = penguinAuto;


        SKLandPlayerBindingAPI = sklandPlayerBindingAPI;
        SKLandPlayerInfoAPI = sklandPlayerInfoAPI;



    }
}
