package com.lhs.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.Logger;
import com.lhs.entity.po.admin.ImageInfo;
import com.lhs.mapper.admin.ImageInfoMapper;
import com.lhs.service.admin.ImageInfoService;
import com.lhs.service.util.TencentCloudService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ImageInfoServiceImpl implements ImageInfoService {

    /** 单张图片大小上限 */
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    /** image_name 同时是礼包等业务数据的关联键，长度需受限 */
    private static final int MAX_IMAGE_NAME_LEN = 64;

    /**
     * 允许写入的对象前缀：仅 image/ 及其子目录
     * <p>
     * 字符集受限，天然排除 ".."、"//"、协议头等写法，避免把对象写到前端发布目录
     */
    private static final Pattern PATH_PATTERN = Pattern.compile("^image/(?:[a-z0-9_-]+/)*$");

    private final ImageInfoMapper imageInfoMapper;
    private final TencentCloudService tencentCloudService;
    private final IdGenerator idGenerator;

    public ImageInfoServiceImpl(ImageInfoMapper imageInfoMapper, TencentCloudService tencentCloudService) {
        this.imageInfoMapper = imageInfoMapper;
        this.tencentCloudService = tencentCloudService;
        idGenerator = new IdGenerator(1L);
    }

    @Override
    public void saveImage(MultipartFile multipartFile, String path, String imageName) {
        // 扩展名由文件头推导，路径由服务端拼接，二者都不使用客户端提供的任何字段
        String ext = validateImage(multipartFile);
        String name = validateImageName(imageName, null);
        String bucketPath = normalizePath(path) + idGenerator.nextId() + "." + ext;

        saveRecord(name, bucketPath);
        tencentCloudService.uploadCOS(multipartFile, bucketPath, FileUtil.imageContentType(ext));
    }

    @Override
    public List<ImageInfo> listImageInfo(String imageType) {
        return imageInfoMapper.selectList(null);
    }

    @Override
    public ImageInfo getImageInfo(String imageName) {
        return imageInfoMapper.selectById(imageName);
    }

    @Override
    public String saveImageFiles(List<MultipartFile> files, String path) {
        if (files == null || files.isEmpty()) {
            throw new ServiceException(ResultCode.FILE_IS_NULL);
        }
        String prefix = normalizePath(path);

        // 先全量校验再上传，避免出现"前几张成功、中间一张非法"的半成品状态
        List<String> exts = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            exts.add(validateImage(file));
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String ext = exts.get(i);
            String bucketPath = prefix + idGenerator.nextId() + "." + ext;

            // 批量上传沿用旧语义：image_name 取自文件名且不含扩展名
            String imageName = stripLastExtension(validateImageName(file.getOriginalFilename(), "默认图片"));
            saveRecord(imageName, bucketPath);
            tencentCloudService.uploadCOS(file, bucketPath, FileUtil.imageContentType(ext));
        }
        return "文件上传成功";
    }

    /**
     * 去掉最后一个 "." 及其后缀
     * <p>
     * 与旧实现的 {@code split("\\.")[0]} 相比，只截断最后一个点，
     * 使 {@code v1.2礼包.png} 这类名称不会被误截成 {@code v1}
     *
     * @param name 文件名
     * @return 不含扩展名的名称
     */
    private String stripLastExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            return name.substring(0, dot);
        }
        return name;
    }

    /**
     * 校验图片并返回服务端推导的扩展名
     *
     * @param file 上传文件
     * @return jpg/png/webp/gif
     */
    private String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ResultCode.FILE_IS_NULL);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ServiceException(ResultCode.FILE_SIZE_LARGE);
        }
        String ext = FileUtil.detectImageExt(file);
        if (ext == null) {
            // 文件头不是受支持的图片格式（含 svg/html 等伪装成图片的内容）一律拒绝
            throw new ServiceException(ResultCode.FILE_TYPE_INVALID);
        }
        return ext;
    }

    /**
     * 校验并清洗图片名称
     * <p>
     * image_name 与业务名称（如礼包 officialName）关联，因此只校验不改写：
     * 仅去掉目录部分与控制字符，不主动截断、不改扩展名，避免破坏既有图片关联
     *
     * @param raw      原始名称
     * @param fallback 名称为空时使用的兜底值，为 null 时视为非法
     * @return 可用于入库的名称
     */
    private String validateImageName(String raw, String fallback) {
        String name = raw == null ? "" : raw.trim();
        // 客户端可能带路径（部分浏览器会提交完整路径），只取文件名部分
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("\\p{Cntrl}", "").trim();

        if (name.isEmpty()) {
            if (fallback == null) {
                throw new ServiceException(ResultCode.PARAM_IS_BLANK);
            }
            return fallback;
        }
        if (name.length() > MAX_IMAGE_NAME_LEN) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        return name;
    }

    /**
     * 校验对象前缀，为空时默认 image/
     *
     * @param path 请求参数中的路径
     * @return 以 "/" 结尾的合法前缀
     */
    private String normalizePath(String path) {
        String prefix = path == null ? "" : path.trim();
        if (prefix.isEmpty()) {
            return "image/";
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        // 只允许写入 image/ 及其子目录，其余前缀（前端发布目录等）一律拒绝
        if (!PATH_PATTERN.matcher(prefix).matches()) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        return prefix;
    }

    /**
     * 保存图片记录，已存在同名记录时更新其链接
     *
     * @param imageName  图片名称
     * @param bucketPath 对象键
     */
    private void saveRecord(String imageName, String bucketPath) {
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.setCreateTime(new Date());
        imageInfo.setImageName(imageName);
        imageInfo.setImageLink(bucketPath);

        LambdaQueryWrapper<ImageInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImageInfo::getImageName, imageName);
        ImageInfo exist = imageInfoMapper.selectOne(queryWrapper);

        if (exist == null) {
            imageInfoMapper.insert(imageInfo);
        } else {
            Logger.info("文件已存在：" + imageName);
            imageInfoMapper.updateById(imageInfo);
        }
    }
}
