package com.lhs.common.exception;

import com.lhs.common.enums.ResultCode;

/**
 * TCL 接口返回业务错误码时抛出的异常
 * <p>
 * 携带 TCL 侧原始错误码，供调用方按错误码分级处置：
 * 例如 90015（通道未开启）属预期状态不告警，90009（刷新令牌失效）需清兑换缓存自愈
 */
public class TclApiException extends ServiceException {

    /** TCL 侧原始错误码 */
    private final Integer tclCode;

    /**
     * 构造 TCL 业务异常
     *
     * @param tclCode  TCL 侧原始错误码
     * @param message 错误描述（对前端统一透出接口调用失败）
     */
    public TclApiException(Integer tclCode, String message) {
        super(ResultCode.INTERFACE_OUTER_INVOKE_ERROR, message);
        this.tclCode = tclCode;
    }

    public Integer getTclCode() {
        return tclCode;
    }
}
