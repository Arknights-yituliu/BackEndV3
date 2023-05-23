package com.lhs.common.util;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

public class FileUtil {



    public static void save(String filepath,String filename,String json){
        File file = new File(filepath);
        if(!file.exists()){
            file.mkdir();
        }

        File file1 = new File(filepath,filename);
        if(!file1.exists()){
            try {
                file1.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filepath+filename);
            byte[] bytes = json.getBytes();
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void save(HttpServletResponse response, String filePath, String fileName, String jsonForMat) {
        try {
            // 拼接文件完整路径
            String fullPath = filePath + fileName ;

            // 保证创建一个新文件
            File file = new File(fullPath);
            if (!file.getParentFile().exists()) { // 如果父目录不存在，创建父目录
                file.getParentFile().mkdirs();
            }
            if (file.exists()) { // 如果已存在,删除旧文件
                file.delete();
            }
            file.createNewFile();

            // 格式化json字符串
            FileInputStream fileInputStream = new FileInputStream(file);
            // 将格式化后的字符串写入文件
            Writer write = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            write.write(jsonForMat);
            write.flush();
            write.close();

//          response.setContentType("application/force-download");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName );
            OutputStream outputStream = response.getOutputStream();
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = fileInputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            fileInputStream.close();
            outputStream.close();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    public static String read(String fileName) {
        String context = "";
        try {
            File jsonFile = new File(fileName);
            Reader reader = new InputStreamReader(Files.newInputStream(jsonFile.toPath()), StandardCharsets.UTF_8);
            int ch = 0;
            StringBuilder sb = new StringBuilder();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }

            reader.close();
            context = sb.toString();
            return context;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String read(MultipartFile multipartFile) {
        String jsonStr = "";
        try {
            File file = null;
            if (multipartFile.getSize() <= 0) {
                multipartFile = null;
            } else {
                InputStream ins = null;
                ins = multipartFile.getInputStream();
                file = new File(Objects.requireNonNull(multipartFile.getOriginalFilename()));
                inputStreamToFile(ins, file);
                ins.close();
            }

            assert file != null;
            Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
            int ch = 0;
            StringBuilder sb = new StringBuilder();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }

            reader.close();
            jsonStr = sb.toString();
            boolean delete = file.delete();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void inputStreamToFile(InputStream ins, File file) {
        try {
            OutputStream os = Files.newOutputStream(file.toPath());
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            ins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
