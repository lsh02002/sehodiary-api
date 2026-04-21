package com.shop.sehodiary_api.web.controller.rss;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Description;
import com.rometools.rome.feed.rss.Item;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedOutput;
import com.shop.sehodiary_api.service.diary.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RssController {
    @Value("${rss.link-url}")
    private String rssLink;

    private final DiaryService diaryService;

    @GetMapping(value = "/rss.xml", produces = "application/rss+xml; charset=UTF-8")
    public ResponseEntity<String> rss() throws FeedException {

        Channel channel = new Channel("rss_2.0");
        channel.setTitle("세호 일기앱 RSS");
        channel.setLink(rssLink);
        channel.setDescription("세호 일기앱의 최신 글 목록");
        channel.setLanguage("ko");

        Pageable pageable = PageRequest.of(0, 20);

        List<Item> items = diaryService.getDiariesByPublic(null, pageable)
                .stream()
                .map(diaryResponse -> {
                    Item item = new Item();
                            item.setTitle(diaryResponse.getTitle());
                            item.setLink(rssLink + "/edit/" + diaryResponse.getId());

                            Description desc = new Description();
                            desc.setType("text/plain");
                            desc.setValue(limit(diaryResponse.getContent()));
                            item.setDescription(desc);
                            item.setPubDate(Timestamp.valueOf(LocalDateTime.parse(diaryResponse.getCreatedAt())));

                            return item;
                })
                .toList();

        channel.setItems(items);

        String xml = new WireFeedOutput().outputString(channel);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/rss+xml; charset=UTF-8")
                .body(xml);
    }

    private String limit(String content) {
        if (content == null) return "";

        int maxLength = 100;

        return content.length() <= maxLength
                ? content
                : content.replaceAll("<[^>]*>", "").substring(0, maxLength) + "...";
    }
}
