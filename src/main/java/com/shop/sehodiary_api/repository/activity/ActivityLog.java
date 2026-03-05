package com.shop.sehodiary_api.repository.activity;

import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
import com.shop.sehodiary_api.repository.user.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityLog extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 16, nullable = false)
    private ActivityEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ActivityAction action;

    @Type(JsonType.class)
    @Column(name = "before_json", columnDefinition = "JSON")
    private Object beforeJson;

    @Type(JsonType.class)
    @Column(name = "after_json", columnDefinition = "JSON")
    private Object afterJson;

    public ActivityLog(ActivityEntityType type, ActivityAction action, Long entityId, String message, User actor, Object beforeJson, Object afterJson) {
        this.entityType = type;
        this.action = action;
        this.entityId = entityId;
        this.message = message;
        this.actor = actor;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
    }
}
