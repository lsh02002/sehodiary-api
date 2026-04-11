package com.shop.sehodiary_api.web.controller.fcm;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.fcm.FcmService;
import com.shop.sehodiary_api.web.dto.fcm.PushSendRequest;
import com.shop.sehodiary_api.web.dto.fcm.PushSendToUserRequest;
import com.shop.sehodiary_api.web.dto.fcm.TokenRegisterRequest;
import com.shop.sehodiary_api.web.mapper.fcm.TokenStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fcm")
@CrossOrigin(origins = "*")
public class FcmController {

    private final TokenStore tokenStore;
    private final FcmService fcmService;

    public FcmController(TokenStore tokenStore, FcmService fcmService) {
        this.tokenStore = tokenStore;
        this.fcmService = fcmService;
    }

    @PostMapping("/register-token")
    public ResponseEntity<?> registerToken(@AuthenticationPrincipal CustomUserDetails customUserDetails, @Valid @RequestBody TokenRegisterRequest request) {
        tokenStore.save(customUserDetails.getId().toString(), request.token());
        return ResponseEntity.ok(Map.of("message", "token saved"));
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@Valid @RequestBody PushSendRequest request) throws Exception {
        String messageId = fcmService.sendToToken(request);
        return ResponseEntity.ok(Map.of("messageId", messageId));
    }

    @PostMapping("/send-to-user")
    public ResponseEntity<?> sendToUser(@Valid @RequestBody PushSendToUserRequest request) throws Exception {
        String token = tokenStore.findByUserId(request.userId());
        if (token == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "token not found"));
        }

        String messageId = fcmService.sendToToken(
                new PushSendRequest(
                        token,
                        request.title(),
                        request.body(),
                        request.data()
                )
        );

        return ResponseEntity.ok(Map.of("messageId", messageId));
    }
}
