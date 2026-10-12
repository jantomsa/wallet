package com.wultra.wallet.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorDetail {

    private ErrorCode code;
    private String message;

}
