package com.lhs.common.util;


/**
 * 统一并自定义返回状态码，如有需求可以另外增加
 */
public enum ResultCode {
	
	/* 成功状态码 */
	SUCCESS(200, "操作成功"),
	
	/* 参数错误：10001-19999 */
	PARAM_IS_INVALID(10001, "参数无效"),
	PARAM_IS_BLANK(10002, "参数为空"),
	PARAM_TYPE_BIND_ERROR(10003, "参数类型错误"),
	PARAM_NOT_COMPLETE(10004, "参数缺失"),
	PARAM_INVALID(10005, "含有非法参数，请检查上传内容"),
	FILE_TYPE_INVALID(10006, "文件格式不正确"),
	FILE_SIZE_LARGE(10007, "文件过大"),

	FILE_NOT_IN_EXCEL_FORMAT(10006, "文件格式必须为.xlsx"),
	MAA_LOW_VERSION(10008, "数据不完整，请将MAA版本升级到V4.16.2版本之后"),

	
	/* 用户错误：20001-29999*/
	USER_NOT_LOGIN(20001, "用户未登录"),
	USER_LOGIN_ERROR(20002, "账号不存在或密码错误"),
	USER_ACCOUNT_FORBIDDEN(20003, "账号已被禁用"),
	USER_NOT_EXIST(20004, "用户不存在"),
	USER_HAS_EXISTED(20005, "用户已存在"),
	USER_INSUFFICIENT_PERMISSIONS  (20006, "权限不足"),
	USER_ID_ERROR(20007, "同ID注册次数过多"),
	USER_IP_TOO_MANY_TIMES(20008, "同IP注册次数过多,60分钟5次"),
	USER_REGISTER_ERROR(20008, "同IP注册次数过多,60分钟5次"),
	
	/* 业务错误：30001-39999 */
	SPECIFIED_QUESTIONED_USER_NOT_EXIST(30001, "业务逻辑出现问题"),
	USER_LOGIN_CODE_ERROR(30002, "验证码错误"),
	
	/* 系统错误：40001-49999 */
	SYSTEM_INNER_ERROR(40001, "系统内部错误，请稍后重试"),
	
	/* 数据错误：50001-599999 */
	DATA_NONE(50001, "数据未找到"),
	DATA_WRONG(50002, "数据错误"),
	DATA_EXISTED(50003, "数据已存在"),

	
	
	/* 接口错误：60001-69999 */
	INTERFACE_INNER_INVOKE_ERROR(60001, "内部系统接口调用异常"),
	INTERFACE_OUTTER_INVOKE_ERROR(60002, "外部系统接口调用异常"),
	INTERFACE_FORBID_VISIT(60003, "该接口禁止访问"),
	INTERFACE_ADDRESS_INVALID(60004, "接口地址无效"),
	INTERFACE_REQUEST_TIMEOUT(60005, "接口请求超时"),
	
	/* 权限错误：70001-79999 */
	PERMISSION_NO_ACCESS(70001, "无访问权限");
	
	
	private Integer code;

	private String message;

	ResultCode(Integer code, String message) {
		this.code = code;
		this.message = message;
	}

	public Integer code() {
		return this.code;
	}

	public String message() {
		return this.message;
	}
}
