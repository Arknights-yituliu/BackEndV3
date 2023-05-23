package com.lhs;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;

@SpringBootTest
public class OssTest {

    @Value("${aliyun.AccessKeyID}")
    private String AccessKeyId;
    @Value("${aliyun.AccessKeySecret}")
    private String AccessKeySecret;


    @Test
    void ossUpload() {
    }
}
