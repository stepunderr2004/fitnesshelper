package com.example.fitnesshelper.services;

import android.util.Log;

import com.example.fitnesshelper.helpers.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.example.fitnesshelper.network.RetrofitClient;
import com.example.fitnesshelper.sync.SyncManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        int userId = SessionManager.getUserId(this);
        if (userId != -1) {
            Map<String, String> body = new HashMap<>();
            body.put("fcmToken", token);
            RetrofitClient.getInstance(this).getApiService()
                    .updateFcmToken(userId, body)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            Log.d(TAG, "FCM token saved");
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e(TAG, "Failed to save FCM token", t);
                        }
                    });
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String type = remoteMessage.getData().get("type");
            if ("sync".equals(type)) {
                Log.d(TAG, "Sync notification received");
                if (SyncManager.isSyncInProgress()) {
                    Log.d(TAG, "Sync already in progress, ignoring FCM trigger");
                    return;
                }
                int userId = SessionManager.getUserId(this);
                if (userId != -1) {
                    new SyncManager(this).performFullSync(userId, success -> {
                        if (success) {
                            Log.d(TAG, "Sync completed after FCM");
                        } else {
                            Log.w(TAG, "Sync after FCM failed or was skipped");
                        }
                    });
                }
            }
        }
    }
}