package com.lhs.common.exception;

import com.lhs.common.enums.ResultCode;

/**
 * 自定义service层异常类
 */
public class ServiceException extends RuntimeException{


	private ResultCode resultCode;

	
	public ServiceException(ResultCode resultCode) {
		super(resultCode.message());
		this.resultCode = resultCode;
	}

	/**
	 * 带自定义消息的构造器，用于将上游（第三方接口）的报错透传给前端
	 *
	 * @param resultCode 错误码
	 * @param message    自定义错误消息
	 */
	public ServiceException(ResultCode resultCode, String message) {
		super(message);
		this.resultCode = resultCode;
	}


	public ResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(ResultCode resultCode) {
		this.resultCode = resultCode;
	}

	
}
