package com.lhs.service.util;

import com.qcloud.cos.COSClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface TencentCloudService {

   void uploadCOS(File file, String bucketPath);

    void uploadCOS(String text, String bucketPath);

   void uploadCOS(MultipartFile multipartFile, String bucketPath);

    void backupCOS(String text, String bucketPath);

    void backupCOS(File file, String bucketPath);

    COSClient createCOSClient();

    void frontEndDeployment(String projectPath,String regionStr,String bucketName);

    void CDNRefreshDirectory(String domain);

    /**
     * 刷新 CDN 指定 URL 的缓存（对应腾讯云 PurgeUrlsCache 接口）
     *
     * @param urls 需要刷新的 URL 列表，单次最多 10000 个
     */
    void CDNRefreshUrls(List<String> urls);
}
