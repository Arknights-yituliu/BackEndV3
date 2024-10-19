package com.lhs.service.util;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class OSSService {

    @Value("${aliyun.accessKeyID}")
    private  String AccessKeyId;
    @Value("${aliyun.accessKeySecret}")
    private  String AccessKeySecret;
    @Value("${aliyun.bakBucketName}")
    private  String BakBucketName;

    /**
     *
     * @param content 上传内容
     * @param objectName  资源路径
     * @return 是否上传成功
     */
    public Boolean upload(String content, String objectName) {
        String endpoint = "https://oss-cn-shanghai.aliyuncs.com";
        String accessKeyId = AccessKeyId;
        String accessKeySecret = AccessKeySecret;

        String bucketName = BakBucketName;//Bucket名称

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        PutObjectResult putObjectResult ;
        try {
            putObjectResult =  ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content.getBytes()));
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

        return true;
    }

    public void uploadFileInputStream(InputStream inputStream, String objectName){
        String endpoint = "https://oss-cn-shanghai.aliyuncs.com";
        String accessKeyId = AccessKeyId;
        String accessKeySecret = AccessKeySecret;

        String bucketName = BakBucketName;//Bucket名称

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
            // 创建PutObject请求。
            PutObjectResult result = ossClient.putObject(putObjectRequest);
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


    public String read(String objectName){
        String endpoint = "https://oss-cn-shanghai.aliyuncs.com";
        String accessKeyId = AccessKeyId;
        String accessKeySecret = AccessKeySecret;

        String bucketName = BakBucketName;//Bucket名称
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        String context = "";

        try {
            OSSObject object = ossClient.getObject(bucketName, objectName);
            InputStream objectContent = object.getObjectContent();

            Reader reader = new InputStreamReader(objectContent, StandardCharsets.UTF_8);
            int ch = 0;
            StringBuilder sb = new StringBuilder();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            reader.close();
            context = sb.toString();

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
        } catch (IOException ie){
            System.out.println(ie.getMessage());
        }
        finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        return context;
    }
}
