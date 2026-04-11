package com.shop.sehodiary_api.service.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.shop.sehodiary_api.web.dto.fcm.PushSendRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FcmService {

    public String sendToToken(PushSendRequest request) throws Exception {
        Map<String, String> data = request.data() != null ? request.data() : new HashMap<>();

        Message message = Message.builder()
                .setToken(request.token())
                .setNotification(Notification.builder()
                        .setTitle(request.title())
                        .setBody(request.body())
                        .build())
                .putAllData(data)
                .build();

        return FirebaseMessaging.getInstance().send(message);
    }
}