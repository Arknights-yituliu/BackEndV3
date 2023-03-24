package com.lhs.common.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

//开启springboot对swagger的支持
@ConditionalOnProperty(prefix = "swagger2",value = {"enable"},havingValue = "true")
@EnableSwagger2
@Configuration
public class Swagger2Config {
	
	/**
	 * 需要配置扫描controller的包路径
	 */

	@Bean
	public Docket ApiConfig() {
		return new Docket(DocumentationType.SWAGGER_2)
				.groupName("API")
				//设置基本信息
				.apiInfo(apiInfo())
				//初始化并返回一个API选择构造器
				.select()
				//指定扫描的Controller包结构
				.apis(RequestHandlerSelectors.basePackage("com.lhs.controller"))
				//设置路径筛选
				.paths(PathSelectors.any())
				//构建
				.build();
	}



	@Bean
	public WebMvcConfigurer webMvcConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addViewControllers(ViewControllerRegistry registry) {

			}
		};
	}
	
	//swagger界面中显示的基本信息
	private ApiInfo apiInfo() {
		return new ApiInfoBuilder()
				.title("api测试文档")
				.description("")
				.termsOfServiceUrl("")
				.version("1.6")
				.build();
	}
}
