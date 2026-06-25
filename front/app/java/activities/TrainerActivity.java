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
import com.example.fitnesshelper.fragments.trainer.ClientsFragment;
import com.example.fitnesshelper.fragments.trainer.ClientSelectionFragment;
import com.example.fitnesshelper.fragments.trainer.TrainerProfileFragment;
import com.example.fitnesshelper.fragments.trainer.TrainerScheduleFragment;
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

public class TrainerActivity extends AppCompatActivity implements RetrofitClient.OnServerErrorListener {

    private BottomNavigationView bottomNav;
    private ProgressBar loadingProgress;
    public static volatile User currentUser;
    private SyncManager syncManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int syncRetryCount = 0;
    private static final int MAX_RETRY = 1;

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer);

        loadingProgress = findViewById(R.id.loadingProgress);
        bottomNav = findViewById(R.id.bottomNavigationView);

        RetrofitClient.setOnServerErrorListener(this);

        // Инициализация отслеживания сети
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    SessionManager.clearSession(TrainerActivity.this);
                    RetrofitClient.resetInstance();
                    Intent intent = new Intent(TrainerActivity.this, LoginActivity.class);
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

        int userId = SessionManager.getUserId(this);
        syncManager = new SyncManager(this);

        new Thread(() -> {
            currentUser = AppDatabase.getInstance(this).userDao().getUserById(userId);
            if (currentUser == null) {
                try {
                    Response<Map<String, Object>> resp = RetrofitClient.getInstance(this)
                            .getApiService().getUser(userId).execute();
                    if (resp.isSuccessful() && resp.body() != null) {
                        currentUser = User.fromMap(resp.body());
                        AppDatabase.getInstance(this).userDao().insertUser(currentUser);
                    }
                } catch (Exception ignored) {}
            }

            runOnUiThread(() -> {
                loadingProgress.setVisibility(View.GONE);
                bottomNav.setVisibility(View.VISIBLE);

                if (savedInstanceState == null) {
                    bottomNav.setSelectedItemId(R.id.navigation_schedule);
                    loadFragment(new TrainerScheduleFragment());
                }

                if (!SyncManager.isSyncInProgress()) {
                    performStartupSync();
                }

                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Map<String, String> body = new HashMap<>();
                        body.put("fcmToken", token);
                        RetrofitClient.getInstance(TrainerActivity.this)
                                .getApiService()
                                .updateFcmToken(userId, body)
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

            if (id == R.id.navigation_schedule) {
                selectedFragment = new TrainerScheduleFragment();
            } else if (id == R.id.navigation_programs_trainer) {
                selectedFragment = ClientSelectionFragment.newInstance("programs");
            } else if (id == R.id.navigation_clients) {
                selectedFragment = new ClientsFragment();
            } else if (id == R.id.navigation_diary_trainer) {
                selectedFragment = ClientSelectionFragment.newInstance("diary");
            } else if (id == R.id.navigation_profile_trainer) {
                selectedFragment = new TrainerProfileFragment();
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
        getSupportFragmentManager().beginTransaction()
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
        int userId = SessionManager.getUserId(this);
        syncManager.performFullSync(userId, success -> {
            if (success) {
                syncRetryCount = 0;
                new Thread(() -> {
                    currentUser = AppDatabase.getInstance(TrainerActivity.this).userDao().getUserById(userId);
                }).start();
                runOnUiThread(() -> {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof TrainerProfileFragment) {
                        getSupportFragmentManager().beginTransaction()
                                .detach(currentFragment)
                                .attach(currentFragment)
                                .commit();
                    }
                });
            } else {
                if (syncRetryCount < MAX_RETRY) {
                    syncRetryCount++;
                    runOnUiThread(() ->
                            Toast.makeText(TrainerActivity.this, "Сервер временно недоступен, пробую снова…", Toast.LENGTH_SHORT).show()
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