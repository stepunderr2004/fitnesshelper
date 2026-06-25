package com.example.fitnesshelper.activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.db.AppDatabase;
import com.example.fitnesshelper.db.User;
import com.example.fitnesshelper.fragments.CalendarFragment;
import com.example.fitnesshelper.fragments.MyTrainingFragment;
import com.example.fitnesshelper.fragments.ProfileFragment;
import com.example.fitnesshelper.fragments.ProgramsFragment;
import com.example.fitnesshelper.fragments.StatsFragment;
import com.example.fitnesshelper.fragments.SyncErrorDialogFragment;
import com.example.fitnesshelper.helpers.SessionManager;
import com.example.fitnesshelper.network.RetrofitClient;
import com.example.fitnesshelper.sync.SyncManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements RetrofitClient.OnServerErrorListener {

    private BottomNavigationView bottomNav;
    private ProgressBar loadingProgress;
    public static volatile int currentUserId = -1;
    public static volatile User currentUser = null;
    private SyncManager syncManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int syncRetryCount = 0;
    private static final int MAX_RETRY = 1;

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadingProgress = findViewById(R.id.loadingProgress);
        bottomNav = findViewById(R.id.bottomNavigationView);

        RetrofitClient.setOnServerErrorListener(this);

        // Инициализация отслеживания сети
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    // Предотвращаем множественные переходы
                    if (isFinishing() || isDestroyed()) return;
                    SessionManager.clearSession(MainActivity.this);
                    RetrofitClient.resetInstance();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        };

        if (!isNetworkAvailable()) {
            new AlertDialog.Builder(this)
                    .setTitle("Нет сети")
                    .setMessage("Для работы приложения требуется подключение к интернету.")
                    .setCancelable(false)
                    .setPositiveButton("Выход", (dialog, which) -> finish())
                    .show();
            return;
        }

        currentUserId = SessionManager.getUserId(this);
        if (currentUserId == -1) {
            SessionManager.clearSession(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        syncManager = new SyncManager(this);

        new Thread(() -> {
            currentUser = AppDatabase.getInstance(this).userDao().getUserById(currentUserId);
            if (currentUser == null) {
                try {
                    Response<Map<String, Object>> response = RetrofitClient.getInstance(this)
                            .getApiService().getUser(currentUserId).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        currentUser = User.fromMap(response.body());
                        AppDatabase.getInstance(this).userDao().insertUser(currentUser);
                    }
                } catch (Exception ignored) {}
            }
            runOnUiThread(() -> {
                loadingProgress.setVisibility(View.GONE);
                bottomNav.setVisibility(View.VISIBLE);
                if (savedInstanceState == null) {
                    loadFragment(new CalendarFragment());
                }
                performStartupSync();

                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String token = task.getResult();
                                Map<String, String> body = new HashMap<>();
                                body.put("fcmToken", token);
                                RetrofitClient.getInstance(MainActivity.this)
                                        .getApiService()
                                        .updateFcmToken(currentUserId, body)
                                        .enqueue(new Callback<Void>() {
                                            @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                                        });
                            }
                        });
            });
        }).start();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.navigation_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (id == R.id.navigation_programs) {
                selectedFragment = new ProgramsFragment();
            } else if (id == R.id.navigation_training) {
                selectedFragment = new MyTrainingFragment();
            } else if (id == R.id.navigation_stats) {
                selectedFragment = new StatsFragment();
            } else if (id == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void performStartupSync() {
        syncRetryCount = 0;
        doSync();
    }

    private void doSync() {
        syncManager.performFullSync(currentUserId, success -> {
            if (success) {
                syncRetryCount = 0;
                new Thread(() -> {
                    MainActivity.currentUser = AppDatabase.getInstance(MainActivity.this).userDao().getUserById(currentUserId);
                }).start();
                runOnUiThread(() -> {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (current != null) {
                        getSupportFragmentManager().beginTransaction()
                                .detach(current)
                                .attach(current)
                                .commit();
                    }
                });
            } else {
                if (syncRetryCount < MAX_RETRY) {
                    syncRetryCount++;
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Сервер временно недоступен, пробую снова…", Toast.LENGTH_SHORT).show()
                    );
                    handler.postDelayed(this::doSync, 3000);
                } else {
                    runOnUiThread(() -> {
                        SyncErrorDialogFragment dialog = SyncErrorDialogFragment.newInstance(
                                "Не удалось синхронизироваться. Проверьте подключение к интернету и попробуйте снова."
                        );
                        dialog.show(getSupportFragmentManager(), "sync_error_dialog");
                    });
                }
            }
        });
    }

    @Override
    public void onServerError(String message) {
        runOnUiThread(() -> {
            SyncErrorDialogFragment dialog = SyncErrorDialogFragment.newInstance(message);
            Fragment prev = getSupportFragmentManager().findFragmentByTag("server_error_dialog");
            if (prev != null) {
                ((SyncErrorDialogFragment) prev).dismiss();
            }
            dialog.show(getSupportFragmentManager(), "server_error_dialog");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RetrofitClient.removeOnServerErrorListener();
    }
}