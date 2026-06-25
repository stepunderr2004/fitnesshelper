package com.example.fitnesshelper.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.fitnesshelper.activities.LoginActivity;
import com.example.fitnesshelper.helpers.SessionManager;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    private static final String BASE_URL = "http://93.88.203.138/";

    private static volatile RetrofitClient instance;
    private ApiService apiService;
    private final OkHttpClient okHttpClient;
    private static final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    public interface OnServerErrorListener {
        void onServerError(String message);
    }

    private static OnServerErrorListener serverErrorListener;

    public static void setOnServerErrorListener(OnServerErrorListener listener) {
        serverErrorListener = listener;
    }

    public static void removeOnServerErrorListener() {
        serverErrorListener = null;
    }

    private RetrofitClient(Context context) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            String token = SessionManager.getToken(context.getApplicationContext());
            if (token != null) {
                Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(request);
            }
            return chain.proceed(original);
        };

        Interceptor serverErrorInterceptor = chain -> {
            okhttp3.Response response = chain.proceed(chain.request());
            if (response.code() == 502 || response.code() == 503) {
                if (serverErrorListener != null) {
                    serverErrorListener.onServerError("Сервер временно недоступен");
                }
            }
            return response;
        };

        Authenticator authenticator = (route, response) -> {
            Log.d(TAG, "Authenticator called with code: " + response.code());
            if (response.code() == 401 || response.code() == 403) {
                if (!refreshInProgress.compareAndSet(false, true)) {
                    Log.d(TAG, "Refresh already in progress");
                    return null;
                }
                try {
                    String refreshToken = SessionManager.getRefreshToken(context.getApplicationContext());
                    Log.d(TAG, "Refresh token exists: " + (refreshToken != null));
                    if (refreshToken == null) {
                        redirectToLogin(context.getApplicationContext());
                        return null;
                    }

                    OkHttpClient tempClient = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.SECONDS)
                            .build();

                    Retrofit tempRetrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(tempClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    ApiService tempApi = tempRetrofit.create(ApiService.class);

                    retrofit2.Response<Map<String, Object>> refreshResponse =
                            tempApi.refresh(Map.of("refreshToken", refreshToken)).execute();

                    Log.d(TAG, "Refresh response code: " + refreshResponse.code());
                    if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                        String newAccessToken = (String) refreshResponse.body().get("token");
                        String newRefreshToken = (String) refreshResponse.body().get("refreshToken");

                        SessionManager.saveSession(context.getApplicationContext(),
                                newAccessToken,
                                SessionManager.getUserId(context.getApplicationContext()),
                                SessionManager.getUserRole(context.getApplicationContext()));
                        SessionManager.saveRefreshToken(context.getApplicationContext(), newRefreshToken);
                        Log.d(TAG, "Token refreshed successfully");

                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + newAccessToken)
                                .build();
                    } else {
                        Log.e(TAG, "Refresh failed");
                        redirectToLogin(context.getApplicationContext());
                        return null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Refresh error", e);
                    redirectToLogin(context.getApplicationContext());
                    return null;
                } finally {
                    refreshInProgress.set(false);
                }
            }
            return null;
        };

        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(serverErrorInterceptor)
                .addInterceptor(logging)
                .authenticator(authenticator)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    private void redirectToLogin(Context context) {
        Log.d(TAG, "Redirecting to login, clearing session");
        SessionManager.clearSession(context);
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public ApiService getApiService() {
        return apiService;
    }
}