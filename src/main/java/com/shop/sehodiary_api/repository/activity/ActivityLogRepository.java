package com.shop.sehodiary_api.repository.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByActorIdAndEntityTypeNot(Long actor_id, ActivityEntityType entityType);
}
