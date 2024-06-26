package com.lhs.service.admin.impl;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.po.admin.ImageInfo;
import com.lhs.mapper.admin.ImageInfoMapper;
import com.lhs.service.admin.ImageInfoService;
import com.lhs.service.util.COSService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ImageInfoServiceImpl implements ImageInfoService {

    private final ImageInfoMapper imageInfoMapper;
    private final COSService cosService;
    private final IdGenerator idGenerator;

    public ImageInfoServiceImpl(ImageInfoMapper imageInfoMapper,COSService cosService) {
        this.imageInfoMapper = imageInfoMapper;
        this.cosService = cosService;
        idGenerator = new IdGenerator(1L);
    }


    @Override
    public void saveImage(MultipartFile multipartFile,String path, String imageName) {
        if(imageName==null||path==null){
            throw new ServiceException(ResultCode.PARAM_NOT_COMPLETE);
        }
        String originalFilename = multipartFile.getOriginalFilename();
        String fileType = "jpg";
        if(originalFilename!=null&&!originalFilename.isEmpty()){
            fileType = originalFilename.split("\\.")[1];
        }
        String imageId = idGenerator.nextId()+"."+fileType;
        String bucketPath = path+imageId;

        ImageInfo imageInfo = new ImageInfo();
        imageInfo.setImageId(imageId);
        imageInfo.setCreateTime(System.currentTimeMillis());
        imageInfo.setImageName(imageName);
        imageInfo.setImageLink(bucketPath);
        imageInfoMapper.insert(imageInfo);

        cosService.uploadFile(multipartFile,bucketPath);

    }

    @Override
    public List<ImageInfo> listImageInfo(String imageType) {
        List<ImageInfo> imageInfos = imageInfoMapper.selectList(null);
        return imageInfos;
    }
}
