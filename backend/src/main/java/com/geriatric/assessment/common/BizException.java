package com.geriatric.assessment.common;

import lombok.Getter;

/**
 * 业务异常：由服务层抛出，全局异常处理器统一转换为 Result 返回。
 */
@Getter
public class BizException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(400, message);
    }
}
