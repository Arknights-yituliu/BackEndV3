package com.lhs.common.config;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
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
				.groupName("api")
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

	@ConditionalOnProperty(prefix = "swagger2",value = {"type"},havingValue = "dev")
	@Bean
	public Docket PrivateConfig() {
		return new Docket(DocumentationType.SWAGGER_2)
				.groupName("private")
				//设置基本信息
				.apiInfo(apiInfo())
				//初始化并返回一个API选择构造器
				.select()
				//指定扫描的Controller包结构
				.apis(RequestHandlerSelectors.basePackage("com.lhs.dev"))
				//设置路径筛选
				.paths(PathSelectors.any())
				//构建
				.build()
				.securitySchemes(security())
				.securityContexts(securityContexts())
				.ignoredParameterTypes(HttpServletRequest.class,HttpServletResponse.class);
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
				.title("一图流API文档")
				.description("一图流的公开API，包括关卡，材料，商店等API")
				.termsOfServiceUrl("")
				.version("3.2")
				.build();
	}

	/**
	 * 设置认证中显示的显示的基本信息
	 */
	private List<ApiKey> security() {
		return Collections.singletonList(
				new ApiKey("Authorization", "Authorization", "header")
		);
	}

	/**
	 * 设置认证规则
	 */
	private List<SecurityContext> securityContexts() {

		List<String> antPaths = new ArrayList<String>();
		antPaths.add("/**");

		return Collections.singletonList(
				SecurityContext.builder()
						.securityReferences(defaultAuth())
						.forPaths(antPathsCondition(antPaths))
						.build()
		);
	}

	/**
	 * 返回认证路径的条件
	 */
	private Predicate<String> antPathsCondition(List<String> antPaths){

		List<Predicate<String>> list = new ArrayList<>();

		antPaths.forEach(path->list.add(PathSelectors.ant(path)));

		return Predicates.or(list);

	}

	/**
	 * 设置认证的范围，以及认证的字段名称
	 */
	private List<SecurityReference> defaultAuth() {
		AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
		AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
		authorizationScopes[0] = authorizationScope;
		return Collections.singletonList(
				new SecurityReference("Authorization", authorizationScopes));
	}


}
