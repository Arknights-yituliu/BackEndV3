package com.lhs.service.dev.impl;

import com.lhs.mapper.dev.ImageInfoMapper;
import com.lhs.service.dev.ImageInfoService;
import com.lhs.service.util.COSService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;

@Service
public class ImageInfoServiceImpl implements ImageInfoService {

    private final ImageInfoMapper imageInfoMapper;
    private final COSService cosService;

    public ImageInfoServiceImpl(ImageInfoMapper imageInfoMapper,COSService cosService) {
        this.imageInfoMapper = imageInfoMapper;
        this.cosService = cosService;
    }


    @Override
    public void saveImage(MultipartFile multipartFile, HashMap<String, String> params) {



    }
}
