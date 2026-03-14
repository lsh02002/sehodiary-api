package com.shop.sehodiary_api.repository.diary;

import com.shop.sehodiary_api.repository.common.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Optional<Diary> findByUserIdAndId(Long userId, Long diaryId);
    void deleteByUserIdAndId(Long userId, Long diaryId);
    List<Diary> findByUserId(Long userId);
    List<Diary> findByVisibilityIn(List<Visibility> visibilities);
    @Query("select d from Diary d join fetch d.user")
    List<Diary> findAllWithUser();

    @Query("""
       select d.id
       from Diary d
       where d.visibility = 'PUBLIC'
       """)
    List<Long> findAllPublicIds();

    @Query("""
       select d.id
       from Diary d
       where d.visibility = 'FRIENDS'
       """)
    List<Long> findAllFriendsIds();
}
