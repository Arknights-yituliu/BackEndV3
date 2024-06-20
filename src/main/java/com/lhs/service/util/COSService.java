package com.lhs.service.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface COSService {

   void uploadFile(File file, String bucketPath);

   void uploadFile(MultipartFile multipartFile, String bucketPath);
}
