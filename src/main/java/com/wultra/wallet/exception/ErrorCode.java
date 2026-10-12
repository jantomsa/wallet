package com.wultra.wallet.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND),
    CURRENCY_MISMATCH(HttpStatus.BAD_REQUEST),
    UNSUPPORTED_CURRENCY(HttpStatus.BAD_REQUEST),
    INSUFFICIENT_FUNDS(HttpStatus.CONFLICT);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

}
