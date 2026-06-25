package com.example.fitnesshelper.fragments.trainer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesshelper.adapters.ClientAdapter;
import com.example.fitnesshelper.adapters.ClientItem;
import com.example.fitnesshelper.R;
import com.example.fitnesshelper.db.User;
import com.example.fitnesshelper.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientsFragment extends Fragment {

    private RecyclerView clientsRecyclerView;
    private TextView emptyText;
    private ClientAdapter adapter;
    private List<ClientItem> clientItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clients, container, false);
        clientsRecyclerView = view.findViewById(R.id.clientsRecyclerView);
        emptyText = view.findViewById(R.id.emptyClientsText);
        clientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClientAdapter(clientItems, this::assignClient, this::unassignClient);
        clientsRecyclerView.setAdapter(adapter);
        loadClients();
        return view;
    }

    private void loadClients() {
        if (!isAdded()) return;
        RetrofitClient.getInstance(getContext()).getApiService().getTrainerClients()
                .enqueue(new Callback<Map<String, List<Map<String, Object>>>>() {
                    @Override
                    public void onResponse(Call<Map<String, List<Map<String, Object>>>> call,
                                           Response<Map<String, List<Map<String, Object>>>> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            List<ClientItem> items = new ArrayList<>();

                            items.add(new ClientItem(true, null, "Мои клиенты"));
                            List<Map<String, Object>> myClients = response.body().get("myClients");
                            if (myClients != null) {
                                for (Map<String, Object> map : myClients) {
                                    items.add(new ClientItem(false, User.fromMap(map), null));
                                }
                            }

                            items.add(new ClientItem(true, null, "Самостоятельные клиенты"));
                            List<Map<String, Object>> independentClients = response.body().get("independentClients");
                            if (independentClients != null) {
                                for (Map<String, Object> map : independentClients) {
                                    items.add(new ClientItem(false, User.fromMap(map), null));
                                }
                            }

                            clientItems.clear();
                            clientItems.addAll(items);
                            adapter.notifyDataSetChanged();

                            // Проверяем, есть ли реальные клиенты (не заголовки)
                            boolean hasClients = false;
                            for (ClientItem item : clientItems) {
                                if (!item.isHeader) {
                                    hasClients = true;
                                    break;
                                }
                            }
                            if (hasClients) {
                                clientsRecyclerView.setVisibility(View.VISIBLE);
                                emptyText.setVisibility(View.GONE);
                            } else {
                                clientsRecyclerView.setVisibility(View.GONE);
                                emptyText.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, List<Map<String, Object>>>> call, Throwable t) {
                        if (isAdded())
                            Toast.makeText(getContext(), "Ошибка загрузки клиентов", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void assignClient(User client) {
        if (!isAdded()) return;
        RetrofitClient.getInstance(getContext()).getApiService().assignClient(client.id)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (isAdded()) {
                            if (response.isSuccessful()) {
                                loadClients();
                            } else {
                                showError(response);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (isAdded())
                            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unassignClient(User client) {
        if (!isAdded()) return;
        RetrofitClient.getInstance(getContext()).getApiService().unassignClient(client.id)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (isAdded()) {
                            if (response.isSuccessful()) {
                                loadClients();
                            } else {
                                showError(response);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (isAdded())
                            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showError(Response<?> response) {
        if (!isAdded()) return;
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Пустое тело ошибки";
            Toast.makeText(getContext(), "Ошибка: " + errorBody, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Ошибка выполнения", Toast.LENGTH_SHORT).show();
        }
    }
}