package com.lhs.service.util.impl;

import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.LogUtils;
import com.lhs.common.enums.ResultCode;
import com.lhs.service.util.TencentCloudService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import com.tencentcloudapi.cdn.v20180606.CdnClient;
import com.tencentcloudapi.cdn.v20180606.models.PurgePathCacheRequest;
import com.tencentcloudapi.cdn.v20180606.models.PurgePathCacheResponse;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class TencentCloudServiceImpl implements TencentCloudService {
    @Override
    public void uploadFileToCOS(File file, String bucketPath) {
        uploadCOS(file, bucketPath);
    }

    @Override
    public void uploadJsonToCOS(String text, String bucketPath) {
        try {
            File file = convertJsonStringToFile(text);
            uploadFileToCOS(file,bucketPath);
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
    public void uploadFileToCOS(MultipartFile multipartFile, String bucketPath) {
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





    @Override
    public void frontEndDeployment(String regionStr,String bucketName) {
        String secretId = ConfigUtil.CosSecretId;
        String secretKey = ConfigUtil.CosSecretKey;

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域的简称请参见 https://cloud.tencent.com/document/product/436/6224
        // clientConfig 中包含了设置 region, https(默认 http), 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
        Region region = new Region(regionStr);
        ClientConfig clientConfig = new ClientConfig(region);
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        COSClient cosClient = new COSClient(cred, clientConfig);
        // 指定文件将要存放的存储桶

        // 获取存储桶中的所有对象列表
        ObjectListing objectListing = cosClient.listObjects(bucketName);

        List<COSObjectSummary> cosObjectSummaries = objectListing.getObjectSummaries();

        while (!cosObjectSummaries.isEmpty()) {

            // 批量删除对象
            List<DeleteObjectsRequest.KeyVersion> keyList = new ArrayList<>();
            for (COSObjectSummary summary : cosObjectSummaries) {
                keyList.add(new DeleteObjectsRequest.KeyVersion(summary.getKey()));
            }

            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
            deleteObjectsRequest.setKeys(keyList);
            cosClient.deleteObjects(deleteObjectsRequest);
            // 获取下一批次的对象列表
            objectListing = cosClient.listNextBatchOfObjects(objectListing);
            cosObjectSummaries = objectListing.getObjectSummaries();
        }

        System.out.println("存储桶已删除干净！");

        List<Path> files = getFiles();
        for (Path path : files) {
            File file = path.toFile();
            String bucketPath = extractPathAfterDist(path);
//            System.out.println(bucketPath);
            if(bucketPath.contains("/image")){
                continue;
            }

            if (!file.exists()) {
                System.err.println("文件不存在: " + file.getAbsolutePath());
                continue;
            }


            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    bucketPath, // 对象键名，这里使用文件名作为键名
                    file
            );
            cosClient.putObject(putObjectRequest);
            LogUtils.info(bucketPath + " 上传成功！");
        }



        cosClient.shutdown();
    }


    /**
     * 从给定的 Path 对象中提取出 'dist' 后的路径部分。
     *
     * @param path 完整路径
     * @return 提取出的 'dist' 后的路径字符串
     */
    public static String extractPathAfterDist(Path path) {
        // 获取路径的各个部分
        List<String> parts = new LinkedList<>();
        path.forEach(p -> parts.add(p.toString()));
        // 寻找 'dist' 的索引位置
        int distIndex = parts.indexOf("dist");
        if (distIndex == -1) {
            // 如果找不到 'dist'，返回空字符串
            return "";
        } else {
            // 截取 'dist' 后的部分
            return String.join("/", parts.subList(distIndex + 1, parts.size()));
        }
    }

    private List<Path> getFiles() {
        // 指定目录路径
        List<Path> paths = new ArrayList<>();
        Path directoryPath = Paths.get("C:\\VCProject\\frontend-v2-plus\\dist");
        try {
            // 获取目录下的所有文件路径
            paths = Files.walk(directoryPath)
                    .filter(Files::isRegularFile)
                    .toList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paths;
    }

    @Override
    public void CDNRefreshDirectory(String domain)  {
        String secretId = ConfigUtil.CosSecretId;
        String secretKey = ConfigUtil.CosSecretKey;
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(secretId, secretKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("cdn.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            CdnClient client = new CdnClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            PurgePathCacheRequest req = new PurgePathCacheRequest();
            String[] paths1 = {domain};
            req.setPaths(paths1);

            req.setFlushType("flush");
            req.setArea("mainland");
            // 返回的resp是一个PurgePathCacheResponse的实例，与请求对象对应
            PurgePathCacheResponse resp = client.PurgePathCache(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
        }
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
