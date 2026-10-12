package com.wultra.wallet.exception;

public class WultraWalletException extends RuntimeException {

    private final ErrorCode errorCode;

    public WultraWalletException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
