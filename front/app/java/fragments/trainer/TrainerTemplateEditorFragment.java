package com.example.fitnesshelper.fragments.trainer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.dto.TrainerScheduleDay;
import com.example.fitnesshelper.dto.TrainerScheduleTemplate;
import com.example.fitnesshelper.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainerTemplateEditorFragment extends Fragment {

    private static final String ARG_TEMPLATE = "template";

    private TrainerScheduleTemplate template;
    private LinearLayout daysContainer;

    public static TrainerTemplateEditorFragment newInstance(TrainerScheduleTemplate template) {
        TrainerTemplateEditorFragment fragment = new TrainerTemplateEditorFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TEMPLATE, template);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_template_editor, container, false);
        Button backBtn = view.findViewById(R.id.backButton);
        daysContainer = view.findViewById(R.id.daysContainer);

        if (getArguments() != null) {
            template = (TrainerScheduleTemplate) getArguments().getSerializable(ARG_TEMPLATE);
        }

        backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        renderDays();
        return view;
    }

    private void renderDays() {
        daysContainer.removeAllViews();
        if (template == null || template.days == null) return;

        List<TrainerScheduleDay> sortedDays = new ArrayList<>(template.days);
        Collections.sort(sortedDays, Comparator.comparingInt(d -> d.dayOfWeek));

        String[] dayNames = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"};
        for (TrainerScheduleDay day : sortedDays) {
            Button dayBtn = new Button(getContext());
            dayBtn.setText(dayNames[day.dayOfWeek - 1]);
            dayBtn.setOnClickListener(v -> showDayEditor(day));
            daysContainer.addView(dayBtn);
        }
    }

    private void showDayEditor(TrainerScheduleDay day) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_hours, null);
        LinearLayout hoursContainer = view.findViewById(R.id.hoursContainer);

        Map<Integer, CheckBox> checkBoxes = new HashMap<>();
        for (int h = 9; h <= 22; h++) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(h + ":00");
            cb.setChecked(day.enabledHours != null && day.enabledHours.contains(h));
            checkBoxes.put(h, cb);
            hoursContainer.addView(cb);
        }

        builder.setView(view);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            List<Integer> newHours = new ArrayList<>();
            for (Map.Entry<Integer, CheckBox> entry : checkBoxes.entrySet()) {
                if (entry.getValue().isChecked()) {
                    newHours.add(entry.getKey());
                }
            }
            // Отправляем сразу все часы одним запросом toggle? Но API toggle принимает один час.
            // Поэтому нужно либо изменить серверный метод, либо вызывать для каждого изменённого часа.
            // Оставим пока индивидуальные вызовы, но оптимизируем хотя бы отправку только изменённых.
            // Для простоты пока отправляем каждый час отдельно, но можно и весь список через PUT /days/{dayId}/hours (если поддержать массив).
            // Рефакторинг серверной части в другой раз.
            for (int h = 9; h <= 22; h++) {
                boolean wasEnabled = day.enabledHours != null && day.enabledHours.contains(h);
                boolean nowEnabled = newHours.contains(h);
                if (wasEnabled != nowEnabled) {
                    Map<String, Object> body = new HashMap<>();
                    body.put("hour", h);
                    body.put("enable", nowEnabled);
                    RetrofitClient.getInstance(getContext()).getApiService()
                            .toggleScheduleDayHour(day.id, body)
                            .enqueue(new Callback<TrainerScheduleDay>() {
                                @Override
                                public void onResponse(Call<TrainerScheduleDay> call, Response<TrainerScheduleDay> response) {}
                                @Override
                                public void onFailure(Call<TrainerScheduleDay> call, Throwable t) {}
                            });
                }
            }
            // Обновим локальный объект
            day.enabledHours = newHours;
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}