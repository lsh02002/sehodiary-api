package com.shop.sehodiary_api.service.exceptions;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException{
    private final String detailMessage;
    private final Object request;

    public ConflictException(String detailMessage, Object request) {
        this.detailMessage = detailMessage;
        this.request = request;
    }
}
