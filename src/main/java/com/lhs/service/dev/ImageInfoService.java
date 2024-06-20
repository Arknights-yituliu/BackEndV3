package com.lhs.service.dev;

import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;

public interface ImageInfoService {

    void saveImage(MultipartFile multipartFile,HashMap<String,String> params);


}
