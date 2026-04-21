package com.shop.sehodiary_api.web.dto.rss;

import java.time.LocalDateTime;

public record FeedItem(
        String title,
        String link,
        String description,
        LocalDateTime publishedAt
) {
}
