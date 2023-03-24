package com.lhs.common.util;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.lhs.common.config.FileConfig;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerTemplateAvailabilityProvider;

import java.util.Collections;

public class FastAutoGeneratorUtils {

    private final static String url ="jdbc:mysql://localhost:3306/yituliu?serverTimezone=GMT%2B8&characterEncoding=utf-8&useSSL=false&rewriteBatchedStatements=true";
    public static void main1(String[] args) {

        FastAutoGenerator.create(url,"root","root")
                .globalConfig(builder -> {
                    builder.author("sakura")
                            .fileOverride()
                            .outputDir("E:\\Idea_Project\\mybatisPlus");
                })
                .packageConfig(builder -> {
                    builder.parent("com")
                            .moduleName("mybatis")
                            .pathInfo(Collections.singletonMap(OutputFile.mapperXml,"E:\\Idea_Project\\mybatisPlus"));
                })
                .strategyConfig(builder -> {
                    builder.addInclude("stage_result");
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    public static void main(String[] args) {
        
    }



}
