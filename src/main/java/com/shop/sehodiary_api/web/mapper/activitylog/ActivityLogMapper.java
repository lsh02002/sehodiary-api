package com.shop.sehodiary_api.web.mapper.activitylog;

import com.shop.sehodiary_api.repository.activity.ActivityLog;
import com.shop.sehodiary_api.web.dto.activitylog.ActivityLogResponse;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogMapper {
    public ActivityLogResponse toResponse(ActivityLog activityLog) {
        return ActivityLogResponse.builder()
                .id(activityLog.getId())
                .message(activityLog.getMessage())
                .createdAt(activityLog.getCreatedAt().toString())
                .build();
    }
}
