package com.lhs.utils;

import com.lhs.common.util.SpriteCreateUtil;
import com.lhs.entity.dto.util.SpriteInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


public class spriteTest {
    @Test
    void spriteGeneratorTest() {
        // 定义格式化方式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 获取当前日期
        LocalDate today = LocalDate.now();
        // 格式化为字符串
        String todayStr = today.format(formatter);
        String spriteFilename = "avatar"+todayStr+".png";
        String inputDir = "C:\\WebStormProject\\ak-resources\\image\\avatar\\";     // 图片输入目录
        String outputDir = "C:\\WebStormProject\\frontend-v2-plus\\src\\assets\\css\\sprite\\";    // 输出目录
        String cssFilename = "sprite_avatar.css";

        SpriteInfo spriteInfo = new SpriteInfo();
        spriteInfo.setInputDir(inputDir);
        spriteInfo.setOutputDir(outputDir);
        spriteInfo.setSpriteFilename(spriteFilename);
        spriteInfo.setCssFilename(cssFilename);
        spriteInfo.setCosFileType("webp");

        SpriteCreateUtil.spriteCreate(spriteInfo);
    }


    @Test
    void spriteGeneratorTest2() {
        // 定义格式化方式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 获取当前日期
        LocalDate today = LocalDate.now();
        // 格式化为字符串
        String todayStr = today.format(formatter);
        String spriteFilename = "skills"+todayStr+".png";
        String inputDir = "C:\\WebStormProject\\ArknightsGameResource\\skill\\";     // 图片输入目录
        String outputDir = "C:\\WebStormProject\\frontend-v2-plus\\src\\assets\\css\\sprite\\";    // 输出目录
        String cssFilename = "sprite_skill.css";

        SpriteInfo spriteInfo = new SpriteInfo();
        spriteInfo.setInputDir(inputDir);
        spriteInfo.setOutputDir(outputDir);
        spriteInfo.setSpriteFilename(spriteFilename);
        spriteInfo.setCssFilename(cssFilename);
        spriteInfo.setCosFileType("jpg");

        SpriteCreateUtil.spriteCreate(spriteInfo);
    }

    @Test
    void spriteGenerator() {

        String inputDir = "C:\\WebStormProject\\ak-resources\\image\\avatar\\";     // 图片输入目录
        String outputDir = "C:\\WebStormProject\\frontend-v2-plus\\src\\assets\\css\\sprite\\";    // 输出目录
        // 获取当前日期
        LocalDate today = LocalDate.now();

        // 定义格式化方式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 格式化为字符串
        String todayStr = today.format(formatter);
        String spriteFilename = "avatar"+todayStr+".png";
        String cssFilename = "sprite_avatar.css";

        String cosLink = "https://cos.yituliu.cn/sprite/avatar"+todayStr+".webp";


        try {
            // 用于存储所有读取的图片
            List<BufferedImage> images = new ArrayList<>();
            // 用于保存每张图片在雪碧图中的位置和尺寸
            Map<String, Rectangle> imagePositions = new HashMap<>();

            // 获取 images 目录下所有 .png 文件，并按名称排序
            List<Path> imageFiles = Files.list(Paths.get(inputDir))
                    .filter(path -> path.toString().endsWith(".png"))   // 只保留 .png 文件
                    .sorted()  // 按文件名排序（保证顺序一致）
                    .toList(); // 转换为不可变列表

            // 设置每行最多显示 10 张图片
            int cols = 10;
            // 当前绘制的 x 坐标
            int currentX = 0;
            // 当前绘制的 y 坐标
            int currentY = 0;
            // 当前行的高度（取最大高度）
            int rowHeight = 0;

            // 用于保存所有图片、它们的名字和位置
            List<BufferedImage> allImages = new ArrayList<>();
            List<String> allNames = new ArrayList<>();

            // 遍历所有图片文件，读取图片并保存到列表中
            for (Path path : imageFiles) {
                // 使用 ImageIO 读取图片
                BufferedImage img = ImageIO.read(path.toFile());
                // 获取文件名（不带后缀）
                String name = trimExtension(path.getFileName().toString());
                // 添加到图片列表
                allImages.add(img);
                // 添加到名字列表
                allNames.add(name);
            }

            // 计算最终雪碧图的总宽度和总高度
            int spriteWidth = 0;
            int spriteHeight = 0;

            int rowWidth = 0;
            int maxRowHeight = 0;

            // 第一次遍历：计算每行的最大高度和累计宽度
            for (int i = 0; i < allImages.size(); i++) {
                BufferedImage img = allImages.get(i);
                rowWidth += img.getWidth();  // 累加当前行宽度
                maxRowHeight = Math.max(maxRowHeight, img.getHeight());  // 记录当前行最大高度

                // 如果是第 10 张图或最后一张图，记录这一行的尺寸
                if ((i + 1) % cols == 0 || i == allImages.size() - 1) {
                    // 更新总宽度（取最大行宽）
                    spriteWidth = Math.max(spriteWidth, rowWidth);
                    // 更新总高度（累加行高）
                    spriteHeight += maxRowHeight;

                    // 重置行宽和行高
                    rowWidth = 0;
                    maxRowHeight = 0;
                }
            }

            // 创建一个空白的 BufferedImage 作为最终的雪碧图
            BufferedImage sprite = new BufferedImage(spriteWidth, spriteHeight, BufferedImage.TYPE_INT_ARGB);

            // 获取绘图对象
            Graphics2D spriteGraphics = sprite.createGraphics();
            // 设置抗锯齿渲染（让图片边缘更平滑）
            spriteGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // 重置坐标

            // 第二次遍历：将图片绘制到雪碧图上
            for (int i = 0; i < allImages.size(); i++) {
                BufferedImage img = allImages.get(i);
                String name = allNames.get(i);

                // 设置图片在雪碧图中的位置
                Rectangle rectangle = new Rectangle(currentX, currentY, img.getWidth(), img.getHeight());
                imagePositions.put(name, rectangle);

                // 绘制图片到雪碧图上
                spriteGraphics.drawImage(img, currentX, currentY, null);

                // 更新 x 坐标（向右移动）
                currentX += img.getWidth();
                // 记录当前行最大高度
                rowHeight = Math.max(rowHeight, img.getHeight());

                // 如果是第 10 张图或最后一张图，换行
                if ((i + 1) % cols == 0 || i == allImages.size() - 1) {
                    currentY += rowHeight;  // 向下移动一行
                    currentX = 0;           // 重置 x 坐标
                    rowHeight = 0;          // 重置行高
                }
            }

            // 释放绘图资源
            spriteGraphics.dispose();

            // 创建输出目录（如果不存在）
            new File(outputDir).mkdirs();

            // 保存雪碧图到文件
            File spriteFile = new File(outputDir, spriteFilename);
            ImageIO.write(sprite, "PNG", spriteFile);
            System.out.println("✅ 雪碧图已生成: " + spriteFile.getAbsolutePath());

            // 保存 CSS 文件
            File cssFile = new File(outputDir, cssFilename);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(cssFile))) {
                // 遍历所有图片的位置信息，生成对应的 CSS 类
                for (Map.Entry<String, Rectangle> entry : imagePositions.entrySet()) {
                    String className = entry.getKey();
                    Rectangle rect = entry.getValue();

                    // 写入 CSS 样式
                    writer.write(String.format(".bg-%s {\n", className));
                    writer.write(String.format("    background-image: url(\"%s\");\n", cosLink));
                    writer.write(String.format("    background-position: -%dpx -%dpx;\n", rect.x, rect.y));
                    writer.write(String.format("    width: %dpx;\n", rect.width));
                    writer.write(String.format("    height: %dpx;\n", rect.height));
                    writer.write("}\n\n");
                }
            }
            System.out.println("✅ CSS 文件已生成: " + cssFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("处理文件时出错");
            e.printStackTrace();
        }
    }

    /**
     * 去除文件名的扩展名
     * @param filename 原始文件名
     * @return 不带扩展名的文件名
     */
    private static String trimExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? filename : filename.substring(0, dotIndex);
    }

}
