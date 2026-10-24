package com.lhs.service.util;

import com.qcloud.cos.COSClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface TencentCloudService {

   void uploadCOS(File file, String bucketPath);

    void uploadCOS(String text, String bucketPath);

   void uploadCOS(MultipartFile multipartFile, String bucketPath);

    /**
     * 上传文件并显式指定 Content-Type
     * <p>
     * 用于图片上传：对象元信息中的 Content-Type 由服务端按魔数推导后写入，
     * 不依赖存储侧按扩展名猜测，避免对象被浏览器当作 HTML/SVG 渲染
     *
     * @param multipartFile 上传文件
     * @param bucketPath    对象键
     * @param contentType   MIME 类型，为空时交由存储侧推断
     */
    void uploadCOS(MultipartFile multipartFile, String bucketPath, String contentType);

    void backupCOS(String text, String bucketPath);

    void backupCOS(File file, String bucketPath);

    COSClient createCOSClient();

    void frontEndDeployment(String projectPath,String regionStr,String bucketName);

    void CDNRefreshDirectory(String domain);

    /**
     * 刷新 CDN 指定 URL 的缓存（对应腾讯云 PurgeUrlsCache 接口）
     *
     * @param urls 需要刷新的 URL 列表，单次最多 10000 个
     */
    void CDNRefreshUrls(List<String> urls);
}
