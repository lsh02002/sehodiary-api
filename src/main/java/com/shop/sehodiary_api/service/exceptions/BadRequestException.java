package com.shop.sehodiary_api.service.exceptions;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException{
    private final String detailMessage;
    private final Object request;

    public BadRequestException(String detailMessage, Object request) {
        this.detailMessage = detailMessage;
        this.request = request;
    }
}
