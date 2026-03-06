package com.shop.sehodiary_api.web.dto.activitylog;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ActivityLogResponse {
    private Long id;
    private String message;
    private String createdAt;
}
