package com.lhs.service.util.impl;

import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.LogUtils;
import com.lhs.common.util.ResultCode;
import com.lhs.service.util.COSService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@Service
public class COSServiceImpl implements COSService {
    @Override
    public void uploadFile(File file, String bucketPath) {
        uploadCOS(file, bucketPath);
    }

    @Override
    public void uploadJson(String text, String bucketPath) {
        try {
            File file = convertJsonStringToFile(text);
            uploadFile(file,bucketPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    private File convertJsonStringToFile(String jsonString) throws IOException {
        File tempFile = File.createTempFile("json", ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(jsonString.getBytes());
        }
        return tempFile;
    }

    @Override
    public void uploadFile(MultipartFile multipartFile, String bucketPath) {
        uploadCOS(multipartFile, bucketPath);
    }

    private void uploadCOS(File file, String bucketPath){
        String secretId = ConfigUtil.CosSecretId;
        String secretKey = ConfigUtil.CosSecretKey;
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域的简称请参见 https://cloud.tencent.com/document/product/436/6224
        // clientConfig 中包含了设置 region, https(默认 http), 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
        Region region = new Region("ap-nanjing");
        ClientConfig clientConfig = new ClientConfig(region);
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        COSClient cosClient = new COSClient(cred, clientConfig);

        // 指定文件将要存放的存储桶
        String bucketName = "yituliu-static-1307648010";
        // 指定文件上传到 COS 上的路径，即对象键。例如对象键为 folder/picture.jpg，则表示将文件 picture.jpg 上传到 folder 路径下

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, bucketPath, file);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        String requestId = putObjectResult.getRequestId();
        cosClient.shutdown();
    }

    private void uploadCOS(MultipartFile file, String bucketPath)  {
        COSClient cosClient = createCOSClient();
        // 指定文件将要存放的存储桶
        String bucketName = "yituliu-static-1307648010";
        // 指定文件上传到 COS 上的路径，即对象键。例如对象键为 folder/picture.jpg，则表示将文件 picture.jpg 上传到 folder 路径下
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
        } catch (IOException e) {
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, bucketPath, inputStream,objectMetadata);
        cosClient.putObject(putObjectRequest);
        cosClient.shutdown();
    }

    @Override
    public COSClient createCOSClient(){
        String secretId = ConfigUtil.CosSecretId;
        String secretKey = ConfigUtil.CosSecretKey;
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region region = new Region("ap-nanjing");
        ClientConfig clientConfig = new ClientConfig(region);
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        clientConfig.setSocketTimeout(30*1000);
        clientConfig.setConnectionTimeout(30*1000);


        // 3 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }

    private File multipartFileToFile(MultipartFile multipartFile, String fileName) {
        String filePath = "/image/store/" + fileName;
        File file = new File(filePath);

        try {
            multipartFile.transferTo(file);
        } catch (IOException exception) {
            LogUtils.error(exception.getMessage());
        }

        return file;
    }
}
