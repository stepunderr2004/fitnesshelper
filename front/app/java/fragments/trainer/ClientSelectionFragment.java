package com.example.fitnesshelper.fragments.trainer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.fragments.ProgramsFragment;
import com.example.fitnesshelper.fragments.StatsFragment;
import com.example.fitnesshelper.network.RetrofitClient;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientSelectionFragment extends Fragment {

    private static final String ARG_MODE = "mode"; // "programs" или "diary"
    private LinearLayout clientsContainer;
    private String mode;

    public static ClientSelectionFragment newInstance(String mode) {
        ClientSelectionFragment fragment = new ClientSelectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_client_selection, container, false);
        clientsContainer = view.findViewById(R.id.clientsContainer);

        if (getArguments() != null) {
            mode = getArguments().getString(ARG_MODE);
        }

        loadClients();
        return view;
    }

    private void loadClients() {
        RetrofitClient.getInstance(getContext()).getApiService().getTrainerClients()
                .enqueue(new Callback<Map<String, List<Map<String, Object>>>>() {
                    @Override
                    public void onResponse(Call<Map<String, List<Map<String, Object>>>> call,
                                           Response<Map<String, List<Map<String, Object>>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Map<String, Object>> myClients = response.body().get("myClients");
                            clientsContainer.removeAllViews();
                            if (myClients == null || myClients.isEmpty()) {
                                TextView emptyText = new TextView(getContext());
                                emptyText.setText("Нет клиентов");
                                emptyText.setTextSize(18);
                                emptyText.setPadding(16, 16, 16, 16);
                                clientsContainer.addView(emptyText);
                            } else {
                                for (Map<String, Object> map : myClients) {
                                    int clientId = ((Double) map.get("id")).intValue();
                                    String clientName = (String) map.get("name");
                                    if (clientName == null || clientName.isEmpty()) {
                                        clientName = (String) map.get("username");
                                    }
                                    Button clientBtn = new Button(getContext());
                                    clientBtn.setText(clientName);
                                    clientBtn.setOnClickListener(v -> openClientData(clientId));
                                    clientsContainer.addView(clientBtn);
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "Ошибка загрузки клиентов", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, List<Map<String, Object>>>> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openClientData(int clientId) {
        if ("programs".equals(mode)) {
            ProgramsFragment fragment = ProgramsFragment.newInstance(clientId);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else if ("diary".equals(mode)) {
            StatsFragment fragment = StatsFragment.newInstance(clientId);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}