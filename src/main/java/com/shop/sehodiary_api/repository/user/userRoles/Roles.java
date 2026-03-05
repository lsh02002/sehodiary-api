package com.shop.sehodiary_api.repository.user.userRoles;

import com.shop.sehodiary_api.repository.activity.logger.Loggable;
import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
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
public class Roles extends BaseTimeEntity implements Loggable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roles_id")
    private Integer rolesId;

    @Column(name = "name",nullable = false)
    private String name;

    @Override
    public String logMessage() {
        return "name=";
    }
}
