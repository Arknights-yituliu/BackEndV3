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
	FILE_NOT_IN_EXCEL_FORMAT(10008, "文件格式必须为.xlsx"),
	USER_NAME_LENGTH_TOO_SHORT(10009, "用户名长度必须大于2个字符"),
	USER_NAME_LENGTH_TOO_LONG(10010, "用户名长度不得超过20个字符"),
	PASS_WORD_LENGTH_TOO_SHORT(10011, "密码长度必须大于6个字符"),
	PASS_WORD_LENGTH_TOO_LONG(10012, "密码长度不得超过20个字符"),
	USER_NAME_MUST_BE_IN_CHINESE_OR_ENGLISH(100013, "用户名只能由中文、英文、数字组成"),
	PASS_WORD_MUST_BE_IN_CHINESE_OR_ENGLISH(100015, "密码只能由英文、数字组成"),
	SKLAND_CRED_ERROR(10016,"森空岛CRED错误"),
	EXCESSIVE_IP_ACCESS_TIMES(10017, "同IP注册次数过多,5分钟5次"),

	
	/* 用户错误：20001-29999*/
	USER_NOT_LOGIN(20001, "用户未登录"),
	USER_NEED_PASSWORD(20002, "需要输入密码"),
	USER_LOGIN_ERROR(20003, "账号不存在或密码错误"),
	USER_BIND_UID(20004, "已经绑定uid了"),

	USER_NOT_EXIST(20006, "用户不存在"),
	USER_EXISTED(20007, "用户已存在"),
	USER_FORBIDDEN(20008, "账号已被禁用"),
	USER_NOT_BIND_UID(20009, "未绑定uid"),
	USER_INSUFFICIENT_PERMISSIONS  (20010, "权限不足"),
	REGISTER_TOO_MANY_TIMES(20012, "同ID注册次数过多"),
	USER_PASSWORD_ERROR(20014, "密码错误"),
	NOT_SET_PASSWORD_OR_BIND_EMAIL(20015,"请先设置密码或绑定邮箱"),

	
	/* 业务错误：30001-39999 */
	SPECIFIED_QUESTIONED_USER_NOT_EXIST(30001, "业务逻辑出现问题"),
	CODE_ERROR(30002, "验证码错误"),
	CODE_NOT_EXIST(30003, "验证码不存在或已过期"),
	CODE_NOT_SEND(30004, "验证码未发送"),
	OSS_UPLOAD_ERROR(30005, "OSS上传错误"),
	EMAIL_SENT_TOO_FREQUENTLY(30006,"邮件发送间隔时间30秒"),
	OPERATION_INTERVAL_TOO_SHORT(30007,"操作间隔过短,10s仅可上传一次"),


		/* 系统错误：40001-49999 */
	SYSTEM_INNER_ERROR(40001, "系统内部错误，请稍后重试"),
	SYSTEM_TIME_ERROR(40002, "系统时间错误，请稍后重试"),
	
	/* 数据错误：50001-599999 */
	DATA_NONE(50001, "数据未找到"),
	DATA_WRONG(50002, "数据错误"),
	DATA_EXISTED(50003, "数据已存在"),
	FILE_NOT_EXIST(50003, "数据已存在"),
	
	
	/* 接口错误：60001-69999 */
	INTERFACE_INNER_INVOKE_ERROR(60001, "内部系统接口调用异常"),
	INTERFACE_OUTER_INVOKE_ERROR(60002, "外部系统接口调用异常"),
	INTERFACE_FORBID_VISIT(60003, "该接口禁止访问"),
	INTERFACE_ADDRESS_INVALID(60004, "接口地址无效"),
	INTERFACE_REQUEST_TIMEOUT(60005, "接口请求超时"),
	INTERFACE_TOO_MANY_EMAIL_SENT(60006,"发送邮件次数过多，请稍后重试"),
	INTERFACE_DAILY_SENDING_LIMIT(60007,"邮件推送服务次数达到上线，无法发送"),
	
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
