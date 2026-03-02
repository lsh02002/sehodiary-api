package com.shop.sehodiary_api.web.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private int code;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;
}
