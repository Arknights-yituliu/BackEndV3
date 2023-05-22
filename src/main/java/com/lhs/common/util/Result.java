package com.lhs.common.util;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 统一Controller中RESTFul风格接口返回的结果
 */
@ApiModel(value = "统一数据返回对象")
public class Result<T> implements Serializable {

	private static final long serialVersionUID = 1L;
	@ApiModelProperty(required = true,value = "返回状态码", dataType = "int", example = "200")
	private Integer code;
	@ApiModelProperty(required = true, value = "返回message信息", dataType = "string", example = "操作成功", position = 1)
	private String msg;
	@ApiModelProperty(required = true, value = "返回数据", dataType = "string", example = "返回数据", position = 2)
	private T data;
	
	private Result() {}
	
	private Result(Integer code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	
	private void setResultCode(ResultCode code) {
		this.code = code.code();
		this.msg = code.message();
	}
	
	
	/**
	 * 操作失败，自定义code和msg
	 */
	public static <T> Result<T> failure(Integer code, String msg) {
		Result<T> result = new Result<T>(code,msg);
		return result;
	}
	
	/**
	 * 操作成功，没有返回的数据
	 */
	public static <T> Result<T> success() {
		Result<T> result = new Result<T>();
		result.setResultCode(ResultCode.SUCCESS);
		return result;
	}
	
	/**
	 * 操作成功，有返回的数据
	 */
	public static  <T> Result<T> success(T data) {
		Result<T> result = new Result<T>();
		result.setResultCode(ResultCode.SUCCESS);
		result.setData(data);
		return result;
	}
	
	/**
	 * 操作失败，没有返回的数据
	 */
	public static <T> Result<T> failure(ResultCode resultCode) {
		Result<T> result = new Result<T>();
		result.setResultCode(resultCode);
		return result;
	}
	
	/**
	 * 操作失败，有返回的数据
	 */
	public static <T> Result<T> failure(ResultCode resultCode, T data) {
		Result<T> result = new Result<T>();
		result.setResultCode(resultCode);
		result.setData(data);
		return result;
	}
	
	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
	
}
