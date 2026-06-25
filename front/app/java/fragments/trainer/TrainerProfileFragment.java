package com.example.fitnesshelper.fragments.trainer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.helpers.SessionManager;
import com.example.fitnesshelper.activities.LoginActivity;
import com.example.fitnesshelper.activities.TrainerActivity;
import com.example.fitnesshelper.db.AppDatabase;
import com.example.fitnesshelper.db.User;
import com.example.fitnesshelper.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainerProfileFragment extends Fragment {

    private EditText nameEditText, ageEditText;
    private RadioGroup genderRadioGroup;
    private Button saveButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable retryFillRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trainer_profile, container, false);
        nameEditText = view.findViewById(R.id.trainerName);
        ageEditText = view.findViewById(R.id.trainerAge);
        genderRadioGroup = view.findViewById(R.id.trainerGenderGroup);
        saveButton = view.findViewById(R.id.saveProfileButton);

        fillUserData();

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

            User user = TrainerActivity.currentUser;
            if (user == null) {
                Toast.makeText(getContext(), "Данные ещё не загружены", Toast.LENGTH_SHORT).show();
                return;
            }
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
                            } else {
                                Toast.makeText(getContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        Button logoutButton = view.findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Выход")
                    .setMessage("Вы уверены, что хотите выйти?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        SessionManager.clearSession(getContext());
                        RetrofitClient.resetInstance();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fillUserData();
    }

    private void fillUserData() {
        if (TrainerActivity.currentUser != null) {
            nameEditText.setText(TrainerActivity.currentUser.name);
            ageEditText.setText(TrainerActivity.currentUser.age);
            if ("male".equals(TrainerActivity.currentUser.gender)) {
                genderRadioGroup.check(R.id.trainerMale);
            } else {
                genderRadioGroup.check(R.id.trainerFemale);
            }
            // Останавливаем повторные попытки
            if (retryFillRunnable != null) {
                handler.removeCallbacks(retryFillRunnable);
            }
        } else {
            // Данные ещё не загружены – попробуем снова через 300 мс
            if (retryFillRunnable == null) {
                retryFillRunnable = () -> {
                    if (isAdded()) fillUserData();
                };
            }
            handler.postDelayed(retryFillRunnable, 300);
        }
    }
}