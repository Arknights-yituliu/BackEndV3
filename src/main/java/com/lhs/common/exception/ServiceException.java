package com.lhs.common.exception;

import com.lhs.common.enums.ResultCode;

/**
 * 自定义service层异常类
 */
public class ServiceException extends RuntimeException{


	private ResultCode resultCode;

	
	public ServiceException(ResultCode resultCode) {
		this.resultCode = resultCode;
	}


	public ResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(ResultCode resultCode) {
		this.resultCode = resultCode;
	}

	
}
