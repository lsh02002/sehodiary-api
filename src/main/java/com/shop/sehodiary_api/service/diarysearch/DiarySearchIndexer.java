package com.shop.sehodiary_api.service.diarysearch;

import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.config.redis.diarysearch.DiarySearchDocument;
import com.shop.sehodiary_api.config.redis.diarysearch.DiarySearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class DiarySearchIndexer {

    private final DiarySearchRepository diarySearchRepository;

    public void index(Diary diary) {
        if (diary.getVisibility() != Visibility.PUBLIC) {
            delete(diary.getId());
            return;
        }

        DiarySearchDocument document = DiarySearchDocument.builder()
                .id(String.valueOf(diary.getId()))
                .diaryId(diary.getId())
                .userId(diary.getUser().getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .visibility(diary.getVisibility().name())
                .createdAt(diary.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli())
                .build();

        diarySearchRepository.save(document);
    }

    public void delete(Long diaryId) {
        diarySearchRepository.deleteById(String.valueOf(diaryId));
    }
}