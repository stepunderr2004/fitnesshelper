package com.example.fitnesshelper.activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.adapters.UserAdapter;
import com.example.fitnesshelper.db.User;
import com.example.fitnesshelper.helpers.SessionManager;
import com.example.fitnesshelper.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView usersRecyclerView;
    private UserAdapter adapter;
    private Button addUserButton, logoutButton;

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        addUserButton = findViewById(R.id.addUserButton);
        logoutButton = findViewById(R.id.logoutButton);

        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(new ArrayList<>(), this::editUser, this::deleteUser);
        usersRecyclerView.setAdapter(adapter);

        // Инициализация отслеживания сети
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    SessionManager.clearSession(AdminActivity.this);
                    RetrofitClient.resetInstance();
                    Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        };

        if (!isNetworkAvailable()) {
            new AlertDialog.Builder(this)
                    .setTitle("Нет сети")
                    .setMessage("Для работы приложения требуется подключение к интернету.")
                    .setCancelable(false)
                    .setPositiveButton("Выход", (dialog, which) -> finish())
                    .show();
            return;
        }

        addUserButton.setOnClickListener(v -> showEditDialog(null));
        logoutButton.setOnClickListener(v -> {
            SessionManager.clearSession(this);
            RetrofitClient.resetInstance();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void loadUsers() {
        RetrofitClient.getInstance(this).getApiService().getAllUsers()
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call,
                                           Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<User> users = new ArrayList<>();
                            for (Map<String, Object> map : response.body()) {
                                User user = mapToUser(map);
                                if (!"ADMIN".equals(user.role)) {
                                    users.add(user);
                                }
                            }
                            adapter.setUsers(users);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        Toast.makeText(AdminActivity.this, "Ошибка загрузки пользователей", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private User mapToUser(Map<String, Object> map) {
        User user = new User();
        user.id = map.get("id") != null ? ((Double) map.get("id")).intValue() : 0;
        user.username = (String) map.get("username");
        user.name = (String) map.get("name");
        user.age = (String) map.get("age");
        user.gender = (String) map.get("gender");
        user.role = (String) map.get("role");
        user.trainerEnabled = map.get("trainerEnabled") != null && (Boolean) map.get("trainerEnabled");
        user.trainerId = map.get("trainerId") != null ? ((Double) map.get("trainerId")).intValue() : null;
        return user;
    }

    private void editUser(User user) {
        showEditDialog(user);
    }

    private void deleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить пользователя?")
                .setMessage("Все данные пользователя " + user.username + " будут удалены.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    RetrofitClient.getInstance(this).getApiService().deleteUser(user.id)
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        loadUsers();
                                        Toast.makeText(AdminActivity.this, "Пользователь удалён", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Toast.makeText(AdminActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_edit, null);
        builder.setView(dialogView);

        final EditText usernameInput = dialogView.findViewById(R.id.editUsername);
        final EditText passwordInput = dialogView.findViewById(R.id.editPassword);
        final EditText nameInput = dialogView.findViewById(R.id.editName);
        final EditText ageInput = dialogView.findViewById(R.id.editAge);
        final RadioGroup genderGroup = dialogView.findViewById(R.id.editGenderGroup);
        final Spinner roleSpinner = dialogView.findViewById(R.id.editRoleSpinner);
        final Spinner trainerSpinner = dialogView.findViewById(R.id.editTrainerSpinner);
        final TextView trainerLabel = dialogView.findViewById(R.id.trainerLabel);

        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        loadTrainerSpinner(trainerSpinner, user != null ? user.trainerId : null);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = (String) parent.getItemAtPosition(position);
                if ("TRAINER".equals(selectedRole)) {
                    trainerLabel.setVisibility(View.GONE);
                    trainerSpinner.setVisibility(View.GONE);
                } else {
                    trainerLabel.setVisibility(View.VISIBLE);
                    trainerSpinner.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (user != null) {
            usernameInput.setText(user.username);
            nameInput.setText(user.name);
            ageInput.setText(user.age);
            if ("male".equals(user.gender)) {
                genderGroup.check(R.id.editMale);
            } else {
                genderGroup.check(R.id.editFemale);
            }
            String[] roles = getResources().getStringArray(R.array.roles_array);
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equals(user.role)) {
                    roleSpinner.setSelection(i);
                    break;
                }
            }
            if ("TRAINER".equals(user.role)) {
                trainerLabel.setVisibility(View.GONE);
                trainerSpinner.setVisibility(View.GONE);
            }
        }

        builder.setTitle(user == null ? "Добавить пользователя" : "Редактировать пользователя");
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String name = nameInput.getText().toString().trim();
            String age = ageInput.getText().toString().trim();
            int checkedGenderId = genderGroup.getCheckedRadioButtonId();
            String gender = (checkedGenderId == R.id.editMale) ? "male" : "female";
            String role = roleSpinner.getSelectedItem().toString();
            User trainer = null;
            if (!"TRAINER".equals(role)) {
                trainer = (User) trainerSpinner.getSelectedItem();
            }
            Integer trainerId = (trainer != null) ? trainer.id : null;

            if (username.isEmpty() || (user == null && password.isEmpty())) {
                Toast.makeText(this, "Логин и пароль обязательны", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("username", username);
            body.put("password", password);
            body.put("name", name);
            body.put("age", age);
            body.put("gender", gender);
            body.put("role", role);
            body.put("trainerId", trainerId);

            Log.d("ADMIN_UPDATE", "Sending to " + (user == null ? "create" : "update/" + user.id) + ": " + new com.google.gson.Gson().toJson(body));

            if (user == null) {
                RetrofitClient.getInstance(this).getApiService().createUser(body)
                        .enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                if (response.isSuccessful()) {
                                    loadUsers();
                                    Toast.makeText(AdminActivity.this, "Пользователь создан", Toast.LENGTH_SHORT).show();
                                } else {
                                    showError(response);
                                }
                            }
                            @Override
                            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                Toast.makeText(AdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                RetrofitClient.getInstance(this).getApiService().updateUserByAdmin(user.id, body)
                        .enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                if (response.isSuccessful()) {
                                    loadUsers();
                                    Toast.makeText(AdminActivity.this, "Пользователь обновлён", Toast.LENGTH_SHORT).show();
                                } else {
                                    showError(response);
                                }
                            }
                            @Override
                            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                Toast.makeText(AdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showError(Response<?> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
            Toast.makeText(AdminActivity.this, "Ошибка " + response.code() + ": " + errorBody, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(AdminActivity.this, "Ошибка " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTrainerSpinner(Spinner spinner, Integer selectedTrainerId) {
        RetrofitClient.getInstance(this).getApiService().getAllUsers()
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call,
                                           Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<User> trainers = new ArrayList<>();
                            trainers.add(null);
                            for (Map<String, Object> map : response.body()) {
                                User user = mapToUser(map);
                                if ("TRAINER".equals(user.role)) {
                                    trainers.add(user);
                                }
                            }
                            ArrayAdapter<User> adapter = new ArrayAdapter<User>(AdminActivity.this,
                                    android.R.layout.simple_spinner_item, trainers) {
                                @NonNull
                                @Override
                                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                                    if (getItem(position) == null) {
                                        TextView view = (TextView) LayoutInflater.from(getContext())
                                                .inflate(android.R.layout.simple_spinner_item, parent, false);
                                        view.setText("Нет тренера");
                                        view.setTypeface(null, android.graphics.Typeface.NORMAL);
                                        return view;
                                    }
                                    View view = super.getView(position, convertView, parent);
                                    if (view instanceof TextView) {
                                        ((TextView) view).setText(((User) getItem(position)).username);
                                        ((TextView) view).setTypeface(null, android.graphics.Typeface.BOLD);
                                    }
                                    return view;
                                }
                                @Override
                                public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                                    if (getItem(position) == null) {
                                        TextView view = (TextView) LayoutInflater.from(getContext())
                                                .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
                                        view.setText("Нет тренера");
                                        view.setTypeface(null, android.graphics.Typeface.NORMAL);
                                        return view;
                                    }
                                    View view = super.getDropDownView(position, convertView, parent);
                                    if (view instanceof TextView) {
                                        ((TextView) view).setText(((User) getItem(position)).username);
                                        ((TextView) view).setTypeface(null, android.graphics.Typeface.BOLD);
                                    }
                                    return view;
                                }
                            };
                            spinner.setAdapter(adapter);
                            if (selectedTrainerId != null) {
                                for (int i = 0; i < trainers.size(); i++) {
                                    if (trainers.get(i) != null && trainers.get(i).id == selectedTrainerId) {
                                        spinner.setSelection(i);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        Toast.makeText(AdminActivity.this, "Не удалось загрузить список тренеров", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}