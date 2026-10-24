package com.lhs.common.util;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtil {


    /**
     * 保存文件
     * @param filepath 文件路径
     * @param filename 文件名称
     * @param json json
     */
    public static void saveJsonFile(String filepath, String filename, String json){
        File file = new File(filepath);

        if(!file.exists()){
            if (!file.mkdirs()) {
                Logger.error("创建目录失败：{}", filepath);
            }
        }

        File file1 = new File(filepath,filename);

        if(!file1.exists()){
            try {
                file1.createNewFile();
            } catch (IOException e) {
                Logger.error("创建文件失败：{}", file1.getAbsolutePath(), e);
            }
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(filepath + filename)) {
            fileOutputStream.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.error("写入文件失败：{}", filepath + filename, e);
        }

    }

    public static void saveJsonFile(HttpServletResponse response, String filePath, String fileName, String jsonForMat) {
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
            Logger.error("导出文件失败：{}", filePath + fileName, e);
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
            Logger.error("读取文件失败：{}", fileName, e);
            return null;
        }
    }


    public static String read(File file) {
        String context = "";
        try {
            Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
            int ch = 0;
            StringBuilder sb = new StringBuilder();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }

            reader.close();
            context = sb.toString();
            return context;

        } catch (IOException e) {
            Logger.error("读取文件失败：{}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * 按文件头（魔数）推导图片扩展名，非图片返回 null
     * <p>
     * 扩展名完全由文件内容决定，不读取、不信任 {@code originalFilename}，
     * 因此无法通过改文件名把 .html/.svg 之类的可执行内容伪装成图片写入静态桶
     *
     * @param multipartFile 上传文件
     * @return 扩展名（jpg/png/webp/gif），非图片返回 null
     */
    public static String detectImageExt(MultipartFile multipartFile) {
        if (multipartFile == null) {
            return null;
        }
        byte[] h;
        try (InputStream in = multipartFile.getInputStream()) {
            h = in.readNBytes(12);
        } catch (IOException e) {
            return null;
        }
        if (h.length < 12) {
            return null;
        }
        // JPEG: FF D8 FF
        if (u(h[0]) == 0xFF && u(h[1]) == 0xD8 && u(h[2]) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (u(h[0]) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && u(h[4]) == 0x0D && u(h[5]) == 0x0A && u(h[6]) == 0x1A && u(h[7]) == 0x0A) {
            return "png";
        }
        // WEBP: RIFF....WEBP
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        // GIF: GIF87a / GIF89a
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8'
                && (h[4] == '7' || h[4] == '9') && h[5] == 'a') {
            return "gif";
        }
        return null;
    }

    /**
     * 图片扩展名对应的 Content-Type
     * <p>
     * 上传时显式写入对象元信息，避免存储侧按扩展名猜测导致的可执行内容渲染
     *
     * @param ext {@link #detectImageExt} 返回的扩展名
     * @return MIME 类型
     */
    public static String imageContentType(String ext) {
        if (ext == null) {
            return "application/octet-stream";
        }
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private static int u(byte b) {
        return b & 0xFF;
    }


    public static void copyFile(File source, File dest) {

        try {
            FileInputStream is = new FileInputStream(source);
            FileOutputStream os = new FileOutputStream(dest);
            FileChannel sourceChannel = is.getChannel();
            FileChannel destChannel = os.getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
            sourceChannel.close();
            destChannel.close();
            is.close();
            os.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            Logger.error("输入流转文件失败：{}", file.getAbsolutePath(), e);
        }
    }


    public static byte[] zipFile(File fileToZip) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)) {
            zipFile(fileToZip, fileToZip.getName(), zipOut);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            // 如果是目录，则遍历子文件
            for (File childFile : Objects.requireNonNull(fileToZip.listFiles())) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(fileToZip)) {
                ZipEntry zipEntry = new ZipEntry(fileName);
                zipOut.putNextEntry(zipEntry);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    zipOut.write(buffer, 0, length);
                }
            }
        }
    }




    /**
     * 获取文件头
     *
     * @param filePath 文件路径
     * @return 16 进制的文件投信息
     *
     * @throws IOException
     */
    private static String getFileHeader(String filePath) throws IOException {
        byte[] b = new byte[28];
        InputStream inputStream = Files.newInputStream(Paths.get(filePath));
        inputStream.read(b, 0, 28);
        inputStream.close();
        return bytes2hex(b);
    }

    /**
     * 获取文件头
     *
     * @param multipartFile 文件
     * @return 16 进制的文件投信息
     *
     * @throws IOException
     */
    private static String getFileHeader(MultipartFile multipartFile) throws IOException {
        byte[] b = new byte[10];

        InputStream inputStream = multipartFile.getInputStream();
        inputStream.read(b, 0, b.length);
        inputStream.close();
        return bytes2hex(b);
    }

    /**
     * 将字节数组转换成16进制字符串
     */
    private static String bytes2hex(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString().toUpperCase();
    }



    /**
     * 校验是否为excel
     *
     * @param filePath 文件路径
     * @return 文件类型
     *
     * @throws IOException
     */
    public static boolean checkFileType(String filePath,String fileType) {
        String fileHead = null;
        try {
            fileHead = getFileHeader(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (null == fileHead || fileHead.length() == 0) {
            return false;
        }
        //校验是否为xls或者xlsx文件
        return Objects.equals(fileHead, fileType);
    }

    /**
     * 校验是否为excel
     *
     * @param multipartFile 文件
     * @return 文件类型
     *
     */
    public static boolean checkFileType(MultipartFile multipartFile,String fileType) {

        String fileHead = null;
        try {
            fileHead = getFileHeader(multipartFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (null == fileHead || fileHead.length() == 0) {
            return false;
        }

        //校验文件格式
        return fileHead.startsWith(fileType);
    }




}
