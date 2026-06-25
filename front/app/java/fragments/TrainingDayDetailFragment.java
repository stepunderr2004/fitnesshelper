package com.example.fitnesshelper.fragments;

import android.os.Bundle;
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
import com.example.fitnesshelper.helpers.ExerciseListHelper;
import com.example.fitnesshelper.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainingDayDetailFragment extends Fragment {

    private static final String ARG_DAY_ID = "day_id";
    private static final String ARG_CLIENT_ID = "client_id";

    private int dayId;
    private int clientId = -1;
    private LinearLayout exercisesContainer;
    private Button addExerciseButton;
    private List<Exercise> currentExercises = new ArrayList<>();
    private boolean isLoading = false;

    public static TrainingDayDetailFragment newInstance(int dayId, int clientId) {
        TrainingDayDetailFragment fragment = new TrainingDayDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DAY_ID, dayId);
        args.putInt(ARG_CLIENT_ID, clientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training_day_detail, container, false);
        Button backBtn = view.findViewById(R.id.backButton);
        exercisesContainer = view.findViewById(R.id.exercisesContainer);
        addExerciseButton = view.findViewById(R.id.addExerciseButton);

        if (getArguments() != null) {
            dayId = getArguments().getInt(ARG_DAY_ID);
            clientId = getArguments().getInt(ARG_CLIENT_ID, -1);
        }

        backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        addExerciseButton.setOnClickListener(v -> {
            if (isLoading) return;
            ExerciseListHelper.createExerciseDropdown(requireContext(), addExerciseButton,
                    (name, group) -> addNewExercise(name, group)).show();
        });
        loadExercises();
        return view;
    }

    private void loadExercises() {
        if (!isAdded() || getContext() == null) return;
        isLoading = true;

        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getExercises(dayId)
                    .enqueue(new Callback<List<Exercise>>() {
                        @Override
                        public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                            isLoading = false;
                            if (response.isSuccessful() && response.body() != null) {
                                renumberAndDisplay(response.body());
                            } else {
                                Toast.makeText(getContext(), "Ошибка загрузки упражнений", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<List<Exercise>> call, Throwable t) {
                            isLoading = false;
                            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getExercisesForClient(clientId, dayId)
                    .enqueue(new Callback<List<Exercise>>() {
                        @Override
                        public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                            isLoading = false;
                            if (response.isSuccessful() && response.body() != null) {
                                renumberAndDisplay(response.body());
                            } else {
                                Toast.makeText(getContext(), "Ошибка загрузки упражнений", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<List<Exercise>> call, Throwable t) {
                            isLoading = false;
                            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void renumberAndDisplay(List<Exercise> exercises) {
        currentExercises = exercises;
        for (Exercise ex : currentExercises) ex.trainingDayId = dayId;
        currentExercises.sort(Comparator.comparingInt(e -> e.orderIndex));
        boolean needsUpdate = false;
        for (int i = 0; i < currentExercises.size(); i++) {
            int newIndex = i + 1;
            if (currentExercises.get(i).orderIndex != newIndex) {
                currentExercises.get(i).orderIndex = newIndex;
                needsUpdate = true;
            }
        }
        exercisesContainer.removeAllViews();
        for (Exercise ex : currentExercises) {
            addExerciseRow(ex);
        }
        updateAddButton();
        if (needsUpdate) {
            for (Exercise ex : currentExercises) {
                if (clientId == -1) {
                    RetrofitClient.getInstance(getContext()).getApiService()
                            .updateExercise(dayId, ex.id, ex)
                            .enqueue(new Callback<Exercise>() {
                                @Override public void onResponse(Call<Exercise> call, Response<Exercise> response) {}
                                @Override public void onFailure(Call<Exercise> call, Throwable t) {}
                            });
                } else {
                    RetrofitClient.getInstance(getContext()).getApiService()
                            .updateExerciseForClient(clientId, dayId, ex.id, ex)
                            .enqueue(new Callback<Exercise>() {
                                @Override public void onResponse(Call<Exercise> call, Response<Exercise> response) {}
                                @Override public void onFailure(Call<Exercise> call, Throwable t) {}
                            });
                }
            }
        }
    }

    private void addExerciseRow(Exercise exercise) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_exercise, exercisesContainer, false);
        TextView numberView = row.findViewById(R.id.exerciseNumber);
        TextView nameView = row.findViewById(R.id.exerciseName);
        TextView setsCountView = row.findViewById(R.id.exerciseSetsCount);
        LinearLayout setsContainer = row.findViewById(R.id.setsContainer);
        ImageButton deleteExerciseBtn = row.findViewById(R.id.deleteExerciseButton);

        numberView.setText(String.valueOf(exercise.orderIndex));
        nameView.setText(exercise.name);

        loadSetsForExercise(exercise, setsContainer, setsCountView);

        nameView.setOnClickListener(v -> {
            ExerciseListHelper.createExerciseDropdown(requireContext(), nameView,
                    (name, group) -> updateExerciseName(exercise, name, group)).show();
        });

        setsCountView.setOnClickListener(v -> {
            ExerciseListHelper.createParameterPicker(requireContext(), setsCountView, "setsCount",
                    (field, value) -> changeSetsCount(exercise, (int) value, setsContainer, setsCountView)).show();
        });

        deleteExerciseBtn.setOnClickListener(v -> {
            if (clientId == -1) {
                RetrofitClient.getInstance(getContext()).getApiService()
                        .deleteExercise(dayId, exercise.id)
                        .enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> response) { loadExercises(); }
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
            } else {
                RetrofitClient.getInstance(getContext()).getApiService()
                        .deleteExerciseForClient(clientId, dayId, exercise.id)
                        .enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> response) { loadExercises(); }
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
            }
        });

        exercisesContainer.addView(row);
    }

    private void loadSetsForExercise(Exercise exercise, LinearLayout container, TextView countView) {
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getExerciseSets(exercise.id)
                    .enqueue(new Callback<List<ExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<ExerciseSet> sets = response.body();
                                for (ExerciseSet s : sets) s.exerciseId = exercise.id; // исправление
                                countView.setText(String.valueOf(sets.size()));
                                container.removeAllViews();
                                for (ExerciseSet set : sets) {
                                    addSetRow(container, exercise, set);
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getExerciseSetsForClient(clientId, exercise.id)
                    .enqueue(new Callback<List<ExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<ExerciseSet> sets = response.body();
                                for (ExerciseSet s : sets) s.exerciseId = exercise.id;
                                countView.setText(String.valueOf(sets.size()));
                                container.removeAllViews();
                                for (ExerciseSet set : sets) {
                                    addSetRow(container, exercise, set);
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {}
                    });
        }
    }

    private void addSetRow(LinearLayout container, Exercise exercise, ExerciseSet set) {
        View setRow = LayoutInflater.from(getContext()).inflate(R.layout.item_exercise_set_row, container, false);
        TextView setNumberText = setRow.findViewById(R.id.setNumberText);
        TextView repsView = setRow.findViewById(R.id.setReps);
        TextView weightView = setRow.findViewById(R.id.setWeight);
        ImageButton copyDownBtn = setRow.findViewById(R.id.copyDownButton);
        ImageButton addSetBtn = setRow.findViewById(R.id.addSetButton);
        ImageButton deleteSetBtn = setRow.findViewById(R.id.deleteSetButton);

        setNumberText.setText(String.valueOf(set.setNumber));
        repsView.setText(String.valueOf(set.reps));
        weightView.setText(String.valueOf(set.weight));

        repsView.setOnClickListener(v -> {
            ExerciseListHelper.createParameterPicker(requireContext(), repsView, "reps",
                    (field, value) -> {
                        set.reps = (int) value;
                        updateSetInDb(set, repsView, weightView, null);
                    }).show();
        });

        weightView.setOnClickListener(v -> {
            ExerciseListHelper.createParameterPicker(requireContext(), weightView, "weight",
                    (field, value) -> {
                        set.weight = value;
                        updateSetInDb(set, null, weightView, null);
                    }).show();
        });

        copyDownBtn.setOnClickListener(v -> {
            if (clientId == -1) {
                RetrofitClient.getInstance(getContext()).getApiService().getExerciseSets(exercise.id)
                        .enqueue(new Callback<List<ExerciseSet>>() {
                            @Override
                            public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    List<ExerciseSet> allSets = response.body();
                                    for (ExerciseSet s : allSets) s.exerciseId = exercise.id; // исправление
                                    for (int i = set.setNumber - 1; i < allSets.size(); i++) {
                                        ExerciseSet s = allSets.get(i);
                                        s.reps = set.reps;
                                        s.weight = set.weight;
                                        RetrofitClient.getInstance(getContext()).getApiService()
                                                .updateExerciseSet(s.exerciseId, s.id, s)
                                                .enqueue(new Callback<ExerciseSet>() {
                                                    @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {}
                                                    @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {}
                                                });
                                    }
                                    reloadSetsForExercise(exercise, container);
                                }
                            }
                            @Override public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {}
                        });
            } else {
                RetrofitClient.getInstance(getContext()).getApiService().getExerciseSetsForClient(clientId, exercise.id)
                        .enqueue(new Callback<List<ExerciseSet>>() {
                            @Override
                            public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    List<ExerciseSet> allSets = response.body();
                                    for (ExerciseSet s : allSets) s.exerciseId = exercise.id;
                                    for (int i = set.setNumber - 1; i < allSets.size(); i++) {
                                        ExerciseSet s = allSets.get(i);
                                        s.reps = set.reps;
                                        s.weight = set.weight;
                                        RetrofitClient.getInstance(getContext()).getApiService()
                                                .updateExerciseSetForClient(clientId, s.exerciseId, s.id, s)
                                                .enqueue(new Callback<ExerciseSet>() {
                                                    @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {}
                                                    @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {}
                                                });
                                    }
                                    reloadSetsForExercise(exercise, container);
                                }
                            }
                            @Override public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {}
                        });
            }
        });

        addSetBtn.setOnClickListener(v -> {
            if (clientId == -1) {
                ExerciseSet newSet = new ExerciseSet(exercise.id, set.setNumber + 1, set.reps, set.weight);
                RetrofitClient.getInstance(getContext()).getApiService().createExerciseSet(exercise.id, newSet)
                        .enqueue(new Callback<ExerciseSet>() {
                            @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {
                                reloadSetsForExercise(exercise, container);
                            }
                            @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {}
                        });
            } else {
                ExerciseSet newSet = new ExerciseSet(exercise.id, set.setNumber + 1, set.reps, set.weight);
                RetrofitClient.getInstance(getContext()).getApiService().createExerciseSetForClient(clientId, exercise.id, newSet)
                        .enqueue(new Callback<ExerciseSet>() {
                            @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {
                                reloadSetsForExercise(exercise, container);
                            }
                            @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {}
                        });
            }
        });

        deleteSetBtn.setOnClickListener(v -> {
            if (clientId == -1) {
                RetrofitClient.getInstance(getContext()).getApiService().deleteExerciseSet(exercise.id, set.id)
                        .enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                                reloadSetsForExercise(exercise, container);
                            }
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
            } else {
                RetrofitClient.getInstance(getContext()).getApiService().deleteExerciseSetForClient(clientId, exercise.id, set.id)
                        .enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                                reloadSetsForExercise(exercise, container);
                            }
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
            }
        });

        container.addView(setRow);
    }

    private void updateSetInDb(ExerciseSet set, TextView repsView, TextView weightView, Runnable afterUpdate) {
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService()
                    .updateExerciseSet(set.exerciseId, set.id, set)
                    .enqueue(new Callback<ExerciseSet>() {
                        @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {
                            if (afterUpdate != null) getActivity().runOnUiThread(afterUpdate);
                            else {
                                if (repsView != null) repsView.setText(String.valueOf(set.reps));
                                if (weightView != null) weightView.setText(String.valueOf(set.weight));
                            }
                        }
                        @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService()
                    .updateExerciseSetForClient(clientId, set.exerciseId, set.id, set)
                    .enqueue(new Callback<ExerciseSet>() {
                        @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {
                            if (afterUpdate != null) afterUpdate.run();
                        }
                        @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {}
                    });
        }
    }

    private void reloadSetsForExercise(Exercise exercise, LinearLayout container) {
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getExerciseSets(exercise.id)
                    .enqueue(new Callback<List<ExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<ExerciseSet> sets = response.body();
                                for (ExerciseSet s : sets) s.exerciseId = exercise.id;
                                container.removeAllViews();
                                for (ExerciseSet s : sets) {
                                    addSetRow(container, exercise, s);
                                }
                                View parent = (View) container.getParent();
                                if (parent != null) {
                                    TextView countView = parent.findViewById(R.id.exerciseSetsCount);
                                    if (countView != null) countView.setText(String.valueOf(sets.size()));
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getExerciseSetsForClient(clientId, exercise.id)
                    .enqueue(new Callback<List<ExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<ExerciseSet> sets = response.body();
                                for (ExerciseSet s : sets) s.exerciseId = exercise.id;
                                container.removeAllViews();
                                for (ExerciseSet s : sets) {
                                    addSetRow(container, exercise, s);
                                }
                                View parent = (View) container.getParent();
                                if (parent != null) {
                                    TextView countView = parent.findViewById(R.id.exerciseSetsCount);
                                    if (countView != null) countView.setText(String.valueOf(sets.size()));
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {}
                    });
        }
    }

    private void changeSetsCount(Exercise exercise, int newCount, LinearLayout container, TextView countView) {
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getExerciseSets(exercise.id)
                    .enqueue(new Callback<List<ExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<ExerciseSet> currentSets = response.body();
                                for (ExerciseSet s : currentSets) s.exerciseId = exercise.id;
                                int currentCount = currentSets.size();
                                if (newCount > currentCount) {
                                    int defaultReps = 1;
                                    float defaultWeight = 20f;
                                    if (currentCount > 0) {
                                        ExerciseSet last = currentSets.get(currentCount - 1);
                                        defaultReps = last.reps;
                                        defaultWeight = last.weight;
                                    }
                                    for (int i = currentCount + 1; i <= newCount; i++) {
                                        ExerciseSet newSet = new ExerciseSet(exercise.id, i, defaultReps, defaultWeight);
                                        RetrofitClient.getInstance(getContext()).getApiService().createExerciseSet(exercise.id, newSet)
                                                .enqueue(new Callback<ExerciseSet>() {
                                                    @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {}
                                                    @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {}
                                                });
                                    }
                                } else if (newCount < currentCount) {
                                    for (int i = currentCount; i > newCount; i--) {
                                        ExerciseSet toDelete = currentSets.get(i - 1);
                                        RetrofitClient.getInstance(getContext()).getApiService().deleteExerciseSet(exercise.id, toDelete.id)
                                                .enqueue(new Callback<Void>() {
                                                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                                                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                                                });
                                    }
                                }
                                reloadSetsForExercise(exercise, container);
                            }
                        }
                        @Override public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getExerciseSetsForClient(clientId, exercise.id)
                    .enqueue(new Callback<List<ExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<ExerciseSet>> call, Response<List<ExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<ExerciseSet> currentSets = response.body();
                                for (ExerciseSet s : currentSets) s.exerciseId = exercise.id;
                                int currentCount = currentSets.size();
                                if (newCount > currentCount) {
                                    int defaultReps = 1;
                                    float defaultWeight = 20f;
                                    if (currentCount > 0) {
                                        ExerciseSet last = currentSets.get(currentCount - 1);
                                        defaultReps = last.reps;
                                        defaultWeight = last.weight;
                                    }
                                    for (int i = currentCount + 1; i <= newCount; i++) {
                                        ExerciseSet newSet = new ExerciseSet(exercise.id, i, defaultReps, defaultWeight);
                                        RetrofitClient.getInstance(getContext()).getApiService()
                                                .createExerciseSetForClient(clientId, exercise.id, newSet)
                                                .enqueue(new Callback<ExerciseSet>() {
                                                    @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {}
                                                    @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {}
                                                });
                                    }
                                } else if (newCount < currentCount) {
                                    for (int i = currentCount; i > newCount; i--) {
                                        ExerciseSet toDelete = currentSets.get(i - 1);
                                        RetrofitClient.getInstance(getContext()).getApiService()
                                                .deleteExerciseSetForClient(clientId, exercise.id, toDelete.id)
                                                .enqueue(new Callback<Void>() {
                                                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                                                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                                                });
                                    }
                                }
                                reloadSetsForExercise(exercise, container);
                            }
                        }
                        @Override public void onFailure(Call<List<ExerciseSet>> call, Throwable t) {}
                    });
        }
    }

    private void updateExerciseName(Exercise exercise, String newName, String newGroup) {
        exercise.name = newName;
        exercise.muscleGroup = newGroup;
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService()
                    .updateExercise(dayId, exercise.id, exercise)
                    .enqueue(new Callback<Exercise>() {
                        @Override public void onResponse(Call<Exercise> call, Response<Exercise> response) {
                            loadExercises();
                        }
                        @Override public void onFailure(Call<Exercise> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService()
                    .updateExerciseForClient(clientId, dayId, exercise.id, exercise)
                    .enqueue(new Callback<Exercise>() {
                        @Override public void onResponse(Call<Exercise> call, Response<Exercise> response) {
                            loadExercises();
                        }
                        @Override public void onFailure(Call<Exercise> call, Throwable t) {}
                    });
        }
    }

    private void addNewExercise(String exerciseName, String muscleGroup) {
        // Определяем следующий orderIndex
        int maxOrder = 0;
        for (Exercise e : currentExercises) {
            if (e.orderIndex > maxOrder) maxOrder = e.orderIndex;
        }
        int nextOrder = maxOrder + 1;

        if (clientId == -1) {
            Exercise newEx = new Exercise(dayId, nextOrder, exerciseName, muscleGroup);
            RetrofitClient.getInstance(getContext()).getApiService().createExercise(dayId, newEx)
                    .enqueue(new Callback<Exercise>() {
                        @Override
                        public void onResponse(Call<Exercise> call, Response<Exercise> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                // Создаём первый подход по умолчанию
                                int exerciseId = response.body().id;
                                ExerciseSet defaultSet = new ExerciseSet(exerciseId, 1, 1, 20f);
                                RetrofitClient.getInstance(getContext()).getApiService().createExerciseSet(exerciseId, defaultSet)
                                        .enqueue(new Callback<ExerciseSet>() {
                                            @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {
                                                loadExercises();
                                            }
                                            @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {
                                                loadExercises();
                                            }
                                        });
                            }
                        }
                        @Override public void onFailure(Call<Exercise> call, Throwable t) {}
                    });
        } else {
            Exercise newEx = new Exercise(dayId, nextOrder, exerciseName, muscleGroup);
            RetrofitClient.getInstance(getContext()).getApiService().createExerciseForClient(clientId, dayId, newEx)
                    .enqueue(new Callback<Exercise>() {
                        @Override
                        public void onResponse(Call<Exercise> call, Response<Exercise> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                int exerciseId = response.body().id;
                                ExerciseSet defaultSet = new ExerciseSet(exerciseId, 1, 1, 20f);
                                RetrofitClient.getInstance(getContext()).getApiService().createExerciseSetForClient(clientId, exerciseId, defaultSet)
                                        .enqueue(new Callback<ExerciseSet>() {
                                            @Override public void onResponse(Call<ExerciseSet> call, Response<ExerciseSet> response) {
                                                loadExercises();
                                            }
                                            @Override public void onFailure(Call<ExerciseSet> call, Throwable t) {
                                                loadExercises();
                                            }
                                        });
                            }
                        }
                        @Override public void onFailure(Call<Exercise> call, Throwable t) {}
                    });
        }
    }

    private void updateAddButton() {
        addExerciseButton.setVisibility(currentExercises.size() < 10 ? View.VISIBLE : View.GONE);
    }
}