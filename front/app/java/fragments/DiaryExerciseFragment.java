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

public class DiaryExerciseFragment extends Fragment {

    private static final String ARG_ENTRY_ID = "entry_id";
    private static final String ARG_CLIENT_ID = "client_id";

    private int entryId;
    private int clientId = -1;
    private LinearLayout exercisesContainer;
    private Button addExerciseButton;
    private List<DiaryExercise> currentExercises = new ArrayList<>();

    public static DiaryExerciseFragment newInstance(int entryId, int clientId) {
        DiaryExerciseFragment fragment = new DiaryExerciseFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ENTRY_ID, entryId);
        args.putInt(ARG_CLIENT_ID, clientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary_exercise, container, false);
        Button backBtn = view.findViewById(R.id.backButton);
        exercisesContainer = view.findViewById(R.id.exercisesContainer);
        addExerciseButton = view.findViewById(R.id.addExerciseButton);

        if (getArguments() != null) {
            entryId = getArguments().getInt(ARG_ENTRY_ID);
            clientId = getArguments().getInt(ARG_CLIENT_ID, -1);
        }

        backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        addExerciseButton.setOnClickListener(v -> {
            ExerciseListHelper.createExerciseDropdown(requireContext(), addExerciseButton,
                    (name, group) -> addNewExercise(name, group)).show();
        });
        loadExercises();
        return view;
    }

    private void loadExercises() {
        if (!isAdded() || getContext() == null) return;

        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryExercises(entryId)
                    .enqueue(new Callback<List<DiaryExercise>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExercise>> call, Response<List<DiaryExercise>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                renumberAndDisplay(response.body());
                            }
                        }
                        @Override
                        public void onFailure(Call<List<DiaryExercise>> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryExercisesForClient(clientId, entryId)
                    .enqueue(new Callback<List<DiaryExercise>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExercise>> call, Response<List<DiaryExercise>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                renumberAndDisplay(response.body());
                            }
                        }
                        @Override
                        public void onFailure(Call<List<DiaryExercise>> call, Throwable t) {}
                    });
        }
    }

    private void renumberAndDisplay(List<DiaryExercise> exercises) {
        currentExercises = exercises;
        for (DiaryExercise de : currentExercises) de.diaryEntryId = entryId;
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
        for (DiaryExercise de : currentExercises) {
            addExerciseRow(de);
        }
        updateAddButton();
        if (needsUpdate) {
            for (DiaryExercise de : currentExercises) {
                if (clientId == -1) {
                    RetrofitClient.getInstance(getContext()).getApiService()
                            .updateDiaryExercise(entryId, de.id, de)
                            .enqueue(new Callback<DiaryExercise>() {
                                @Override public void onResponse(Call<DiaryExercise> call, Response<DiaryExercise> response) {}
                                @Override public void onFailure(Call<DiaryExercise> call, Throwable t) {}
                            });
                } else {
                    RetrofitClient.getInstance(getContext()).getApiService()
                            .updateDiaryExerciseForClient(clientId, entryId, de.id, de)
                            .enqueue(new Callback<DiaryExercise>() {
                                @Override public void onResponse(Call<DiaryExercise> call, Response<DiaryExercise> response) {}
                                @Override public void onFailure(Call<DiaryExercise> call, Throwable t) {}
                            });
                }
            }
        }
    }

    private void addExerciseRow(DiaryExercise exercise) {
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
                        .deleteDiaryExercise(entryId, exercise.id)
                        .enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> response) { loadExercises(); }
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
            } else {
                RetrofitClient.getInstance(getContext()).getApiService()
                        .deleteDiaryExerciseForClient(clientId, entryId, exercise.id)
                        .enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> response) { loadExercises(); }
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
            }
        });

        exercisesContainer.addView(row);
    }

    private void loadSetsForExercise(DiaryExercise exercise, LinearLayout container, TextView countView) {
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSets(exercise.id)
                    .enqueue(new Callback<List<DiaryExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<DiaryExerciseSet> sets = response.body();
                                for (DiaryExerciseSet s : sets) s.diaryExerciseId = exercise.id;
                                countView.setText(String.valueOf(sets.size()));
                                container.removeAllViews();
                                for (DiaryExerciseSet set : sets) {
                                    addSetRow(container, exercise, set);
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSetsForClient(clientId, exercise.id)
                    .enqueue(new Callback<List<DiaryExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<DiaryExerciseSet> sets = response.body();
                                for (DiaryExerciseSet s : sets) s.diaryExerciseId = exercise.id;
                                countView.setText(String.valueOf(sets.size()));
                                container.removeAllViews();
                                for (DiaryExerciseSet set : sets) {
                                    addSetRow(container, exercise, set);
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {}
                    });
        }
    }

    private void addSetRow(LinearLayout container, DiaryExercise exercise, DiaryExerciseSet set) {
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
                RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSets(exercise.id)
                        .enqueue(new Callback<List<DiaryExerciseSet>>() {
                            @Override
                            public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    List<DiaryExerciseSet> allSets = response.body();
                                    for (DiaryExerciseSet s : allSets) s.diaryExerciseId = exercise.id;
                                    for (int i = set.setNumber - 1; i < allSets.size(); i++) {
                                        DiaryExerciseSet s = allSets.get(i);
                                        s.reps = set.reps;
                                        s.weight = set.weight;
                                        RetrofitClient.getInstance(getContext()).getApiService()
                                                .updateDiaryExerciseSet(s.diaryExerciseId, s.id, s)
                                                .enqueue(new Callback<DiaryExerciseSet>() {
                                                    @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {}
                                                    @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                                                });
                                    }
                                    reloadSetsForExercise(exercise, container);
                                }
                            }
                            @Override public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {}
                        });
            } else {
                RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSetsForClient(clientId, exercise.id)
                        .enqueue(new Callback<List<DiaryExerciseSet>>() {
                            @Override
                            public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    List<DiaryExerciseSet> allSets = response.body();
                                    for (DiaryExerciseSet s : allSets) s.diaryExerciseId = exercise.id;
                                    for (int i = set.setNumber - 1; i < allSets.size(); i++) {
                                        DiaryExerciseSet s = allSets.get(i);
                                        s.reps = set.reps;
                                        s.weight = set.weight;
                                        RetrofitClient.getInstance(getContext()).getApiService()
                                                .updateDiaryExerciseSetForClient(clientId, s.diaryExerciseId, s.id, s)
                                                .enqueue(new Callback<DiaryExerciseSet>() {
                                                    @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {}
                                                    @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                                                });
                                    }
                                    reloadSetsForExercise(exercise, container);
                                }
                            }
                            @Override public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {}
                        });
            }
        });

        addSetBtn.setOnClickListener(v -> {
            if (clientId == -1) {
                DiaryExerciseSet newSet = new DiaryExerciseSet(exercise.id, set.setNumber + 1, set.reps, set.weight);
                RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseSet(exercise.id, newSet)
                        .enqueue(new Callback<DiaryExerciseSet>() {
                            @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {
                                reloadSetsForExercise(exercise, container);
                            }
                            @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                        });
            } else {
                DiaryExerciseSet newSet = new DiaryExerciseSet(exercise.id, set.setNumber + 1, set.reps, set.weight);
                RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseSetForClient(clientId, exercise.id, newSet)
                        .enqueue(new Callback<DiaryExerciseSet>() {
                            @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {
                                reloadSetsForExercise(exercise, container);
                            }
                            @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                        });
            }
        });

        deleteSetBtn.setOnClickListener(v -> {
            if (clientId == -1) {
                RetrofitClient.getInstance(getContext()).getApiService().deleteDiaryExerciseSet(exercise.id, set.id)
                        .enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                                reloadSetsForExercise(exercise, container);
                            }
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
            } else {
                RetrofitClient.getInstance(getContext()).getApiService().deleteDiaryExerciseSetForClient(clientId, exercise.id, set.id)
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

    private void updateSetInDb(DiaryExerciseSet set, TextView repsView, TextView weightView, Runnable afterUpdate) {
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService()
                    .updateDiaryExerciseSet(set.diaryExerciseId, set.id, set)
                    .enqueue(new Callback<DiaryExerciseSet>() {
                        @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {
                            if (afterUpdate != null) getActivity().runOnUiThread(afterUpdate);
                            else {
                                if (repsView != null) repsView.setText(String.valueOf(set.reps));
                                if (weightView != null) weightView.setText(String.valueOf(set.weight));
                            }
                        }
                        @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService()
                    .updateDiaryExerciseSetForClient(clientId, set.diaryExerciseId, set.id, set)
                    .enqueue(new Callback<DiaryExerciseSet>() {
                        @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {
                            if (afterUpdate != null) afterUpdate.run();
                        }
                        @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                    });
        }
    }

    private void reloadSetsForExercise(DiaryExercise exercise, LinearLayout container) {
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSets(exercise.id)
                    .enqueue(new Callback<List<DiaryExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<DiaryExerciseSet> sets = response.body();
                                for (DiaryExerciseSet s : sets) s.diaryExerciseId = exercise.id;
                                container.removeAllViews();
                                for (DiaryExerciseSet s : sets) {
                                    addSetRow(container, exercise, s);
                                }
                                View parent = (View) container.getParent();
                                if (parent != null) {
                                    TextView countView = parent.findViewById(R.id.exerciseSetsCount);
                                    if (countView != null) countView.setText(String.valueOf(sets.size()));
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSetsForClient(clientId, exercise.id)
                    .enqueue(new Callback<List<DiaryExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<DiaryExerciseSet> sets = response.body();
                                for (DiaryExerciseSet s : sets) s.diaryExerciseId = exercise.id;
                                container.removeAllViews();
                                for (DiaryExerciseSet s : sets) {
                                    addSetRow(container, exercise, s);
                                }
                                View parent = (View) container.getParent();
                                if (parent != null) {
                                    TextView countView = parent.findViewById(R.id.exerciseSetsCount);
                                    if (countView != null) countView.setText(String.valueOf(sets.size()));
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {}
                    });
        }
    }

    private void changeSetsCount(DiaryExercise exercise, int newCount, LinearLayout container, TextView countView) {
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSets(exercise.id)
                    .enqueue(new Callback<List<DiaryExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<DiaryExerciseSet> currentSets = response.body();
                                for (DiaryExerciseSet s : currentSets) s.diaryExerciseId = exercise.id;
                                int currentCount = currentSets.size();
                                if (newCount > currentCount) {
                                    int defaultReps = 1;
                                    float defaultWeight = 20f;
                                    if (currentCount > 0) {
                                        DiaryExerciseSet last = currentSets.get(currentCount - 1);
                                        defaultReps = last.reps;
                                        defaultWeight = last.weight;
                                    }
                                    for (int i = currentCount + 1; i <= newCount; i++) {
                                        DiaryExerciseSet newSet = new DiaryExerciseSet(exercise.id, i, defaultReps, defaultWeight);
                                        RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseSet(exercise.id, newSet)
                                                .enqueue(new Callback<DiaryExerciseSet>() {
                                                    @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {}
                                                    @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                                                });
                                    }
                                } else if (newCount < currentCount) {
                                    for (int i = currentCount; i > newCount; i--) {
                                        DiaryExerciseSet toDelete = currentSets.get(i - 1);
                                        RetrofitClient.getInstance(getContext()).getApiService().deleteDiaryExerciseSet(exercise.id, toDelete.id)
                                                .enqueue(new Callback<Void>() {
                                                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                                                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                                                });
                                    }
                                }
                                reloadSetsForExercise(exercise, container);
                            }
                        }
                        @Override public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getDiaryExerciseSetsForClient(clientId, exercise.id)
                    .enqueue(new Callback<List<DiaryExerciseSet>>() {
                        @Override
                        public void onResponse(Call<List<DiaryExerciseSet>> call, Response<List<DiaryExerciseSet>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<DiaryExerciseSet> currentSets = response.body();
                                for (DiaryExerciseSet s : currentSets) s.diaryExerciseId = exercise.id;
                                int currentCount = currentSets.size();
                                if (newCount > currentCount) {
                                    int defaultReps = 1;
                                    float defaultWeight = 20f;
                                    if (currentCount > 0) {
                                        DiaryExerciseSet last = currentSets.get(currentCount - 1);
                                        defaultReps = last.reps;
                                        defaultWeight = last.weight;
                                    }
                                    for (int i = currentCount + 1; i <= newCount; i++) {
                                        DiaryExerciseSet newSet = new DiaryExerciseSet(exercise.id, i, defaultReps, defaultWeight);
                                        RetrofitClient.getInstance(getContext()).getApiService()
                                                .createDiaryExerciseSetForClient(clientId, exercise.id, newSet)
                                                .enqueue(new Callback<DiaryExerciseSet>() {
                                                    @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {}
                                                    @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {}
                                                });
                                    }
                                } else if (newCount < currentCount) {
                                    for (int i = currentCount; i > newCount; i--) {
                                        DiaryExerciseSet toDelete = currentSets.get(i - 1);
                                        RetrofitClient.getInstance(getContext()).getApiService()
                                                .deleteDiaryExerciseSetForClient(clientId, exercise.id, toDelete.id)
                                                .enqueue(new Callback<Void>() {
                                                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                                                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                                                });
                                    }
                                }
                                reloadSetsForExercise(exercise, container);
                            }
                        }
                        @Override public void onFailure(Call<List<DiaryExerciseSet>> call, Throwable t) {}
                    });
        }
    }

    private void updateExerciseName(DiaryExercise exercise, String newName, String newGroup) {
        exercise.name = newName;
        exercise.muscleGroup = newGroup;
        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService()
                    .updateDiaryExercise(entryId, exercise.id, exercise)
                    .enqueue(new Callback<DiaryExercise>() {
                        @Override public void onResponse(Call<DiaryExercise> call, Response<DiaryExercise> response) {
                            loadExercises();
                        }
                        @Override public void onFailure(Call<DiaryExercise> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService()
                    .updateDiaryExerciseForClient(clientId, entryId, exercise.id, exercise)
                    .enqueue(new Callback<DiaryExercise>() {
                        @Override public void onResponse(Call<DiaryExercise> call, Response<DiaryExercise> response) {
                            loadExercises();
                        }
                        @Override public void onFailure(Call<DiaryExercise> call, Throwable t) {}
                    });
        }
    }

    private void addNewExercise(String exerciseName, String muscleGroup) {
        int maxOrder = 0;
        for (DiaryExercise e : currentExercises) {
            if (e.orderIndex > maxOrder) maxOrder = e.orderIndex;
        }
        int nextOrder = maxOrder + 1;

        if (clientId == -1) {
            DiaryExercise newEx = new DiaryExercise(entryId, nextOrder, exerciseName, muscleGroup);
            RetrofitClient.getInstance(getContext()).getApiService().createDiaryExercise(entryId, newEx)
                    .enqueue(new Callback<DiaryExercise>() {
                        @Override
                        public void onResponse(Call<DiaryExercise> call, Response<DiaryExercise> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                int exerciseId = response.body().id;
                                DiaryExerciseSet defaultSet = new DiaryExerciseSet(exerciseId, 1, 1, 20f);
                                RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseSet(exerciseId, defaultSet)
                                        .enqueue(new Callback<DiaryExerciseSet>() {
                                            @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {
                                                loadExercises();
                                            }
                                            @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {
                                                loadExercises();
                                            }
                                        });
                            }
                        }
                        @Override public void onFailure(Call<DiaryExercise> call, Throwable t) {}
                    });
        } else {
            DiaryExercise newEx = new DiaryExercise(entryId, nextOrder, exerciseName, muscleGroup);
            RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseForClient(clientId, entryId, newEx)
                    .enqueue(new Callback<DiaryExercise>() {
                        @Override
                        public void onResponse(Call<DiaryExercise> call, Response<DiaryExercise> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                int exerciseId = response.body().id;
                                DiaryExerciseSet defaultSet = new DiaryExerciseSet(exerciseId, 1, 1, 20f);
                                RetrofitClient.getInstance(getContext()).getApiService().createDiaryExerciseSetForClient(clientId, exerciseId, defaultSet)
                                        .enqueue(new Callback<DiaryExerciseSet>() {
                                            @Override public void onResponse(Call<DiaryExerciseSet> call, Response<DiaryExerciseSet> response) {
                                                loadExercises();
                                            }
                                            @Override public void onFailure(Call<DiaryExerciseSet> call, Throwable t) {
                                                loadExercises();
                                            }
                                        });
                            }
                        }
                        @Override public void onFailure(Call<DiaryExercise> call, Throwable t) {}
                    });
        }
    }

    private void updateAddButton() {
        addExerciseButton.setVisibility(currentExercises.size() < 10 ? View.VISIBLE : View.GONE);
    }
}