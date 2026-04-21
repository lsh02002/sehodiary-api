package com.shop.sehodiary_api.web.controller.rss;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Description;
import com.rometools.rome.feed.rss.Item;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedOutput;
import com.shop.sehodiary_api.service.rss.FeedService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RssController {
    @Value("${rss.link-url}")
    private String rssLink;

    private final FeedService feedService;

    @GetMapping(value = "/rss.xml", produces = "application/rss+xml; charset=UTF-8")
    public ResponseEntity<String> rss() throws FeedException {
        Channel channel = new Channel("rss_2.0");
        channel.setTitle("세호 일기앱 RSS");
        channel.setLink(rssLink);
        channel.setDescription("세호 일기앱의 최신 글 목록");
        channel.setLanguage("ko");

        List<Item> items = feedService.getItems().stream()
                .map(feedItem -> {
                    Item item = new Item();
                    item.setTitle(feedItem.title());
                    item.setLink(feedItem.link());

                    Description desc = new Description();
                    desc.setType("text/plain");
                    desc.setValue(feedItem.description());
                    item.setDescription(desc);

                    item.setPubDate(
                            Timestamp.valueOf(feedItem.publishedAt())
                    );
                    return item;
                })
                .toList();

        channel.setItems(items);

        String xml = new WireFeedOutput().outputString(channel);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/rss+xml; charset=UTF-8")
                .body(xml);
    }
}
