package com.lhs.common.exception;

import com.lhs.common.util.ResultCode;

/**
 * 自定义service层异常类
 */
public class ServiceException extends RuntimeException{


	private ResultCode resultCode;
	
	public ServiceException(ResultCode resultCode) {
		this(resultCode.message());
		this.resultCode = resultCode;
	}
	
	private ServiceException(String message) {
		super(message);
	}

	public ResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(ResultCode resultCode) {
		this.resultCode = resultCode;
	}

	
}
