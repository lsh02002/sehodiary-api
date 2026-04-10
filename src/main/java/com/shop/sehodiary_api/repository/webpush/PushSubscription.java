package com.shop.sehodiary_api.repository.webpush;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class PushSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 1000, nullable = false)
    private String endpoint;

    @Column(length = 500, nullable = false)
    private String p256dh;

    @Column(length = 500, nullable = false)
    private String auth;

    private LocalDateTime createdAt = LocalDateTime.now();
}
