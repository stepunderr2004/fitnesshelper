package com.example.fitnesshelper.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public void sendSyncNotification(String fcmToken) {
        if (fcmToken == null || fcmToken.isEmpty()) return;
        Message message = Message.builder()
                .setToken(fcmToken)
                .putData("type", "sync")
                .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                .build();
        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            System.err.println("Failed to send FCM message: " + e.getMessage());
        }
    }
}