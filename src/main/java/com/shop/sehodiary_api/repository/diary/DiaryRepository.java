package com.shop.sehodiary_api.repository.diary;

import com.shop.sehodiary_api.repository.common.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Optional<Diary> findByUserIdAndId(Long userId, Long diaryId);
    void deleteByUserIdAndId(Long userId, Long diaryId);

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

    @Query("select d.id from Diary d where d.user.id = :userId")
    List<Long> findIdsByUserId(@Param("userId") Long userId);
}
