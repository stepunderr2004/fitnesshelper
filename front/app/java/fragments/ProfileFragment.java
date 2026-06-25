package com.example.fitnesshelper.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.helpers.SessionManager;
import com.example.fitnesshelper.activities.LoginActivity;
import com.example.fitnesshelper.activities.MainActivity;
import com.example.fitnesshelper.db.AppDatabase;
import com.example.fitnesshelper.db.User;
import com.example.fitnesshelper.network.RetrofitClient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private EditText nameEditText, ageEditText;
    private RadioGroup genderRadioGroup;
    private TextView trainerStatusText;
    private Button saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_settings, container, false);
        nameEditText = view.findViewById(R.id.nameEditText);
        ageEditText = view.findViewById(R.id.ageEditText);
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup);
        trainerStatusText = view.findViewById(R.id.trainerStatusText);
        saveButton = view.findViewById(R.id.saveProfileButton);

        if (MainActivity.currentUser != null) {
            nameEditText.setText(MainActivity.currentUser.name);
            ageEditText.setText(MainActivity.currentUser.age);
            if (MainActivity.currentUser.gender.equals("male")) {
                genderRadioGroup.check(R.id.maleRadioButton);
            } else {
                genderRadioGroup.check(R.id.femaleRadioButton);
            }
            updateTrainerStatus();
        }

        saveButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String age = ageEditText.getText().toString().trim();
            int checkedId = genderRadioGroup.getCheckedRadioButtonId();
            if (checkedId == -1) {
                Toast.makeText(getContext(), "Выберите пол", Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton selectedGender = view.findViewById(checkedId);
            String genderValue = selectedGender.getText().toString().equals("Мужской") ? "male" : "female";

            User user = MainActivity.currentUser;
            user.name = name;
            user.age = age;
            user.gender = genderValue;

            new Thread(() -> {
                AppDatabase.getInstance(getContext()).userDao().updateUser(user);
            }).start();

            RetrofitClient.getInstance(getContext()).getApiService()
                    .updateUser(user.id, user)
                    .enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Профиль сохранён", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            Toast.makeText(getContext(), "Ошибка сети при сохранении", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        Button statisticsButton = view.findViewById(R.id.statisticsButton);
        statisticsButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileStatsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        Button logoutButton = view.findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Выход")
                        .setMessage("Вы уверены, что хотите выйти? Все локальные данные будут удалены.")
                        .setPositiveButton("Да", (dialog, which) -> {
                            SessionManager.clearSession(getContext());
                            new Thread(() -> {
                                AppDatabase db = AppDatabase.getInstance(getContext());
                                int userId = MainActivity.currentUserId;
                                db.programDao().deleteAllForUser(userId);
                                db.trainingDateDao().deleteAllForUser(userId);
                                db.trainingSessionDao().deleteAllForUser(userId);
                                db.diaryEntryDao().deleteAllForUser(userId);
                            }).start();
                            RetrofitClient.resetInstance();
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            getActivity().finish();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем статус тренера при возвращении на фрагмент
        updateTrainerStatus();
    }

    private void updateTrainerStatus() {
        User user = MainActivity.currentUser;
        if (user == null) return;
        if (user.trainerId != null && user.trainerId != 0) {
            // Загружаем имя тренера через getUser (теперь возвращает Map)
            RetrofitClient.getInstance(getContext()).getApiService().getUser(user.trainerId)
                    .enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Map<String, Object> trainerMap = response.body();
                                String trainerName = (String) trainerMap.get("name");
                                if (trainerName == null || trainerName.isEmpty()) {
                                    trainerName = (String) trainerMap.get("username");
                                }
                                trainerStatusText.setText("Вы занимаетесь с тренером " + trainerName);
                            } else {
                                trainerStatusText.setText("Вы занимаетесь с тренером");
                            }
                        }
                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            trainerStatusText.setText("Вы занимаетесь с тренером");
                        }
                    });
        } else {
            trainerStatusText.setText("Вы не занимаетесь с тренером");
        }
    }
}