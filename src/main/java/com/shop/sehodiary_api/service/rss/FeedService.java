package com.shop.sehodiary_api.service.rss;

import com.shop.sehodiary_api.service.diary.DiaryService;
import com.shop.sehodiary_api.web.dto.rss.FeedItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {
    @Value("${rss.link-url}")
    private String rssLink;

    private final DiaryService diaryService;

    public List<FeedItem> getItems() {
        Pageable pageable = PageRequest.of(0, 20);

        return diaryService.getDiariesByPublic(null, pageable)
                .stream()
                .map(diaryResponse -> new FeedItem(
                        diaryResponse.getTitle(),
                         rssLink + "/edit/" + diaryResponse.getId(),
                         limit(diaryResponse.getContent()),
                        LocalDateTime.parse(diaryResponse.getCreatedAt())
                ))
                .toList();
    }

    private String limit(String content) {
        if (content == null) return "";

        int maxLength = 100;

        return content.length() <= maxLength
                ? content
                : content.replaceAll("<[^>]*>", "").substring(0, maxLength) + "...";
    }
}
