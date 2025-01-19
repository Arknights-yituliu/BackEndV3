package com.lhs.service.admin;

import com.lhs.entity.po.admin.ImageInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageInfoService {

    void saveImage(MultipartFile multipartFile, String path, String imageName);

    void saveImage(MultipartFile multipartFile, String path);

    List<ImageInfo> listImageInfo(String imageType);

    ImageInfo getImageInfo(String imageName);

    String saveImageFiles(List<MultipartFile> files, String path);
}

