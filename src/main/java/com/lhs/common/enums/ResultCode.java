package com.lhs.common.enums;


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
	FILE_IS_NULL(10008,"文件为空"),
	FILE_NOT_IN_EXCEL_FORMAT(10008, "文件格式必须为.xlsx"),
	OPERATOR_QUANTITY_INVALID(10018,"提交的干员数量需大于6位且小于13位"),


	/* 用户错误：20001-29999*/
	USER_NOT_LOGIN(20001, "用户未登录"),
	PASSWORD_IS_BLANK(20002, "密码为空"),
	ACCOUNT_IS_BLANK(20002, "账号为空"),
	USER_PASSWORD_OR_ACCOUNT_ERROR(20003, "账号不存在或密码错误"),
	USER_BIND_UID(20004, "已经绑定uid了"),
	USER_NOT_EXIST(20006, "用户不存在"),
	USER_IS_EXIST(20007, "用户已存在"),
	USER_FORBIDDEN(20008, "账号已被禁用"),
	USER_NOT_BIND_UID(20009, "未绑定uid"),
	USER_NOT_BIND_EMAIL(20009, "未绑定邮箱"),
	USER_INSUFFICIENT_PERMISSIONS  (20010, "权限不足"),
	REGISTER_TOO_MANY_TIMES(20012, "同ID注册次数过多"),
	USER_PASSWORD_ERROR(20014, "密码错误"),
	NOT_SET_PASSWORD_OR_BIND_EMAIL(20015,"请先设置密码或绑定邮箱"),
	USER_SIGN_IN_ERROR(20016, "登录失败，请向网站开发人员反馈"),
	USER_PERMISSION_NO_ACCESS_OR_TIME_OUT(20017, "用户权限验证失败或操作超时"),
	EMAIL_REGISTERED(20018,"邮箱已被注册"),
	LOGIN_EXPIRATION(20019,"登录过期"),
	EXCESSIVE_IP_ACCESS_TIMES(20020, "同IP注册次数过多,5分钟5次"),
	USER_NAME_LENGTH_TOO_SHORT(20021, "用户名长度必须大于2个字符"),
	USER_NAME_LENGTH_TOO_LONG(20022, "用户名长度不得超过20个字符"),
	PASS_WORD_LENGTH_TOO_SHORT(20023, "密码长度必须大于6个字符"),
	PASS_WORD_LENGTH_TOO_LONG(20024, "密码长度不得超过20个字符"),
	USER_NAME_MUST_BE_IN_CHINESE_OR_ENGLISH(20025, "用户名只能由中文、英文、数字组成"),
	PASS_WORD_MUST_BE_IN_CHINESE_OR_ENGLISH(20026, "密码只能由英文、数字组成"),
	USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN(20027, "用户登录凭证解析错误或用户未登录，请进行反馈"),



	/* 业务错误：30000-39999 */
	/*肉鸽种子站相关报错：30000-30999*/
	ROGUE_SEED_NOT_EXIST(30000, "肉鸽种子不存在"),
	ROGUE_SEED_RATING_COUNT_EXCESSIVE(30002,"种子短时间内评分次数过多"),
	ROGUE_SEED_IS_NULL_OR_FORMAT_ERROR(30000, "肉鸽种子未填写或格式不正确"),
	ROGUE_SEED_TYPE_IS_NULL_OR_ERROR(30000, "种子类型未选择或类型错误"),
	ROGUE_SEED_PARAMS_IS_NULL(30000, "肉鸽种子表单未填写完整"),
	ROGUE_SEED_RATING_ERROR(30000, "肉鸽种子评价错误"),
	ROGUE_SEED_DESCRIPTION_LONGER_THAN_200_CHARACTERS(30000, "肉鸽种子描述超出200字"),
	ROGUE_SEED_DESCRIPTION_LESS_THAN_8_CHARACTERS(30000, "肉鸽种子描述少于8字"),
	ROGUE_SEED_COUNT_LESS_THAN_10(30000, "当前可roll种子总数小于10"),



	/*游戏养成相关报错：31000-31999*/


	/*通用逻辑相关报错：39000-39999*/
	SPECIFIED_QUESTIONED_USER_NOT_EXIST(39000, "业务逻辑出现问题"),
	VERIFICATION_CODE_ERROR(39001, "验证码错误"),
	VERIFICATION_CODE_NOT_ENTER(39002, "验证码未输入"),
	VERIFICATION_CODE_NOT_EXIST(39003, "验证码未发送或已过期"),
	OSS_UPLOAD_ERROR(39005, "OSS上传错误"),
	EMAIL_SENT_TOO_FREQUENTLY(39006,"邮件发送间隔时间30秒"),
	NOT_REPEAT_REQUESTS(39007,"不要重复请求"),


	/* 系统错误：40001-49999 */
	SYSTEM_INNER_ERROR(40001, "系统内部错误，请稍后重试"),
	SYSTEM_TIME_ERROR(40002, "系统时间错误，请稍后重试"),
	
	/* 数据错误：50001-599999 */
	DATA_NONE(50001, "数据未找到"),
	DATA_WRONG(50002, "数据错误"),
	DATA_EXISTED(50003, "数据已存在"),
	FILE_NOT_EXIST(50003, "数据已存在"),
	REDIS_CLEAR_CACHE_ERROR(50004,"缓存清除失败"),
	
	/* 接口错误：60001-69999 */
	INTERFACE_INNER_INVOKE_ERROR(60001, "内部系统接口调用异常"),
	INTERFACE_OUTER_INVOKE_ERROR(60002, "外部系统接口调用异常"),
	INTERFACE_FORBID_VISIT(60003, "该接口禁止访问"),
	INTERFACE_ADDRESS_INVALID(60004, "接口地址无效"),
	INTERFACE_REQUEST_TIMEOUT(60005, "接口请求超时"),

	/* 外部调用错误：70001-79999 */
	INTERFACE_TOO_MANY_EMAIL_SENT(70001,"发送邮件次数过多，请稍后重试"),
	INTERFACE_DAILY_SENDING_LIMIT(70002,"邮件推送服务次数达到上线，无法发送"),
	AUTHORIZATION_FAILURE(70004,"鹰角网络通行证授权失败" ),
	CERTIFICATE_GENERATION_FAILURE(70005,"森空岛凭证生成失败" ),
	SKLAND_CRED_ERROR(10016,"森空岛CRED错误"),


	/* 权限错误：80001-89999 */
	PERMISSION_NO_ACCESS(80001, "无访问权限");



	
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
