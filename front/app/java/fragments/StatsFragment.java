package com.example.fitnesshelper.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.activities.MainActivity;
import com.example.fitnesshelper.db.*;
import com.example.fitnesshelper.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsFragment extends Fragment {

    private static final String ARG_CLIENT_ID = "client_id";
    private LinearLayout diaryEntriesContainer;
    private Button createTrainingButton;
    private int clientId = -1;

    public static StatsFragment newInstance(int clientId) {
        StatsFragment fragment = new StatsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIENT_ID, clientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        diaryEntriesContainer = view.findViewById(R.id.diaryEntriesContainer);
        createTrainingButton = view.findViewById(R.id.createTrainingButton);

        if (getArguments() != null && getArguments().containsKey(ARG_CLIENT_ID)) {
            clientId = getArguments().getInt(ARG_CLIENT_ID);
        }

        createTrainingButton.setOnClickListener(v -> showDatePicker());
        loadDiaryEntries();
        return view;
    }

    private void loadDiaryEntries() {
        if (!isAdded() || getContext() == null) return;

        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryEntries()
                    .enqueue(new Callback<List<DiaryEntry>>() {
                        @Override
                        public void onResponse(Call<List<DiaryEntry>> call, Response<List<DiaryEntry>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                displayEntries(response.body());
                            }
                        }
                        @Override
                        public void onFailure(Call<List<DiaryEntry>> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryEntriesForClient(clientId)
                    .enqueue(new Callback<List<DiaryEntry>>() {
                        @Override
                        public void onResponse(Call<List<DiaryEntry>> call, Response<List<DiaryEntry>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                for (DiaryEntry e : response.body()) e.userId = clientId;
                                displayEntries(response.body());
                            }
                        }
                        @Override
                        public void onFailure(Call<List<DiaryEntry>> call, Throwable t) {}
                    });
        }
    }

    private void displayEntries(List<DiaryEntry> entries) {
        diaryEntriesContainer.removeAllViews();
        for (DiaryEntry entry : entries) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);

            Button btn = new Button(getContext());
            btn.setText(entry.date);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            btn.setLayoutParams(btnParams);
            btn.setOnClickListener(v -> openDiaryEntry(entry.id));

            ImageButton deleteBtn = new ImageButton(getContext());
            deleteBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            deleteBtn.setBackgroundResource(android.R.color.transparent);
            deleteBtn.setOnClickListener(v -> confirmDeleteEntry(entry));
            row.addView(btn);
            row.addView(deleteBtn);

            diaryEntriesContainer.addView(row);
        }
    }

    private void confirmDeleteEntry(DiaryEntry entry) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удалить запись?")
                .setMessage("Вы уверены, что хотите удалить запись за " + entry.date + "?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    if (clientId == -1) {
                        RetrofitClient.getInstance(getContext()).getApiService().deleteDiaryEntry(entry.id)
                                .enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (response.isSuccessful()) loadDiaryEntries();
                                    }
                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {}
                                });
                    } else {
                        RetrofitClient.getInstance(getContext()).getApiService().deleteDiaryEntryForClient(clientId, entry.id)
                                .enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (response.isSuccessful()) loadDiaryEntries();
                                    }
                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {}
                                });
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void openDiaryEntry(int entryId) {
        DiaryExerciseFragment fragment = DiaryExerciseFragment.newInstance(entryId, clientId);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year1, monthOfYear, dayOfMonth);
                    String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCal.getTime());
                    checkDiaryLimitAndProceed(dateStr);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void checkDiaryLimitAndProceed(String dateStr) {
        Call<List<DiaryEntry>> call = (clientId == -1)
                ? RetrofitClient.getInstance(getContext()).getApiService().getDiaryEntries()
                : RetrofitClient.getInstance(getContext()).getApiService().getDiaryEntriesForClient(clientId);

        call.enqueue(new Callback<List<DiaryEntry>>() {
            @Override
            public void onResponse(Call<List<DiaryEntry>> call, Response<List<DiaryEntry>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().size() >= 312) {
                        Toast.makeText(getContext(), "Достигнут лимит записей (312)", Toast.LENGTH_SHORT).show();
                    } else {
                        showTemplateDialog(dateStr);
                    }
                }
            }
            @Override
            public void onFailure(Call<List<DiaryEntry>> call, Throwable t) {}
        });
    }

    private void showTemplateDialog(String dateStr) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Использовать шаблон");
        String[] options = {"Моя программа", "Моя тренировка", "Создать с нуля"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                selectProgramTemplate(dateStr);
            } else if (which == 1) {
                selectDiaryTemplate(dateStr);
            } else {
                createEmptyDiary(dateStr);
            }
        });
        builder.show();
    }

    private void selectProgramTemplate(String targetDate) {
        Call<List<Program>> programsCall = (clientId == -1)
                ? RetrofitClient.getInstance(getContext()).getApiService().getPrograms()
                : RetrofitClient.getInstance(getContext()).getApiService().getProgramsForClient(clientId);

        programsCall.enqueue(new Callback<List<Program>>() {
            @Override
            public void onResponse(Call<List<Program>> call, Response<List<Program>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Program> programs = response.body();
                    if (programs.isEmpty()) {
                        Toast.makeText(getContext(), "Нет программ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] names = new String[programs.size()];
                    for (int i = 0; i < programs.size(); i++) names[i] = programs.get(i).name;
                    new AlertDialog.Builder(getContext())
                            .setTitle("Выберите программу")
                            .setItems(names, (dialog, which) -> selectDayInProgram(programs.get(which).id, targetDate))
                            .show();
                }
            }
            @Override
            public void onFailure(Call<List<Program>> call, Throwable t) {}
        });
    }

    private void selectDayInProgram(int programId, String targetDate) {
        Call<List<TrainingDay>> daysCall = (clientId == -1)
                ? RetrofitClient.getInstance(getContext()).getApiService().getTrainingDays(programId)
                : RetrofitClient.getInstance(getContext()).getApiService().getTrainingDaysForClient(clientId, programId);

        daysCall.enqueue(new Callback<List<TrainingDay>>() {
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
                            .setItems(dayNames, (dialog, which) -> copyProgramDayToDiary(days.get(which).id, targetDate))
                            .show();
                }
            }
            @Override
            public void onFailure(Call<List<TrainingDay>> call, Throwable t) {}
        });
    }

    private void copyProgramDayToDiary(int dayId, String targetDate) {
        DiaryEntry newEntry = new DiaryEntry();
        newEntry.userId = (clientId == -1) ? MainActivity.currentUserId : clientId;
        newEntry.date = targetDate;

        Call<DiaryEntry> createCall = (clientId == -1)
                ? RetrofitClient.getInstance(getContext()).getApiService().createDiaryEntry(newEntry)
                : RetrofitClient.getInstance(getContext()).getApiService().createDiaryEntryForClient(clientId, newEntry);

        createCall.enqueue(new Callback<DiaryEntry>() {
            @Override
            public void onResponse(Call<DiaryEntry> call, Response<DiaryEntry> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int newEntryId = response.body().id;
                    Call<List<Exercise>> exercisesCall = (clientId == -1)
                            ? RetrofitClient.getInstance(getContext()).getApiService().getExercises(dayId)
                            : RetrofitClient.getInstance(getContext()).getApiService().getExercisesForClient(clientId, dayId);
                    exercisesCall.enqueue(new Callback<List<Exercise>>() {
                        @Override
                        public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                for (Exercise ex : response.body()) {
                                    copyExerciseToDiary(ex, newEntryId);
                                }
                                requireActivity().runOnUiThread(() -> {
                                    loadDiaryEntries();
                                    openDiaryEntry(newEntryId);
                                });
                            }
                        }
                        @Override
                        public void onFailure(Call<List<Exercise>> call, Throwable t) {}
                    });
                }
            }
            @Override
            public void onFailure(Call<DiaryEntry> call, Throwable t) {}
        });
    }

    private void copyExerciseToDiary(Exercise exercise, int diaryEntryId) {
        DiaryExercise de = new DiaryExercise();
        de.diaryEntryId = diaryEntryId;
        de.orderIndex = exercise.orderIndex;
        de.name = exercise.name;
        de.muscleGroup = exercise.muscleGroup;

        Call<DiaryExercise> createExCall = (clientId == -1)
                ? RetrofitClient.getInstance(getContext()).getApiService().createDiaryExercise(diaryEntryId, de)
                : RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseForClient(clientId, diaryEntryId, de);

        createExCall.enqueue(new Callback<DiaryExercise>() {
            @Override
            public void onResponse(Call<DiaryExercise> call, Response<DiaryExercise> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int newExerciseId = response.body().id;
                    Call<List<ExerciseSet>> setsCall = (clientId == -1)
                            ? RetrofitClient.getInstance(getContext()).getApiService().getExerciseSets(exercise.id)
                            : RetrofitClient.getInstance(getContext()).getApiService().getExerciseSetsForClient(clientId, exercise.id);
                    setsCall.enqueue(new Callback<List<ExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                for (ExerciseSet set : response.body()) {
                                    DiaryExerciseSet des = new DiaryExerciseSet();
                                    des.diaryExerciseId = newExerciseId;
                                    des.setNumber = set.setNumber;
                                    des.reps = set.reps;
                                    des.weight = set.weight;
                                    Call<DiaryExerciseSet> createSetCall = (clientId == -1)
                                            ? RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseSet(newExerciseId, des)
                                            : RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseSetForClient(clientId, newExerciseId, des);
                                    createSetCall.enqueue(new Callback<DiaryExerciseSet>() {
                                        @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {}
                                        @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                                    });
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {}
                    });
                }
            }
            @Override
            public void onFailure(Call<DiaryExercise> call, Throwable t) {}
        });
    }

    private void selectDiaryTemplate(String targetDate) {
        Call<List<DiaryEntry>> entriesCall = (clientId == -1)
                ? RetrofitClient.getInstance(getContext()).getApiService().getDiaryEntries()
                : RetrofitClient.getInstance(getContext()).getApiService().getDiaryEntriesForClient(clientId);

        entriesCall.enqueue(new Callback<List<DiaryEntry>>() {
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
                            .setItems(dates, (dialog, which) -> copyDiaryEntryToDiary(entries.get(which).id, targetDate))
                            .show();
                }
            }
            @Override
            public void onFailure(Call<List<DiaryEntry>> call, Throwable t) {}
        });
    }

    private void copyDiaryEntryToDiary(int sourceEntryId, String targetDate) {
        DiaryEntry newEntry = new DiaryEntry();
        newEntry.userId = (clientId == -1) ? MainActivity.currentUserId : clientId;
        newEntry.date = targetDate;

        Call<DiaryEntry> createCall = (clientId == -1)
                ? RetrofitClient.getInstance(getContext()).getApiService().createDiaryEntry(newEntry)
                : RetrofitClient.getInstance(getContext()).getApiService().createDiaryEntryForClient(clientId, newEntry);

        createCall.enqueue(new Callback<DiaryEntry>() {
            @Override
            public void onResponse(Call<DiaryEntry> call, Response<DiaryEntry> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int newEntryId = response.body().id;
                    Call<List<DiaryExercise>> exercisesCall = (clientId == -1)
                            ? RetrofitClient.getInstance(getContext()).getApiService().getDiaryExercises(sourceEntryId)
                            : RetrofitClient.getInstance(getContext()).getApiService().getDiaryExercisesForClient(clientId, sourceEntryId);
                    exercisesCall.enqueue(new Callback<List<DiaryExercise>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExercise>> call, Response<List<DiaryExercise>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                for (DiaryExercise de : response.body()) {
                                    copyDiaryExerciseToDiary(de, newEntryId);
                                }
                                requireActivity().runOnUiThread(() -> {
                                    loadDiaryEntries();
                                    openDiaryEntry(newEntryId);
                                });
                            }
                        }
                        @Override
                        public void onFailure(Call<List<DiaryExercise>> call, Throwable t) {}
                    });
                }
            }
            @Override
            public void onFailure(Call<DiaryEntry> call, Throwable t) {}
        });
    }

    private void copyDiaryExerciseToDiary(DiaryExercise sourceEx, int newEntryId) {
        DiaryExercise de = new DiaryExercise();
        de.diaryEntryId = newEntryId;
        de.orderIndex = sourceEx.orderIndex;
        de.name = sourceEx.name;
        de.muscleGroup = sourceEx.muscleGroup;

        Call<DiaryExercise> createExCall = (clientId == -1)
                ? RetrofitClient.getInstance(getContext()).getApiService().createDiaryExercise(newEntryId, de)
                : RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseForClient(clientId, newEntryId, de);

        createExCall.enqueue(new Callback<DiaryExercise>() {
            @Override
            public void onResponse(Call<DiaryExercise> call, Response<DiaryExercise> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int newExerciseId = response.body().id;
                    Call<List<DiaryExerciseSet>> setsCall = (clientId == -1)
                            ? RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSets(sourceEx.id)
                            : RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSetsForClient(clientId, sourceEx.id);
                    setsCall.enqueue(new Callback<List<DiaryExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                for (DiaryExerciseSet set : response.body()) {
                                    DiaryExerciseSet des = new DiaryExerciseSet();
                                    des.diaryExerciseId = newExerciseId;
                                    des.setNumber = set.setNumber;
                                    des.reps = set.reps;
                                    des.weight = set.weight;
                                    Call<DiaryExerciseSet> createSetCall = (clientId == -1)
                                            ? RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseSet(newExerciseId, des)
                                            : RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseSetForClient(clientId, newExerciseId, des);
                                    createSetCall.enqueue(new Callback<DiaryExerciseSet>() {
                                        @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {}
                                        @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                                    });
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {}
                    });
                }
            }
            @Override
            public void onFailure(Call<DiaryExercise> call, Throwable t) {}
        });
    }

    private void createEmptyDiary(String dateStr) {
        DiaryEntry entry = new DiaryEntry();
        entry.userId = (clientId == -1) ? MainActivity.currentUserId : clientId;
        entry.date = dateStr;

        Call<DiaryEntry> createCall = (clientId == -1)
                ? RetrofitClient.getInstance(getContext()).getApiService().createDiaryEntry(entry)
                : RetrofitClient.getInstance(getContext()).getApiService().createDiaryEntryForClient(clientId, entry);

        createCall.enqueue(new Callback<DiaryEntry>() {
            @Override
            public void onResponse(Call<DiaryEntry> call, Response<DiaryEntry> response) {
                if (response.isSuccessful() && response.body() != null) {
                    loadDiaryEntries();
                    openDiaryEntry(response.body().id);
                }
            }
            @Override
            public void onFailure(Call<DiaryEntry> call, Throwable t) {}
        });
    }
}