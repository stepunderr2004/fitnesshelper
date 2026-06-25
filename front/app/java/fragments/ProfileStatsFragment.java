package com.example.fitnesshelper.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.activities.MainActivity;
import com.example.fitnesshelper.db.AppDatabase;
import com.example.fitnesshelper.db.TrainingSession;

public class ProfileStatsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_stats, container, false);
        Button backBtn = view.findViewById(R.id.backButton);
        TextView lastTimeView = view.findViewById(R.id.lastTrainingTime);
        TextView tonnageView = view.findViewById(R.id.totalTonnage);

        backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        new Thread(() -> {
            TrainingSession session = AppDatabase.getInstance(getContext())
                    .trainingSessionDao().getLastSessionForUser(MainActivity.currentUserId);
            getActivity().runOnUiThread(() -> {
                if (session != null) {
                    int min = session.totalSeconds / 60;
                    int sec = session.totalSeconds % 60;
                    lastTimeView.setText("Последняя тренировка: " + String.format("%02d:%02d", min, sec));
                    tonnageView.setText("Общий тоннаж: " + session.tonnage + " кг");
                } else {
                    lastTimeView.setText("Нет данных");
                    tonnageView.setText("Общий тоннаж: 0 кг");
                }
            });
        }).start();
        return view;
    }
}