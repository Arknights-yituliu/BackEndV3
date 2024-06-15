package com.lhs.service.util.impl;

import com.lhs.common.config.ConfigUtil;
import com.lhs.service.util.COSService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class COSServiceImpl implements COSService {
    @Override
    public void uploadFile(File file,String bucketPath) {

        String secretId = ConfigUtil.CosSecretId;
        String secretKey = ConfigUtil.CosSecretKey;
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域的简称请参见 https://cloud.tencent.com/document/product/436/6224
        // clientConfig 中包含了设置 region, https(默认 http), 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
        Region region = new Region("ap-beijing");
        ClientConfig clientConfig = new ClientConfig(region);
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        COSClient cosClient = new COSClient(cred, clientConfig);


        // 指定文件将要存放的存储桶
        String bucketName = "yituliu-1307648010";
        // 指定文件上传到 COS 上的路径，即对象键。例如对象键为 folder/picture.jpg，则表示将文件 picture.jpg 上传到 folder 路径下

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, bucketPath, file);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);


    }
}
