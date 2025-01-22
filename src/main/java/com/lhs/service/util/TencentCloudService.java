package com.lhs.service.util;

import com.qcloud.cos.COSClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface TencentCloudService {

   void uploadFileToCOS(File file, String bucketPath);

    void uploadJsonToCOS(String text, String bucketPath);

   void uploadFileToCOS(MultipartFile multipartFile, String bucketPath);

    COSClient createCOSClient();

    void frontEndDeployment(String regionStr,String bucketName);

    void CDNRefreshDirectory(String domain);
}
