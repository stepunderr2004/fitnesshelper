package com.example.fitnesshelper.activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.db.AppDatabase;
import com.example.fitnesshelper.db.User;
import com.example.fitnesshelper.helpers.SessionManager;
import com.example.fitnesshelper.network.RetrofitClient;
import com.example.fitnesshelper.sync.SyncManager;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEditText, passwordEditText;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEditText = findViewById(R.id.loginEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        if (SessionManager.getToken(this) != null) {
            navigateByRole(SessionManager.getUserRole(this));
            return;
        }

        loginButton.setOnClickListener(v -> {
            String username = loginEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверка сети перед запросом
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "Нет подключения к сети", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("password", password);

            RetrofitClient.getInstance(this).getApiService().login(body)
                    .enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                String token = (String) response.body().get("token");
                                String refreshToken = (String) response.body().get("refreshToken");
                                int userId = ((Double) response.body().get("userId")).intValue();
                                String role = (String) response.body().get("role");
                                long lastModified = response.body().get("lastModified") != null ?
                                        ((Double) response.body().get("lastModified")).longValue() : 0L;

                                SessionManager.saveSession(LoginActivity.this, token, userId, role);
                                SessionManager.saveRefreshToken(LoginActivity.this, refreshToken);
                                SessionManager.saveLastModified(LoginActivity.this, lastModified);

                                new Thread(() -> {
                                    AppDatabase db = AppDatabase.getInstance(LoginActivity.this);
                                    User existing = db.userDao().getUserById(userId);
                                    if (existing == null) {
                                        User user = new User(username, "", "", "", "male", true);
                                        user.id = userId;
                                        db.userDao().insertUser(user);
                                    }
                                }).start();

                                FirebaseMessaging.getInstance().getToken()
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                String fcmToken = task.getResult();
                                                Map<String, String> tokenBody = new HashMap<>();
                                                tokenBody.put("fcmToken", fcmToken);
                                                RetrofitClient.getInstance(LoginActivity.this)
                                                        .getApiService()
                                                        .updateFcmToken(userId, tokenBody)
                                                        .enqueue(new Callback<Void>() {
                                                            @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                                                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                                                        });
                                            }
                                        });

                                if ("CLIENT".equals(role)) {
                                    SyncManager syncManager = new SyncManager(LoginActivity.this);
                                    syncManager.performFullSync(userId, success -> {
                                        runOnUiThread(() -> {
                                            if (success) {
                                                navigateByRole(role);
                                            } else {
                                                Toast.makeText(LoginActivity.this, "Ошибка синхронизации данных", Toast.LENGTH_SHORT).show();
                                                navigateByRole(role);
                                            }
                                        });
                                    });
                                } else {
                                    navigateByRole(role);
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            Toast.makeText(LoginActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void navigateByRole(String role) {
        Intent intent;
        if ("ADMIN".equals(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else if ("TRAINER".equals(role)) {
            intent = new Intent(this, TrainerActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}