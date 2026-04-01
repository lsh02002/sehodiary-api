package com.shop.sehodiary_api.repository.follow;

import com.shop.sehodiary_api.repository.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowerId(Long followerId);   // 내가 팔로우한 수
    long countByFollowingId(Long followingId); // 나를 팔로우한 수

    @EntityGraph(attributePaths = {"following", "following.profileImages"})
    List<Follow> findAllByFollowerIdOrderByIdDesc(Long followerId);

    @EntityGraph(attributePaths = {"follower", "follower.profileImages"})
    List<Follow> findAllByFollowingIdOrderByIdDesc(Long followingId);

    @Query("""
        select case when count(f) > 0 then true else false end
        from Follow f
        where f.follower.id = :followerId and f.following.id = :followingId
    """)
    boolean isFollowing(@Param("followerId") Long followerId,
                        @Param("followingId") Long followingId);

    @Query("""
    SELECT u
    FROM User u
    WHERE u.id != :userId
    AND u.id NOT IN (
        SELECT f.following.id
        FROM Follow f
        WHERE f.follower.id = :userId
    )
""")
    List<User> findUnfollowedUsers(@Param("userId") Long userId);
}