package com.lhs.common.exception;


import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;


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
			String message = e.getMessage();
			Logger.error(e.getMessage());
			if(message.contains("SQLException")){
				int i = message.indexOf("###");
				message = message.substring(0,message.indexOf("###",i+3));
			}
            result = Result.failure(500, message);
		}
		return result;
    }
	
}
