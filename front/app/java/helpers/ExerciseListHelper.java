package com.example.fitnesshelper.helpers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ExerciseListHelper {

    public static final String[][] EXERCISES_WITH_GROUPS = {
            {"Жим штанги лёжа", "Грудь"},
            {"Узкий жим", "Грудь"},
            {"Жим гантелей лёжа", "Грудь"},
            {"Жим штанги на наклонной скамье", "Грудь"},
            {"Жим гантелей на наклонной скамье", "Грудь"},
            {"Отжимания на брусьях", "Грудь"},
            {"Бабочка", "Грудь"},
            {"Разводка", "Грудь"},
            {"Сведение рук в кроссовере", "Грудь"},
            {"Жим лёжа в Смите", "Грудь"},
            {"Отжимания с колен", "Грудь"},
            {"Подтягивания", "Спина"},
            {"Подтягивания узким хватом", "Спина"},
            {"Подтягивания в гравитроне", "Спина"},
            {"Подтягивания в гравитроне узким хватом", "Спина"},
            {"Тяга верхнего блока к груди", "Спина"},
            {"Тяга верхнего блока к груди узким хватом", "Спина"},
            {"Тяга нижнего блока к животу", "Спина"},
            {"Тяга среднего блока", "Спина"},
            {"Тяга штанги в наклоне прямой хват", "Спина"},
            {"Тяга штанги в наклоне обратный хват", "Спина"},
            {"Тяга T-грифа", "Спина"},
            {"Тяга гантели в наклоне одной рукой", "Спина"},
            {"Тяга нижнего блока одной рукой", "Спина"},
            {"Шраги со штангой", "Спина"},
            {"Шраги с гантелями", "Спина"},
            {"Пуловер", "Спина"},
            {"Жим штанги стоя", "Плечи"},
            {"Жим гантелей сидя", "Плечи"},
            {"Протяжка", "Плечи"},
            {"Махи гантелей в стороны", "Плечи"},
            {"Махи гантелей перед собой", "Плечи"},
            {"Махи на заднюю дельту", "Плечи"},
            {"Тяга на заднюю дельту", "Плечи"},
            {"Махи в кроссовере", "Плечи"},
            {"Махи в кроссовере перед собой", "Плечи"},
            {"Сгибание рук со штангой", "Руки"},
            {"Сгибание рук с гантелями супинация", "Руки"},
            {"Сгибание рук с гантелями молотки", "Руки"},
            {"Бицепс в Скотте с гантелей", "Руки"},
            {"Бицепс в Скотте со штангой", "Руки"},
            {"Сгибание рук на блоке", "Руки"},
            {"Французский жим штангой", "Руки"},
            {"Французский жим с гантелями", "Руки"},
            {"Разгибание с гантелью из-за головы", "Руки"},
            {"Разгибание рук на блоке", "Руки"},
            {"Разгибание рук на блоке из-за головы", "Руки"},
            {"Скручивания", "Кор"},
            {"Скручивания на полусфере", "Кор"},
            {"Подъём ног в висе", "Кор"},
            {"Планка", "Кор"},
            {"Боковая планка", "Кор"},
            {"Мёртвый жук", "Кор"},
            {"Гиперэкстензия", "Кор"},
            {"Молитва", "Кор"},
            {"Ягодичный мост", "Ягодичные"},
            {"Отведение ноги в кроссовере", "Ягодичные"},
            {"Румынская тяга со штангой", "Ягодичные"},
            {"Румынская тяга с гантелей", "Ягодичные"},
            {"Румынская тяга в Смите", "Ягодичные"},
            {"Зашагивания на тумбу", "Ягодичные"},
            {"Болгарский присед", "Ягодичные"},
            {"Разведение ног в тренажёре", "Ягодичные"},
            {"Гуд монинг в Смите", "Ягодичные"},
            {"Выпады назад со штангой", "Ноги"},
            {"Приседания со штангой", "Ноги"},
            {"Жим ногами", "Ноги"},
            {"Разгибание ног в тренажёре", "Ноги"},
            {"Сгибание ног в тренажёре", "Ноги"},
            {"Выпады с гантелями", "Ноги"},
            {"Выпады вперёд", "Ноги"},
            {"Выпады назад", "Ноги"},
            {"Становая тяга классическая", "Ноги"},
            {"Становая тяга сумо", "Ноги"},
            {"Мёртвая тяга со штангой", "Ноги"},
            {"Мёртвая тяга с гантелями", "Ноги"},
            {"Приседания в Смите", "Ноги"},
            {"Приседания с гантелей", "Ноги"},
            {"Сведение ног в тренажёре", "Ноги"},
            {"Тяга Зерхера", "Ноги"},
            {"Подъём на носки стоя", "Икры"},
            {"Подъём на носки сидя", "Икры"},
            {"Подъём на носки в жиме ногами", "Икры"}
    };

    public interface OnExerciseSelectedListener {
        void onExerciseSelected(String exerciseName, String muscleGroup);
    }

    public interface OnParameterSelectedListener {
        void onParameterSelected(String field, float value);
    }

    public static ListPopupWindow createExerciseDropdown(Context context, View anchorView,
                                                         OnExerciseSelectedListener listener) {
        ListPopupWindow popup = new ListPopupWindow(context);
        List<String> items = new ArrayList<>();
        List<Integer> enabledPositions = new ArrayList<>();
        String currentGroup = null;
        for (String[] pair : EXERCISES_WITH_GROUPS) {
            String group = pair[1];
            if (!group.equals(currentGroup)) {
                items.add(group.toUpperCase());
                enabledPositions.add(-1); // заголовок
                currentGroup = group;
            }
            items.add(pair[0]);
            enabledPositions.add(1); // упражнение
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1, items) {
            @Override
            public boolean isEnabled(int position) {
                return enabledPositions.get(position) == 1;
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (enabledPositions.get(position) == -1) {
                    tv.setTextColor(0xFF888888);
                    tv.setAllCaps(true);
                    tv.setPadding(16, 16, 16, 8);
                } else {
                    tv.setTextColor(0xFF000000);
                    tv.setAllCaps(false);
                    tv.setPadding(16, 8, 16, 8);
                }
                return view;
            }
        };

        popup.setAnchorView(anchorView);
        popup.setWidth(500);
        popup.setHeight(600);
        popup.setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.white)));
        popup.setAdapter(adapter);
        popup.setOnItemClickListener((parent, view, position, id) -> {
            if (enabledPositions.get(position) == -1) return;
            String selectedName = items.get(position);
            String selectedGroup = "";
            for (String[] pair : EXERCISES_WITH_GROUPS) {
                if (pair[0].equals(selectedName)) {
                    selectedGroup = pair[1];
                    break;
                }
            }
            if (listener != null) {
                listener.onExerciseSelected(selectedName, selectedGroup);
            }
            popup.dismiss();
        });
        return popup;
    }

    public static ListPopupWindow createParameterPicker(Context context, View anchorView, String field,
                                                        OnParameterSelectedListener listener) {
        ListPopupWindow popup = new ListPopupWindow(context);
        popup.setAnchorView(anchorView);
        popup.setWidth(300);
        popup.setHeight(400);
        popup.setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.white)));

        String[] values;
        if (field.equals("sets")) {
            values = new String[]{"1","2","3","4","5","6"};
        } else if (field.equals("reps")) {
            values = new String[]{"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"};
        } else if (field.equals("setsCount")) {
            values = new String[15];
            for (int i = 0; i < 15; i++) values[i] = String.valueOf(i + 1);
        } else { // weight
            int count = 121; // 0, 2.5, 5.0 ... 300.0 (120 шагов)
            values = new String[count];
            for (int i = 0; i < count; i++) {
                values[i] = String.valueOf(i * 2.5f);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, values);
        popup.setAdapter(adapter);
        popup.setOnItemClickListener((parent, view, position, id) -> {
            String selected = values[position];
            float val = Float.parseFloat(selected.replace(",", "."));
            if (listener != null) {
                listener.onParameterSelected(field, val);
            }
            popup.dismiss();
        });
        return popup;
    }
}