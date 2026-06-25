package com.example.fitnesshelper.sync;

import android.content.Context;
import android.util.Log;
import com.example.fitnesshelper.activities.MainActivity;
import com.example.fitnesshelper.helpers.SessionManager;
import com.example.fitnesshelper.db.*;
import com.example.fitnesshelper.network.ApiService;
import com.example.fitnesshelper.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final AtomicBoolean isSyncing = new AtomicBoolean(false);

    private final AppDatabase db;
    private final ApiService api;
    private final Context context;

    public interface SyncCallback {
        void onComplete(boolean success);
    }

    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(context);
        this.api = RetrofitClient.getInstance(context).getApiService();
    }

    public static boolean isSyncInProgress() {
        return isSyncing.get();
    }

    public void performFullSync(int userId, SyncCallback callback) {
        if (!isSyncing.compareAndSet(false, true)) {
            Log.w(TAG, "Sync already in progress, ignoring new request");
            callback.onComplete(false);
            return;
        }
        Log.d(TAG, "Starting full sync for userId=" + userId);
        if (SessionManager.getToken(context) == null) {
            Log.w(TAG, "No token, aborting sync");
            isSyncing.set(false);
            callback.onComplete(false);
            return;
        }
        new Thread(() -> {
            boolean success = true;
            try {
                List<Program> programs = loadProgramsSafe(userId);
                List<TrainingDate> trainingDates = loadTrainingDatesSafe();
                List<DiaryEntry> diaryEntries = loadDiaryEntriesSafe();
                TrainingSession lastSession = loadTrainingSessionSafe();
                User user = loadUserSafe(userId);

                db.runInTransaction(() -> {
                    if (programs != null && !programs.isEmpty()) {
                        db.programDao().deleteAllForUser(userId);
                        for (Program program : programs) {
                            program.userId = userId;
                            long programId = db.programDao().insertProgramWithReturn(program);
                            for (TrainingDay day : program.days) {
                                day.programId = (int) programId;
                                long dayId = db.trainingDayDao().insertWithReturn(day);
                                for (Exercise ex : day.exercises) {
                                    ex.trainingDayId = (int) dayId;
                                    long exerciseId = db.exerciseDao().insertExerciseWithReturn(ex);
                                    for (ExerciseSet set : ex.sets) {
                                        set.exerciseId = (int) exerciseId;
                                        db.exerciseSetDao().insertSet(set);
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "Inserted " + programs.size() + " programs");
                    } else if (programs != null) {
                        Log.d(TAG, "Server returned empty programs, keeping old data");
                    }

                    if (trainingDates != null && !trainingDates.isEmpty()) {
                        db.trainingDateDao().deleteAllForUser(userId);
                        for (TrainingDate td : trainingDates) {
                            td.userId = userId;
                            db.trainingDateDao().insertDate(td);
                        }
                    }

                    if (diaryEntries != null && !diaryEntries.isEmpty()) {
                        db.diaryEntryDao().deleteAllForUser(userId);
                        for (DiaryEntry entry : diaryEntries) {
                            entry.userId = userId;
                            long entryId = db.diaryEntryDao().insert(entry);
                            for (DiaryExercise de : entry.exercises) {
                                de.diaryEntryId = (int) entryId;
                                long deId = db.diaryExerciseDao().insertWithReturn(de);
                                for (DiaryExerciseSet set : de.sets) {
                                    set.diaryExerciseId = (int) deId;
                                    db.diaryExerciseSetDao().insertSet(set);
                                }
                            }
                        }
                        Log.d(TAG, "Inserted " + diaryEntries.size() + " diary entries");
                    } else if (diaryEntries != null) {
                        Log.d(TAG, "Server returned empty diary, keeping old data");
                    }

                    if (lastSession != null) {
                        db.trainingSessionDao().deleteAllForUser(userId);
                        lastSession.userId = userId;
                        db.trainingSessionDao().insertSession(lastSession);
                    }
                });

                if (user != null) {
                    db.userDao().insertUser(user);
                    MainActivity.currentUser = db.userDao().getUserById(userId);
                }
                SessionManager.saveLastModified(context, System.currentTimeMillis());

                Log.d(TAG, "Full sync completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Sync failed", e);
                success = false;
            } finally {
                isSyncing.set(false);
                callback.onComplete(success);
            }
        }).start();
    }

    private List<Program> loadProgramsSafe(int userId) {
        try {
            Response<List<Program>> resp = api.getPrograms().execute();
            if (resp.isSuccessful() && resp.body() != null) {
                List<Program> result = new ArrayList<>();
                for (Program p : resp.body()) {
                    p.userId = userId;
                    p.days = loadTrainingDaysSafe(p.id);
                    result.add(p);
                }
                Log.d(TAG, "Fetched " + result.size() + " programs from server");
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load programs", e);
        }
        return null;
    }

    private List<TrainingDay> loadTrainingDaysSafe(int serverProgramId) {
        try {
            Response<List<TrainingDay>> resp = api.getTrainingDays(serverProgramId).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                List<TrainingDay> days = new ArrayList<>();
                for (TrainingDay day : resp.body()) {
                    day.programId = serverProgramId;
                    day.exercises = loadExercisesSafe(day.id);
                    days.add(day);
                }
                return days;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load training days for program " + serverProgramId, e);
        }
        return new ArrayList<>();
    }

    private List<Exercise> loadExercisesSafe(int serverDayId) {
        try {
            Response<List<Exercise>> resp = api.getExercises(serverDayId).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                List<Exercise> exercises = new ArrayList<>();
                for (Exercise ex : resp.body()) {
                    ex.trainingDayId = serverDayId;
                    ex.sets = loadExerciseSetsSafe(ex.id);
                    exercises.add(ex);
                }
                return exercises;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load exercises for day " + serverDayId, e);
        }
        return new ArrayList<>();
    }

    private List<ExerciseSet> loadExerciseSetsSafe(int serverExerciseId) {
        try {
            Response<List<ExerciseSet>> resp = api.getExerciseSets(serverExerciseId).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                return resp.body();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load sets for exercise " + serverExerciseId, e);
        }
        return new ArrayList<>();
    }

    private List<TrainingDate> loadTrainingDatesSafe() {
        try {
            Response<List<TrainingDate>> resp = api.getTrainingDates().execute();
            if (resp.isSuccessful() && resp.body() != null) {
                return resp.body();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load training dates", e);
        }
        return null;
    }

    private List<DiaryEntry> loadDiaryEntriesSafe() {
        try {
            Response<List<DiaryEntry>> resp = api.getDiaryEntries().execute();
            if (resp.isSuccessful() && resp.body() != null) {
                List<DiaryEntry> entries = new ArrayList<>();
                for (DiaryEntry entry : resp.body()) {
                    entry.exercises = loadDiaryExercisesSafe(entry.id);
                    entries.add(entry);
                }
                Log.d(TAG, "Fetched " + entries.size() + " diary entries from server");
                return entries;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load diary entries", e);
        }
        return null;
    }

    private List<DiaryExercise> loadDiaryExercisesSafe(int serverEntryId) {
        try {
            Response<List<DiaryExercise>> resp = api.getDiaryExercises(serverEntryId).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                List<DiaryExercise> exercises = new ArrayList<>();
                for (DiaryExercise de : resp.body()) {
                    de.diaryEntryId = serverEntryId;
                    de.sets = loadDiaryExerciseSetsSafe(de.id);
                    exercises.add(de);
                }
                return exercises;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load diary exercises for entry " + serverEntryId, e);
        }
        return new ArrayList<>();
    }

    private List<DiaryExerciseSet> loadDiaryExerciseSetsSafe(int serverExerciseId) {
        try {
            Response<List<DiaryExerciseSet>> resp = api.getDiaryExerciseSets(serverExerciseId).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                return resp.body();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load diary sets for exercise " + serverExerciseId, e);
        }
        return new ArrayList<>();
    }

    private TrainingSession loadTrainingSessionSafe() {
        try {
            Response<ResponseBody> resp = api.getLastTrainingSessionAsBody().execute();
            if (resp.isSuccessful() && resp.body() != null && resp.body().contentLength() > 0) {
                // Парсим вручную через Gson
                String json = resp.body().string();
                if (!json.isEmpty()) {
                    return new com.google.gson.Gson().fromJson(json, TrainingSession.class);
                }
            }
        } catch (Exception e) {
            // Пустое тело или ошибка – не критично, пропускаем
        }
        return null;
    }

    private User loadUserSafe(int userId) {
        try {
            Response<Map<String, Object>> resp = api.getUser(userId).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                return User.fromMap(resp.body());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load user " + userId, e);
        }
        return null;
    }
}