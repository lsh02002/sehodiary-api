package com.shop.sehodiary_api.config.redis.redissearch;

import com.redis.om.spring.annotations.Query;
import com.redis.om.spring.repository.RedisDocumentRepository;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface DiarySearchRepository
        extends RedisDocumentRepository<DiarySearchDocument, String> {

    @Query("""
    SELECT p
    FROM DiarySearchDocument p
    WHERE p.userId = :userId
      AND p.visibility IN :visibilities
      AND (
            p.title LIKE %:keyword%
            OR p.content LIKE %:keyword%
      )
    ORDER BY p.createdAt DESC
""")
    Page<DiarySearchDocument> searchByKeyword(
            @Param("userId") Long userId,
            @Param("visibilities") Set<String> visibilities,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
