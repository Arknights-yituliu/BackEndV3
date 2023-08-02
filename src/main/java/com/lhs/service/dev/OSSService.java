package com.lhs.service.dev;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.lhs.common.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class OSSService {

    @Value("${aliyun.accessKeyID}")
    private  String AccessKeyId;
    @Value("${aliyun.accessKeySecret}")
    private  String AccessKeySecret;
    @Value("${aliyun.bakBucketName}")
    private  String  BakBucketName;

    /**
     *
     * @param content 上传内容
     * @param objectName  资源路径
     */
    public  void upload(String content, String objectName) {
        String endpoint = "https://oss-cn-beijing.aliyuncs.com";
        String accessKeyId = AccessKeyId;
        String accessKeySecret = AccessKeySecret;

        String bucketName = BakBucketName;//Bucket名称

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content.getBytes()));
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
