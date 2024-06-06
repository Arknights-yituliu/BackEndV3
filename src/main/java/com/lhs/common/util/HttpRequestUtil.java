package com.lhs.common.util;

import com.lhs.common.exception.ServiceException;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestUtil {
    /**
     * @Description: 发送get请求
     */
    public static String get(String url, Map<String,String> header) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);

        if(!header.isEmpty()) {
            header.forEach(httpGet::setHeader);
        }

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();
        httpGet.setConfig(requestConfig);

        CloseableHttpResponse httpResponse = null;
        try {

            httpResponse = httpClient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();
            if(httpResponse.getStatusLine().getStatusCode() != 200){
                Logger.info(EntityUtils.toString(entity));
                return null;
            }
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * @Description: 发送http post请求
     */
    public static String post(String url, HashMap<String,String> header , String data) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(35000)
                .setConnectionRequestTimeout(35000)
                .setSocketTimeout(60000).build();
        httpPost.setConfig(requestConfig);
        httpPost.setHeader("Content-type", "application/json");

        if(!header.isEmpty()) {
            header.forEach(httpPost::setHeader);
        }

        CloseableHttpResponse httpResponse = null;
        try {
            httpPost.setEntity(new StringEntity(data,"utf-8"));

            httpResponse = httpClient.execute(httpPost);
            if(httpResponse.getStatusLine().getStatusCode() != 200){
                HttpEntity entity = httpResponse.getEntity();
                String errorMessage = EntityUtils.toString(entity, "UTF-8");
                Logger.error("Http Request Message: " + errorMessage);
            }
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            Logger.error(e.getMessage());
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    Logger.error(e.getMessage());
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    Logger.error(e.getMessage());
                }
            }
        }
        return null;
    }



}
