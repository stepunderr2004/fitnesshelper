package com.example.fitnesshelper.helpers;

import android.content.Context;

public class SessionManager {
    private static final String PREFS_NAME = "FitnessPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_LAST_MODIFIED = "last_modified";

    public static void saveSession(Context context, String token, int userId, String role) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_TOKEN, token)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER_ROLE, role)
                .apply();
    }

    public static void saveRefreshToken(Context context, String refreshToken) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public static String getToken(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null);
    }

    public static String getRefreshToken(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_REFRESH_TOKEN, null);
    }

    public static int getUserId(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_USER_ID, -1);
    }

    public static String getUserRole(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USER_ROLE, "CLIENT");
    }

    public static void saveLastModified(Context context, long lastModified) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putLong(KEY_LAST_MODIFIED, lastModified).apply();
    }

    public static long getLastModified(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_LAST_MODIFIED, 0);
    }

    public static void clearSession(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }
}