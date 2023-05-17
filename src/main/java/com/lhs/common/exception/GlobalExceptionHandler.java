package com.lhs.common.exception;


import com.lhs.common.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ResponseBody
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e) {
		Result result = null;
		if(e instanceof ServiceException) {
			result = Result.failure(((ServiceException) e).getResultCode());
		}
		else {
			log.error(e.getMessage());
            result = Result.failure(500, "服务器意外错误,如严重影响使用请联系开发人员");
		}
		return result;
    }
	
}
