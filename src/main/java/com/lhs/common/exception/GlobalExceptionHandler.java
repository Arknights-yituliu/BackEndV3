package com.lhs.common.exception;


import com.lhs.common.enums.ResultCode;
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
			// 使用异常携带的消息（默认即枚举的 message，若构造时传入自定义消息则透传该消息）
			ResultCode resultCode = ((ServiceException) e).getResultCode();
			result = Result.failure(resultCode.code(), e.getMessage());
		}
		else {
			String message = e.getMessage();
			e.printStackTrace();
			if(message.contains("database")){
				int index = message.indexOf("###");
				if(index>-1){
					int endIndex = message.indexOf("###", index + 3);
					if(endIndex>-1){
						message = message.substring(0,endIndex);
					}
				}

			}
            result = Result.failure(500, message);
		}
		return result;
    }
	
}
