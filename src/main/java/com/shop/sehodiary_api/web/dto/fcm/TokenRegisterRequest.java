package com.shop.sehodiary_api.web.dto.fcm;

import jakarta.validation.constraints.NotBlank;

public record TokenRegisterRequest(
        @NotBlank String token,
        @NotBlank String deviceId
) {}
