package com.example.fitnesshelper.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.db.AppDatabase;
import com.example.fitnesshelper.db.TrainingDay;
import com.example.fitnesshelper.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProgramDetailFragment extends Fragment {

    private static final String ARG_PROGRAM_ID = "program_id";
    private static final String ARG_CLIENT_ID = "client_id";

    private int programId;
    private int clientId = -1;
    private LinearLayout daysContainer;
    private Button addDayButton;

    public static ProgramDetailFragment newInstance(int programId, int clientId) {
        ProgramDetailFragment fragment = new ProgramDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PROGRAM_ID, programId);
        args.putInt(ARG_CLIENT_ID, clientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_program_detail, container, false);
        Button backBtn = view.findViewById(R.id.backButton);
        daysContainer = view.findViewById(R.id.daysContainer);
        addDayButton = view.findViewById(R.id.addDayButton);

        if (getArguments() != null) {
            programId = getArguments().getInt(ARG_PROGRAM_ID);
            clientId = getArguments().getInt(ARG_CLIENT_ID, -1);
        }

        backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        addDayButton.setOnClickListener(v -> showAddDayDialog());
        loadDays();
        return view;
    }

    private void loadDays() {
        if (!isAdded() || getContext() == null) return;

        if (clientId == -1) {
            RetrofitClient.getInstance(getContext()).getApiService().getTrainingDays(programId)
                    .enqueue(new Callback<List<TrainingDay>>() {
                        @Override
                        public void onResponse(Call<List<TrainingDay>> call, Response<List<TrainingDay>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                displayDays(response.body());
                            }
                        }
                        @Override
                        public void onFailure(Call<List<TrainingDay>> call, Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(getContext()).getApiService().getTrainingDaysForClient(clientId, programId)
                    .enqueue(new Callback<List<TrainingDay>>() {
                        @Override
                        public void onResponse(Call<List<TrainingDay>> call, Response<List<TrainingDay>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                displayDays(response.body());
                            }
                        }
                        @Override
                        public void onFailure(Call<List<TrainingDay>> call, Throwable t) {}
                    });
        }
    }

    private void displayDays(List<TrainingDay> days) {
        daysContainer.removeAllViews();
        for (TrainingDay day : days) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button dayBtn = new Button(getContext());
            dayBtn.setText(day.name);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            dayBtn.setLayoutParams(btnParams);
            dayBtn.setOnClickListener(v -> openDay(day));

            ImageButton deleteBtn = new ImageButton(getContext());
            deleteBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            deleteBtn.setBackgroundResource(android.R.color.transparent);
            deleteBtn.setOnClickListener(v -> confirmDeleteDay(day));

            row.addView(dayBtn);
            row.addView(deleteBtn);
            daysContainer.addView(row);
        }
        addDayButton.setVisibility(days.size() < 8 ? View.VISIBLE : View.GONE);
    }

    private void confirmDeleteDay(TrainingDay day) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удалить день?")
                .setMessage("Вы уверены, что хотите удалить '" + day.name + "'?")
                .setPositiveButton("Удалить", (d, w) -> {
                    if (clientId == -1) {
                        RetrofitClient.getInstance(getContext()).getApiService()
                                .deleteTrainingDay(programId, day.id)
                                .enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (response.isSuccessful()) loadDays();
                                    }
                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {}
                                });
                    } else {
                        RetrofitClient.getInstance(getContext()).getApiService()
                                .deleteTrainingDayForClient(clientId, programId, day.id)
                                .enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (response.isSuccessful()) loadDays();
                                    }
                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {}
                                });
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void openDay(TrainingDay day) {
        Fragment fragment = TrainingDayDetailFragment.newInstance(day.id, clientId);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showAddDayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Название тренировочного дня");
        final EditText input = new EditText(getContext());
        input.setHint("День 1");
        builder.setView(input);
        builder.setPositiveButton("Создать", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) return;
            TrainingDay day = new TrainingDay(programId, name);
            if (clientId == -1) {
                RetrofitClient.getInstance(getContext()).getApiService().createTrainingDay(programId, day)
                        .enqueue(new Callback<TrainingDay>() {
                            @Override
                            public void onResponse(Call<TrainingDay> call, Response<TrainingDay> response) {
                                if (response.isSuccessful()) loadDays();
                            }
                            @Override
                            public void onFailure(Call<TrainingDay> call, Throwable t) {}
                        });
            } else {
                RetrofitClient.getInstance(getContext()).getApiService().createTrainingDayForClient(clientId, programId, day)
                        .enqueue(new Callback<TrainingDay>() {
                            @Override
                            public void onResponse(Call<TrainingDay> call, Response<TrainingDay> response) {
                                if (response.isSuccessful()) loadDays();
                            }
                            @Override
                            public void onFailure(Call<TrainingDay> call, Throwable t) {}
                        });
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}