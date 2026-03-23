package com.shop.sehodiary_api;

import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.user.User;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public final class TestUserFactory {

    private static final AtomicLong COUNTER = new AtomicLong();

    public static User createUser() {
        long suffix = COUNTER.incrementAndGet();

        return User.builder()
                .email("test" + suffix + "@example.com")
                .password("encoded-password")
                .nickname("tester" + suffix)
                .userStatus("ACTIVE")
                .build();
    }

    public static Diary createDiary(User user) {
        long suffix = COUNTER.incrementAndGet();

        Diary diary = Diary.builder()
                .title("테스트 일기 " + suffix)
                .content("테스트 내용 " + suffix)
                .user(user)
                .build();

        // 🔥 양방향 관계 맞추기 (중요)
        user.addDiary(diary);

        return diary;
    }
}