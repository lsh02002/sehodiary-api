package com.shop.sehodiary_api.repository.user.userRoles;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@Table(name = "roles")
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class Roles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roles_id")
    private Integer rolesId;

    @Column(name = "name",nullable = false)
    private String name;
}
