package com.shop.sehodiary_api.service.activelog;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.ActivityLog;
import com.shop.sehodiary_api.repository.activity.ActivityLogRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.web.dto.activitylog.ActivityLogResponse;
import com.shop.sehodiary_api.web.mapper.activitylog.ActivityLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogMapper activityLogMapper;

    @Transactional
    public void log(ActivityEntityType type, ActivityAction action, Long entityId, String message, User actor, Object beforeJson, Object afterJson) {

        if(Objects.equals(beforeJson, afterJson)) {
            throw new NotAcceptableException("변경된 사항이 없습니다.", null);
        }

        String logMessage = type.toString() + "에서 " + message + " 이(가) " + action.toString() + " 되었습니다.";

        ActivityLog log = new ActivityLog(type, action, entityId, logMessage, actor, beforeJson, afterJson);

        activityLogRepository.save(log);
    }

    @Transactional
    public List<ActivityLogResponse> getActivityLogsByUser(Long userId) {
        return activityLogRepository.findByActorIdAndEntityTypeNot(userId, ActivityEntityType.LIKE)
                .stream().map(activityLogMapper::toResponse).toList();
    }
}
