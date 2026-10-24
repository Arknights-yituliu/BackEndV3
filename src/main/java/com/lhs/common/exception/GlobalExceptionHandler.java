package com.lhs.common.exception;


import com.lhs.common.enums.ResultCode;
import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;


/**
 * 全局异常处理
 * <p>
 * 对外只返回错误码与排查号，堆栈与内部细节只进日志：
 * 原实现把 {@code e.getMessage()} 原样返回前端，SQL 片段、表名、字段名、SDK 报错、
 * 内部路径都会泄露，且 message 为 null 时处理器自身还会 NPE。
 * <p>
 * 约定不变：HTTP 状态恒为 200，错误码放在 body 的 {@code code} 字段（前端已有拦截器依赖此行为）。
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 系统异常对外返回的错误码，沿用历史值 500，避免前端已有的判断逻辑失效 */
	private static final int SYSTEM_ERROR_CODE = 500;

	/** 排查号响应头：用户报错时带上它，可直接定位日志 */
	private static final String TRACE_HEADER = "X-Trace-Id";

	/** 排查号长度 */
	private static final int TRACE_ID_LENGTH = 12;

	/** 系统级错误码下界：达到该值才记业务异常日志，避免"未登录/验证码错误"这类正常拒绝刷屏 */
	private static final int SYSTEM_LEVEL_CODE = 40000;

	@ResponseBody
	@ExceptionHandler(Exception.class)
	public Object handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {

		// 业务异常：message 本身就是给用户看的（如"验证码错误"），原样返回
		if (e instanceof ServiceException serviceException) {
			ResultCode resultCode = serviceException.getResultCode();
			String message = StringUtils.hasText(serviceException.getMessage())
					? serviceException.getMessage()
					: resultCode.message();
			if (resultCode.code() >= SYSTEM_LEVEL_CODE) {
				Logger.warn("业务异常 code={} uri={} msg={}",
						resultCode.code(), request.getRequestURI(), message);
			}
			return Result.failure(resultCode.code(), message);
		}

		// 系统异常：堆栈只进日志，响应里只给排查号
		String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);
		response.setHeader(TRACE_HEADER, traceId);
		Logger.error("[{}] 未捕获异常 uri={} type={}",
				traceId, request.getRequestURI(), e.getClass().getName(), e);

		return Result.failure(SYSTEM_ERROR_CODE,
				ResultCode.SYSTEM_INNER_ERROR.message() + "（traceId: " + traceId + "）");
	}

}
