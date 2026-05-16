package com.shop.sehodiary_api.service.diarysearch;

import com.shop.sehodiary_api.config.redis.redissearch.DiarySearchDocument;
import com.shop.sehodiary_api.config.redis.redissearch.DiarySearchRepository;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiarySearchIndexer {

    private final DiaryRepository diaryRepository;
    private final DiarySearchRepository diarySearchRepository;

    @Transactional
    public void rebuildDiarySearchDocuments() {
        List<Diary> diaries = diaryRepository.findAll();

        List<DiarySearchDocument> documents = diaries.stream()
                .map(DiarySearchDocument::from)
                .toList();

        diarySearchRepository.saveAll(documents);
    }

    public void index(Diary diary) {
        if (diary.getVisibility() == Visibility.PRIVATE) {
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
                .createdAt(diary.getCreatedAt())
                .build();

        diarySearchRepository.save(document);
    }

    public void delete(Long diaryId) {
        diarySearchRepository.deleteById(String.valueOf(diaryId));
    }
}
