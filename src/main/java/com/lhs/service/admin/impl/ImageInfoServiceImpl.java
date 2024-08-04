package com.lhs.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.Logger;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.po.admin.ImageInfo;
import com.lhs.entity.po.admin.LogInfo;
import com.lhs.mapper.admin.ImageInfoMapper;
import com.lhs.service.admin.ImageInfoService;
import com.lhs.service.util.COSService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ImageInfoServiceImpl implements ImageInfoService {

    private final ImageInfoMapper imageInfoMapper;
    private final COSService cosService;
    private final IdGenerator idGenerator;

    public ImageInfoServiceImpl(ImageInfoMapper imageInfoMapper, COSService cosService) {
        this.imageInfoMapper = imageInfoMapper;
        this.cosService = cosService;
        idGenerator = new IdGenerator(1L);
    }


    @Override
    public void saveImage(MultipartFile multipartFile, String path, String imageName) {
        if (imageName == null || path == null) {
            throw new ServiceException(ResultCode.PARAM_NOT_COMPLETE);
        }
        String originalFilename = multipartFile.getOriginalFilename();
        String fileType = "jpg";
        if (originalFilename != null && !originalFilename.isEmpty()) {
            fileType = originalFilename.split("\\.")[1];
        }
        String imageId = idGenerator.nextId() + "." + fileType;
        String bucketPath = path + imageId;

        ImageInfo imageInfo = new ImageInfo();
        imageInfo.setImageId(imageId);
        imageInfo.setCreateTime(System.currentTimeMillis());
        imageInfo.setImageName(imageName);
        imageInfo.setImageLink(bucketPath);
        LambdaQueryWrapper<ImageInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImageInfo::getImageName, imageName);
        ImageInfo exist = imageInfoMapper.selectOne(queryWrapper);
        if (exist == null) {
            imageInfoMapper.insert(imageInfo);
        } else {
            imageInfoMapper.updateById(imageInfo);
        }


        cosService.uploadFile(multipartFile, bucketPath);

    }

    @Override
    public List<ImageInfo> listImageInfo(String imageType) {
        List<ImageInfo> imageInfos = imageInfoMapper.selectList(null);
        return imageInfos;
    }

    @Override
    public String saveImageFiles(List<MultipartFile> files, String path) {
        for (MultipartFile multipartFile : files) {
            String originalFilename = multipartFile.getOriginalFilename();
            String fileType = "jpg";
            String imageName = "默认图片";
            if (originalFilename != null && !originalFilename.isEmpty()) {

                String[] split = originalFilename.split("\\.");
                imageName = split[0];
                fileType = split[1];

            }
            String imageId = idGenerator.nextId() + "." + fileType;
            String bucketPath = path + imageId;
            ImageInfo imageInfo = new ImageInfo();
            imageInfo.setImageId(imageId);
            imageInfo.setCreateTime(System.currentTimeMillis());
            imageInfo.setImageName(imageName);
            imageInfo.setImageLink(bucketPath);
            LambdaQueryWrapper<ImageInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ImageInfo::getImageName, imageName);
            ImageInfo exist = imageInfoMapper.selectOne(queryWrapper);

            if (exist == null) {
                imageInfoMapper.insert(imageInfo);
            } else {
                Logger.info("文件已存在");
                imageInfoMapper.updateById(imageInfo);
            }
            cosService.uploadFile(multipartFile, bucketPath);
        }
        return "文件上传成功";
    }
}
