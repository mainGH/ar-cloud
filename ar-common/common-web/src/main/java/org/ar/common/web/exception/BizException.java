package org.ar.common.web.exception;

import lombok.Getter;
import org.ar.common.core.result.ResultCode;

@Getter
public class BizException extends RuntimeException {

    public ResultCode resultCode;

    public BizException(ResultCode errorCode) {
        super(errorCode.getMsg());
        this.resultCode = errorCode;
    }

    public BizException(String message) {
        super(message);
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }

    public BizException(Throwable cause) {
        super(cause);
    }
}
