package com.lhs.service.util;

import com.qcloud.cos.COSClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface TencentCloudService {

   void uploadCOS(File file, String bucketPath);

    void uploadCOS(String text, String bucketPath);

   void uploadCOS(MultipartFile multipartFile, String bucketPath);

    void backupCOS(String text, String bucketPath);

    void backupCOS(File file, String bucketPath);

    COSClient createCOSClient();

    void frontEndDeployment(String projectPath,String regionStr,String bucketName);

    void CDNRefreshDirectory(String domain);
}
