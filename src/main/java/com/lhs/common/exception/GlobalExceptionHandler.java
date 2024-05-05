package com.lhs.common.exception;


import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * 全局异常处理
 */

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ResponseBody
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e) {
		Result<Object> result = null;
		if(e instanceof ServiceException) {
			result = Result.failure(((ServiceException) e).getResultCode());
		}
		else {
			Logger.error(e.getMessage());
			e.printStackTrace();
            result = Result.failure(500, "服务器内部错误");
		}
		return result;
    }
	
}
