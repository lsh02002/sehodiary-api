package com.shop.sehodiary_api.config.redis.redissearch;

import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DiarySearchRepository
        extends RedisDocumentRepository<DiarySearchDocument, String> {

    List<DiarySearchDocument> searchByTitleOrContent(
            String title,
            String content
    );

    Page<DiarySearchDocument>
    findByUserIdAndTitleContainingOrUserIdAndContentContaining(
            Long userId1,
            String title,
            Long userId2,
            String content,
            Pageable pageable
    );

    Page<DiarySearchDocument>
    findByUserIdAndVisibilityAndTitleContainingOrUserIdAndVisibilityAndContentContaining(
            Long userId1,
            String visibility1,
            String title,
            Long userId2,
            String visibility2,
            String content,
            Pageable pageable
    );

    Page<DiarySearchDocument>
    findByUserIdAndVisibilityInAndTitleContainingOrUserIdAndVisibilityInAndContentContaining(
            Long userId1,
            List<String> visibility1,
            String title,
            Long userId2,
            List<String> visibility2,
            String content,
            Pageable pageable
    );
}
