package com.example.fitnesshelper.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.db.*;
import com.example.fitnesshelper.network.RetrofitClient;
import com.example.fitnesshelper.activities.MainActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyTrainingFragment extends Fragment {

    private enum State { IDLE, RUNNING, PAUSED }

    static class ExerciseData {
        String name;
        String muscleGroup;
        List<SetData> sets = new ArrayList<>();
    }
    static class SetData {
        int reps;
        float weight;
        SetData(int reps, float weight) {
            this.reps = reps;
            this.weight = weight;
        }
    }

    private Button startButton, pauseButton, resumeButton, finishButton;
    private TextView timerText;
    private LinearLayout sourceSelectionLayout;
    private Button myProgramButton, myTrainingButton;
    private LinearLayout exerciseLayout;
    private TextView currentExerciseName, currentExerciseDetails;
    private Button prevExerciseButton, nextExerciseButton;
    private LinearLayout allExercisesList;

    private State currentState = State.IDLE;
    private List<ExerciseData> exerciseDataList = new ArrayList<>();
    private int currentExerciseIndex = -1;
    private long startTime, pauseStart;
    private Handler handler = new Handler();
    private Runnable timerRunnable;

    private volatile int currentDiaryEntryId = -1;
    private final AtomicBoolean savingExercises = new AtomicBoolean(false);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_training, container, false);

        startButton = view.findViewById(R.id.startButton);
        pauseButton = view.findViewById(R.id.pauseButton);
        resumeButton = view.findViewById(R.id.resumeButton);
        finishButton = view.findViewById(R.id.finishButton);
        timerText = view.findViewById(R.id.timerText);
        sourceSelectionLayout = view.findViewById(R.id.sourceSelectionLayout);
        myProgramButton = view.findViewById(R.id.myProgramButton);
        myTrainingButton = view.findViewById(R.id.myTrainingButton);
        exerciseLayout = view.findViewById(R.id.exerciseLayout);
        currentExerciseName = view.findViewById(R.id.currentExerciseName);
        currentExerciseDetails = view.findViewById(R.id.currentExerciseDetails);
        prevExerciseButton = view.findViewById(R.id.prevExerciseButton);
        nextExerciseButton = view.findViewById(R.id.nextExerciseButton);
        allExercisesList = view.findViewById(R.id.allExercisesList);

        updateUIForState(State.IDLE);

        startButton.setOnClickListener(v -> {
            if (exerciseDataList.isEmpty()) {
                Toast.makeText(getContext(), "Сначала выберите программу или тренировку", Toast.LENGTH_SHORT).show();
                return;
            }
            startTraining();
        });
        pauseButton.setOnClickListener(v -> pauseTraining());
        resumeButton.setOnClickListener(v -> resumeTraining());
        finishButton.setOnClickListener(v -> stopTraining(true));

        myProgramButton.setOnClickListener(v -> selectProgramSource());
        myTrainingButton.setOnClickListener(v -> selectDiarySource());

        prevExerciseButton.setOnClickListener(v -> navigateExercise(-1));
        nextExerciseButton.setOnClickListener(v -> navigateExercise(1));

        return view;
    }

    private void updateUIForState(State state) {
        currentState = state;
        startButton.setVisibility(state == State.IDLE ? View.VISIBLE : View.GONE);
        pauseButton.setVisibility(state == State.RUNNING ? View.VISIBLE : View.GONE);
        resumeButton.setVisibility(state == State.PAUSED ? View.VISIBLE : View.GONE);
        finishButton.setVisibility(state == State.PAUSED ? View.VISIBLE : View.GONE);
        if (state == State.IDLE) timerText.setText(R.string.zero_time);
    }

    private void selectProgramSource() {
        RetrofitClient.getInstance(getContext()).getApiService().getPrograms()
                .enqueue(new Callback<List<Program>>() {
                    @Override
                    public void onResponse(Call<List<Program>> call, Response<List<Program>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Program> programs = response.body();
                            if (programs.isEmpty()) {
                                Toast.makeText(getContext(), "Нет созданных программ", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String[] names = new String[programs.size()];
                            for (int i = 0; i < programs.size(); i++) names[i] = programs.get(i).name;
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Выберите программу")
                                    .setItems(names, (dialog, which) -> selectTrainingDay(programs.get(which).id))
                                    .show();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Program>> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void selectTrainingDay(int programId) {
        RetrofitClient.getInstance(getContext()).getApiService().getTrainingDays(programId)
                .enqueue(new Callback<List<TrainingDay>>() {
                    @Override
                    public void onResponse(Call<List<TrainingDay>> call, Response<List<TrainingDay>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<TrainingDay> days = response.body();
                            if (days.isEmpty()) {
                                Toast.makeText(getContext(), "В программе нет дней", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String[] dayNames = new String[days.size()];
                            for (int i = 0; i < days.size(); i++) dayNames[i] = days.get(i).name;
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Выберите день")
                                    .setItems(dayNames, (dialog, which) -> loadExercisesFromDay(days.get(which).id))
                                    .show();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<TrainingDay>> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadExercisesFromDay(int dayId) {
        RetrofitClient.getInstance(getContext()).getApiService().getExercises(dayId)
                .enqueue(new Callback<List<Exercise>>() {
                    @Override
                    public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Exercise> exercises = response.body();
                            exerciseDataList.clear();
                            for (Exercise ex : exercises) {
                                ExerciseData data = new ExerciseData();
                                data.name = ex.name;
                                data.muscleGroup = ex.muscleGroup;
                                loadSetsForExerciseData(ex.id, data, () -> {
                                    if (exerciseDataList.size() == exercises.size()) {
                                        startButton.setEnabled(true);
                                        Toast.makeText(getContext(), "Программа выбрана", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(getContext(), "Нет упражнений", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Exercise>> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSetsForExerciseData(int exerciseId, ExerciseData data, Runnable onComplete) {
        RetrofitClient.getInstance(getContext()).getApiService().getExerciseSets(exerciseId)
                .enqueue(new Callback<List<ExerciseSet>>() {
                    @Override
                    public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (ExerciseSet es : response.body()) {
                                data.sets.add(new SetData(es.reps, es.weight));
                            }
                        }
                        exerciseDataList.add(data);
                        onComplete.run();
                    }
                    @Override
                    public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {
                        exerciseDataList.add(data);
                        onComplete.run();
                    }
                });
    }

    private void selectDiarySource() {
        RetrofitClient.getInstance(getContext()).getApiService().getDiaryEntries()
                .enqueue(new Callback<List<DiaryEntry>>() {
                    @Override
                    public void onResponse(Call<List<DiaryEntry>> call, Response<List<DiaryEntry>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<DiaryEntry> entries = response.body();
                            if (entries.isEmpty()) {
                                Toast.makeText(getContext(), "Нет записей в дневнике", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String[] dates = new String[entries.size()];
                            for (int i = 0; i < entries.size(); i++) dates[i] = entries.get(i).date;
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Выберите тренировку")
                                    .setItems(dates, (dialog, which) -> loadExercisesFromDiary(entries.get(which).id))
                                    .show();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<DiaryEntry>> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadExercisesFromDiary(int entryId) {
        RetrofitClient.getInstance(getContext()).getApiService().getDiaryExercises(entryId)
                .enqueue(new Callback<List<DiaryExercise>>() {
                    @Override
                    public void onResponse(Call<List<DiaryExercise>> call, Response<List<DiaryExercise>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<DiaryExercise> diaryExercises = response.body();
                            exerciseDataList.clear();
                            for (DiaryExercise de : diaryExercises) {
                                ExerciseData data = new ExerciseData();
                                data.name = de.name;
                                data.muscleGroup = de.muscleGroup;
                                loadDiarySetsForExerciseData(de.id, data, () -> {
                                    if (exerciseDataList.size() == diaryExercises.size()) {
                                        startButton.setEnabled(true);
                                        Toast.makeText(getContext(), "Тренировка выбрана", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<List<DiaryExercise>> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadDiarySetsForExerciseData(int diaryExerciseId, ExerciseData data, Runnable onComplete) {
        RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSets(diaryExerciseId)
                .enqueue(new Callback<List<DiaryExerciseSet>>() {
                    @Override
                    public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (DiaryExerciseSet ds : response.body()) {
                                data.sets.add(new SetData(ds.reps, ds.weight));
                            }
                        }
                        exerciseDataList.add(data);
                        onComplete.run();
                    }
                    @Override
                    public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {
                        exerciseDataList.add(data);
                        onComplete.run();
                    }
                });
    }

    private void startTraining() {
        String today = SDF.format(new Date());
        currentDiaryEntryId = -1;

        DiaryEntry newEntry = new DiaryEntry();
        newEntry.userId = MainActivity.currentUserId;
        newEntry.date = today;
        RetrofitClient.getInstance(getContext()).getApiService().createDiaryEntry(newEntry)
                .enqueue(new Callback<DiaryEntry>() {
                    @Override
                    public void onResponse(Call<DiaryEntry> call, Response<DiaryEntry> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            DiaryEntry created = response.body();
                            currentDiaryEntryId = created.id;
                            new Thread(() -> {
                                created.userId = MainActivity.currentUserId;
                                AppDatabase.getInstance(getContext()).diaryEntryDao().insert(created);
                            }).start();
                            saveExercisesToServer(created.id);
                        } else {
                            Toast.makeText(getContext(), "Ошибка создания дневника", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<DiaryEntry> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });

        currentState = State.RUNNING;
        startTime = System.currentTimeMillis();
        updateUIForState(State.RUNNING);
        timerText.setText(R.string.zero_time);
        sourceSelectionLayout.setVisibility(View.GONE);
        exerciseLayout.setVisibility(View.VISIBLE);
        currentExerciseIndex = 0;
        renderAllExercises();
        showCurrentExercise();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                int sec = (int) (elapsed / 1000) % 60;
                int min = (int) (elapsed / (1000 * 60));
                timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
                handler.postDelayed(this, 500);
            }
        };
        handler.post(timerRunnable);
    }

    private void saveExercisesToServer(int diaryEntryId) {
        if (!savingExercises.compareAndSet(false, true)) {
            return;
        }
        new Thread(() -> {
            AppDatabase.getInstance(getContext()).diaryExerciseDao().deleteAllByEntryId(diaryEntryId);
        }).start();

        processNextExercise(0, diaryEntryId);
    }

    private void processNextExercise(int index, int diaryEntryId) {
        if (index >= exerciseDataList.size()) {
            savingExercises.set(false);
            return;
        }
        ExerciseData data = exerciseDataList.get(index);
        DiaryExercise de = new DiaryExercise();
        de.diaryEntryId = diaryEntryId;
        de.orderIndex = index + 1;
        de.name = data.name;
        de.muscleGroup = data.muscleGroup;

        RetrofitClient.getInstance(getContext()).getApiService()
                .createDiaryExercise(diaryEntryId, de)
                .enqueue(new Callback<DiaryExercise>() {
                    @Override
                    public void onResponse(Call<DiaryExercise> call, Response<DiaryExercise> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            DiaryExercise createdEx = response.body();
                            int serverExerciseId = createdEx.id;
                            // Вставка упражнения в Room в фоновом потоке
                            new Thread(() -> {
                                createdEx.diaryEntryId = diaryEntryId;
                                long localExerciseId = AppDatabase.getInstance(getContext()).diaryExerciseDao().insertWithReturn(createdEx);

                                // Создаём подходы на сервере, а их Room-вставку делаем в фоне
                                for (int j = 0; j < data.sets.size(); j++) {
                                    SetData sd = data.sets.get(j);
                                    DiaryExerciseSet des = new DiaryExerciseSet();
                                    des.diaryExerciseId = (int) localExerciseId;
                                    des.setNumber = j + 1;
                                    des.reps = sd.reps;
                                    des.weight = sd.weight;

                                    RetrofitClient.getInstance(getContext()).getApiService()
                                            .createDiaryExerciseSet(serverExerciseId, new DiaryExerciseSet(serverExerciseId, j + 1, sd.reps, sd.weight))
                                            .enqueue(new Callback<DiaryExerciseSet>() {
                                                @Override
                                                public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {
                                                    if (response.isSuccessful() && response.body() != null) {
                                                        DiaryExerciseSet serverSet = response.body();
                                                        // Вставка подхода в Room в фоне
                                                        new Thread(() -> {
                                                            serverSet.diaryExerciseId = (int) localExerciseId;
                                                            AppDatabase.getInstance(getContext()).diaryExerciseSetDao().insertSet(serverSet);
                                                        }).start();
                                                    }
                                                }
                                                @Override
                                                public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                                            });
                                }
                                // Переход к следующему упражнению
                                handler.postDelayed(() -> processNextExercise(index + 1, diaryEntryId), 100);
                            }).start();
                        } else {
                            processNextExercise(index + 1, diaryEntryId);
                        }
                    }
                    @Override
                    public void onFailure(Call<DiaryExercise> call, Throwable t) {
                        processNextExercise(index + 1, diaryEntryId);
                    }
                });
    }

    private void pauseTraining() {
        if (currentState != State.RUNNING) return;
        handler.removeCallbacks(timerRunnable);
        pauseStart = System.currentTimeMillis();
        updateUIForState(State.PAUSED);
    }

    private void resumeTraining() {
        if (currentState != State.PAUSED) return;
        startTime += System.currentTimeMillis() - pauseStart;
        updateUIForState(State.RUNNING);
        handler.post(timerRunnable);
    }

    private void stopTraining(boolean saveSession) {
        if (currentState == State.RUNNING || currentState == State.PAUSED) {
            handler.removeCallbacks(timerRunnable);
            long elapsed = (currentState == State.PAUSED) ? pauseStart - startTime : System.currentTimeMillis() - startTime;
            final int totalSeconds = (int) (elapsed / 1000);
            float tonnage = 0;
            for (ExerciseData data : exerciseDataList) {
                for (SetData sd : data.sets) tonnage += sd.reps * sd.weight;
            }
            if (saveSession) {
                TrainingSession session = new TrainingSession();
                session.userId = MainActivity.currentUserId;
                session.totalSeconds = totalSeconds;
                session.tonnage = tonnage;
                RetrofitClient.getInstance(getContext()).getApiService()
                        .createTrainingSession(session)
                        .enqueue(new Callback<TrainingSession>() {
                            @Override
                            public void onResponse(Call<TrainingSession> call, Response<TrainingSession> response) {}
                            @Override
                            public void onFailure(Call<TrainingSession> call, Throwable t) {}
                        });
            }
            updateUIForState(State.IDLE);
            timerText.setText(R.string.zero_time);
            exerciseLayout.setVisibility(View.GONE);
            sourceSelectionLayout.setVisibility(View.VISIBLE);
            startButton.setEnabled(false);
            exerciseDataList.clear();
            currentExerciseIndex = -1;
            currentDiaryEntryId = -1;
        }
    }

    private void renderAllExercises() {
        allExercisesList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < exerciseDataList.size(); i++) {
            ExerciseData exData = exerciseDataList.get(i);
            TextView header = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, allExercisesList, false);
            header.setText((i + 1) + ". " + exData.name);
            header.setPadding(16, 8, 16, 8);
            header.setTextSize(16);
            header.setBackgroundColor(i == currentExerciseIndex ?
                    Color.parseColor("#FFCC80") : Color.LTGRAY);
            allExercisesList.addView(header);

            for (int s = 0; s < exData.sets.size(); s++) {
                View setRow = inflater.inflate(R.layout.item_exercise_set, allExercisesList, false);
                TextView setInfo = setRow.findViewById(R.id.setInfo);
                SetData sd = exData.sets.get(s);
                setInfo.setText("Подход " + (s + 1) + ": " + sd.reps + " x " + sd.weight);
                setRow.setBackgroundColor(i == currentExerciseIndex ?
                        Color.parseColor("#FFF3E0") : Color.parseColor("#F0F0F0"));

                ImageButton addBtn = setRow.findViewById(R.id.addSetButton);
                ImageButton delBtn = setRow.findViewById(R.id.deleteSetButton);
                final int exIndex = i;
                final int setIndex = s;
                final SetData currentSet = sd;
                addBtn.setOnClickListener(v -> {
                    exData.sets.add(setIndex + 1, new SetData(currentSet.reps, currentSet.weight));
                    saveAndRefresh();
                });
                delBtn.setOnClickListener(v -> {
                    if (exData.sets.size() > 1) {
                        exData.sets.remove(setIndex);
                        saveAndRefresh();
                    } else {
                        Toast.makeText(getContext(), "Нельзя удалить последний подход", Toast.LENGTH_SHORT).show();
                    }
                });
                allExercisesList.addView(setRow);
            }
        }
    }

    private void saveAndRefresh() {
        if (currentDiaryEntryId == -1) return;
        new Thread(() -> {
            saveExercisesToServer(currentDiaryEntryId);
            getActivity().runOnUiThread(() -> {
                renderAllExercises();
                showCurrentExercise();
            });
        }).start();
    }

    private void showCurrentExercise() {
        if (currentExerciseIndex < 0 || currentExerciseIndex >= exerciseDataList.size()) return;
        ExerciseData data = exerciseDataList.get(currentExerciseIndex);
        currentExerciseName.setText(data.name);
        String details = getString(R.string.exercise_details, data.sets.size(),
                data.sets.isEmpty() ? 0 : data.sets.get(0).reps,
                data.sets.isEmpty() ? 0f : data.sets.get(0).weight);
        currentExerciseDetails.setText(details);
        prevExerciseButton.setEnabled(currentExerciseIndex > 0);
        nextExerciseButton.setEnabled(currentExerciseIndex < exerciseDataList.size() - 1);
        prevExerciseButton.setAlpha(currentExerciseIndex > 0 ? 1.0f : 0.5f);
        nextExerciseButton.setAlpha(currentExerciseIndex < exerciseDataList.size() - 1 ? 1.0f : 0.5f);
    }

    private void navigateExercise(int direction) {
        if (exerciseDataList.isEmpty()) return;
        int newIndex = currentExerciseIndex + direction;
        if (newIndex < 0 || newIndex >= exerciseDataList.size()) return;
        currentExerciseIndex = newIndex;
        renderAllExercises();
        showCurrentExercise();
    }
}