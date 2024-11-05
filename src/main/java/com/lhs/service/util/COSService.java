package com.lhs.service.util;

import com.qcloud.cos.COSClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface COSService {

   void uploadFile(File file, String bucketPath);

    void uploadJson(String text, String bucketPath);

   void uploadFile(MultipartFile multipartFile, String bucketPath);

    COSClient createCOSClient();
}
