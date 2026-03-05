package com.shop.sehodiary_api.repository.user.userRoles;

import com.shop.sehodiary_api.repository.activity.logger.Loggable;
import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "user_roles")
@Getter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class UserRoles extends BaseTimeEntity implements Loggable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_roles_id")
    private Integer userRolesId;
    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "role_id",nullable = false)
    private Roles roles;

    @Override
    public String logMessage() {
        return "name=";
    }
}
