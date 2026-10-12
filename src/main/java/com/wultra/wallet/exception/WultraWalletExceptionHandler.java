package com.wultra.wallet.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WultraWalletExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorDetail> customException(WultraWalletException exception) {
        ErrorCode code = exception.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(new ErrorDetail(code, exception.getMessage()));
    }
}
