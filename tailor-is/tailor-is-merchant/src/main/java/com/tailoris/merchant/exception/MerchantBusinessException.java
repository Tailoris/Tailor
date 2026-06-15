package com.tailoris.merchant.exception;

import com.tailoris.common.exception.BusinessException;
import lombok.Getter;

/**
 * 商家业务异常 - MER.
 *
 * <p>商家服务所有业务异常统一通过此类抛出，便于上层统一处理。</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Getter
public class MerchantBusinessException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final String errorCode;

    public MerchantBusinessException(String message) {
        super(message);
        this.errorCode = "MERCHANT_ERROR";
    }

    public MerchantBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MerchantBusinessException(String message, Throwable cause) {
        super(message);
        initCause(cause);
        this.errorCode = "MERCHANT_ERROR";
    }

    public MerchantBusinessException(String errorCode, String message, Throwable cause) {
        super(message);
        initCause(cause);
        this.errorCode = errorCode;
    }
}
