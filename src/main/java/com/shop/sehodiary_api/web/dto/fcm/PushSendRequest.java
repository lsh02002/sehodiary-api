package com.shop.sehodiary_api.web.dto.fcm;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record PushSendRequest(
        @NotBlank String token,
        @NotBlank String title,
        @NotBlank String body,
        Map<String, String> data
) {}
