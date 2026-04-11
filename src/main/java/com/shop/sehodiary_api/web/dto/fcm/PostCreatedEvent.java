package com.shop.sehodiary_api.web.dto.fcm;

public record PostCreatedEvent(
        Long postId,
        String authorId,
        String title,
        String content
) {}
