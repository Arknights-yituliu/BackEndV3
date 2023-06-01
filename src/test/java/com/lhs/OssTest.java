package com.lhs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.HoneyCake;
import com.lhs.mapper.HoneyCakeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class OssTest {

    @Value("${aliyun.AccessKeyID}")
    private String AccessKeyId;
    @Value("${aliyun.AccessKeySecret}")
    private String AccessKeySecret;


    @Resource
    private HoneyCakeMapper honeyCakeMapper;

    @Test
    void ossUpload() {

        String read = FileUtil.read(ConfigUtil.Backup + "honeyCake.json");
        List<HoneyCake> honeyCakeList = new ArrayList<>();
        JSONObject.parseObject(read).forEach((k,v)->{
            HoneyCake honeyCake = JSONObject.parseObject(String.valueOf(v), HoneyCake.class);
            honeyCake.setName(k);
            honeyCakeMapper.insert(honeyCake);
            honeyCakeList.add(honeyCake);
        });

        String s = JSON.toJSONString(honeyCakeList);
        System.out.println(s);
    }
}
