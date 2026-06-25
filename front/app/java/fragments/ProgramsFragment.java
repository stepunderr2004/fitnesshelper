package com.example.fitnesshelper.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.activities.MainActivity;
import com.example.fitnesshelper.db.*;
import com.example.fitnesshelper.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProgramsFragment extends Fragment {

    private static final String ARG_CLIENT_ID = "client_id";
    private LinearLayout programsContainer;
    private Button addButton;
    private int clientId = -1;

    public static ProgramsFragment newInstance(int clientId) {
        ProgramsFragment fragment = new ProgramsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIENT_ID, clientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_programs, container, false);
        programsContainer = view.findViewById(R.id.programsContainer);
        addButton = view.findViewById(R.id.addProgramButton);

        if (getArguments() != null && getArguments().containsKey(ARG_CLIENT_ID)) {
            clientId = getArguments().getInt(ARG_CLIENT_ID);
        }

        addButton.setOnClickListener(v -> showAddProgramDialog());
        loadPrograms();
        return view;
    }

    private void loadPrograms() {
        if (!isAdded() || getContext() == null) return;

        if (clientId == -1) {
            // === КЛИЕНТ: загружаем с сервера, сохраняем в Room с дочерними элементами ===
            RetrofitClient.getInstance(getContext()).getApiService().getPrograms()
                    .enqueue(new Callback<List<Program>>() {
                        @Override
                        public void onResponse(Call<List<Program>> call, Response<List<Program>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<Program> programs = response.body();
                                // Фоновая загрузка дней, упражнений, подходов
                                new Thread(() -> {
                                    AppDatabase db = AppDatabase.getInstance(getContext());
                                    for (Program p : programs) {
                                        p.userId = MainActivity.currentUserId;
                                        long localProgramId = db.programDao().insertProgramWithReturn(p);
                                        loadAndSaveTrainingDays((int) localProgramId, p.id);
                                    }
                                }).start();
                                displayPrograms(programs);
                            } else {
                                Toast.makeText(getContext(), "Ошибка загрузки программ", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<List<Program>> call, Throwable t) {
                            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // === ТРЕНЕР: программы клиента ===
            RetrofitClient.getInstance(getContext()).getApiService().getProgramsForClient(clientId)
                    .enqueue(new Callback<List<Program>>() {
                        @Override
                        public void onResponse(Call<List<Program>> call, Response<List<Program>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<Program> programs = response.body();
                                for (Program p : programs) p.userId = clientId;
                                displayPrograms(programs);
                            } else {
                                Toast.makeText(getContext(), "Ошибка загрузки программ", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<List<Program>> call, Throwable t) {
                            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void loadAndSaveTrainingDays(int localProgramId, int serverProgramId) {
        try {
            Response<List<TrainingDay>> resp = RetrofitClient.getInstance(getContext())
                    .getApiService().getTrainingDays(serverProgramId).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                AppDatabase db = AppDatabase.getInstance(getContext());
                db.trainingDayDao().deleteAllForProgram(localProgramId);
                for (TrainingDay day : resp.body()) {
                    day.programId = localProgramId;
                    long localDayId = db.trainingDayDao().insertWithReturn(day);
                    loadAndSaveExercises((int) localDayId, day.id);
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadAndSaveExercises(int localDayId, int serverDayId) {
        try {
            Response<List<Exercise>> resp = RetrofitClient.getInstance(getContext())
                    .getApiService().getExercises(serverDayId).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                AppDatabase db = AppDatabase.getInstance(getContext());
                for (Exercise ex : resp.body()) {
                    ex.trainingDayId = localDayId;
                    long localExerciseId = db.exerciseDao().insertExerciseWithReturn(ex);
                    loadAndSaveSets((int) localExerciseId, ex.id);
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadAndSaveSets(int localExerciseId, int serverExerciseId) {
        try {
            Response<List<ExerciseSet>> resp = RetrofitClient.getInstance(getContext())
                    .getApiService().getExerciseSets(serverExerciseId).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                AppDatabase db = AppDatabase.getInstance(getContext());
                for (ExerciseSet set : resp.body()) {
                    set.exerciseId = localExerciseId;
                    db.exerciseSetDao().insertSet(set);
                }
            }
        } catch (Exception ignored) {}
    }

    private void displayPrograms(List<Program> programs) {
        programsContainer.removeAllViews();
        for (Program p : programs) {
            addProgramRow(p);
        }
        addButton.setVisibility(programs.size() < 3 ? View.VISIBLE : View.GONE);
    }

    private void addProgramRow(Program program) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_program, programsContainer, false);
        TextView nameView = row.findViewById(R.id.programName);
        ImageButton deleteBtn = row.findViewById(R.id.deleteButton);

        nameView.setText(program.name);
        nameView.setOnClickListener(v -> {
            Fragment detailFragment = ProgramDetailFragment.newInstance(program.id, clientId);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        deleteBtn.setOnClickListener(v -> {
            if (clientId == -1) {
                RetrofitClient.getInstance(getContext()).getApiService().deleteProgram(program.id)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    new Thread(() -> {
                                        AppDatabase.getInstance(getContext()).programDao().deleteProgram(program);
                                    }).start();
                                    loadPrograms();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {}
                        });
            } else {
                RetrofitClient.getInstance(getContext()).getApiService().deleteProgramForClient(clientId, program.id)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) loadPrograms();
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {}
                        });
            }
        });

        programsContainer.addView(row);
    }

    private void showAddProgramDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Название программы");
        final EditText input = new EditText(getContext());
        input.setHint("Введите название");
        builder.setView(input);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show();
                return;
            }
            Program program = new Program(clientId == -1 ? MainActivity.currentUserId : clientId, name);
            if (clientId == -1) {
                RetrofitClient.getInstance(getContext()).getApiService().createProgram(program)
                        .enqueue(new Callback<Program>() {
                            @Override
                            public void onResponse(Call<Program> call, Response<Program> response) {
                                if (response.isSuccessful()) loadPrograms();
                            }
                            @Override
                            public void onFailure(Call<Program> call, Throwable t) {}
                        });
            } else {
                RetrofitClient.getInstance(getContext()).getApiService().createProgramForClient(clientId, program)
                        .enqueue(new Callback<Program>() {
                            @Override
                            public void onResponse(Call<Program> call, Response<Program> response) {
                                if (response.isSuccessful()) loadPrograms();
                            }
                            @Override
                            public void onFailure(Call<Program> call, Throwable t) {}
                        });
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}