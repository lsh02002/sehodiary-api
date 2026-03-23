package com.shop.sehodiary_api.repository.diaryImage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiaryImageRepository extends JpaRepository<DiaryImage, Long> {
    @Query("""
        select d
        from DiaryImage d
        where d.id = :id
          and (d.deleted <> :deleted or d.deleted is null)
    """)
    Optional<DiaryImage> findByIdAndDeletedNot(@Param("id") Long id,
                                               @Param("deleted") Boolean deleted);
}
