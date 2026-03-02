package com.shop.sehodiary_api.service.exceptions;

import lombok.Getter;

@Getter
public class CustomBadCredentialsException extends RuntimeException{
    private final String detailMessage;
    private final Object request;

    public CustomBadCredentialsException(String detailMessage, Object request) {
        this.detailMessage = detailMessage;
        this.request = request;
    }
}
