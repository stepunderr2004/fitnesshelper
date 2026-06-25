package com.example.fitnesshelper.fragments;

import android.graphics.Color;
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

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.CalendarWeekDay;
import com.applandeo.materialcalendarview.EventDay;
import com.example.fitnesshelper.R;
import com.example.fitnesshelper.activities.MainActivity;
import com.example.fitnesshelper.dto.ClientBooking;
import com.example.fitnesshelper.dto.TrainerWorkingDay;
import com.example.fitnesshelper.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private Button bookButton;
    private TextView statusText;
    private LinearLayout timeSelectionPanel;
    private LinearLayout timeButtonsContainer;
    private List<TrainerWorkingDay> workingDays = new ArrayList<>();
    private List<ClientBooking> myBookings = new ArrayList<>();
    private Calendar selectedCalendar;
    private String selectedTime = null;
    private Button lastSelectedTimeButton = null;
    private boolean isBookingInProgress = false;
    private List<Button> timeButtonList = new ArrayList<>();

    private boolean workingDaysLoaded = false;
    private boolean bookingsLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        calendarView = view.findViewById(R.id.calendarView);
        bookButton = view.findViewById(R.id.bookTrainingButton);
        statusText = view.findViewById(R.id.statusText);
        timeSelectionPanel = view.findViewById(R.id.timeSelectionPanel);
        timeButtonsContainer = view.findViewById(R.id.timeButtonsContainer);

        calendarView.setFirstDayOfWeek(CalendarWeekDay.MONDAY);
        selectedCalendar = Calendar.getInstance();

        loadMyBookings();
        loadWorkingDays();

        calendarView.setOnPreviousPageChangeListener(() -> {
            workingDaysLoaded = false;
            loadWorkingDays();
        });
        calendarView.setOnForwardPageChangeListener(() -> {
            workingDaysLoaded = false;
            loadWorkingDays();
        });

        calendarView.setOnDayClickListener(eventDay -> {
            selectedCalendar = eventDay.getCalendar();
            String dateStr = formatDate(selectedCalendar);
            TrainerWorkingDay workingDay = findWorkingDay(dateStr);

            if (workingDay != null) {
                ClientBooking myBooking = myBookings.stream()
                        .filter(b -> b.bookingDate.equals(dateStr) && b.active)
                        .findFirst().orElse(null);

                if (myBooking != null) {
                    displayBookingTime(myBooking);
                    timeSelectionPanel.setVisibility(View.VISIBLE);
                    statusText.setText("");
                    bookButton.setText("Отменить тренировку");
                    bookButton.setEnabled(true);
                } else {
                    displayAvailableHours(workingDay.enabledHours);
                    timeSelectionPanel.setVisibility(View.VISIBLE);
                    statusText.setText("");
                    bookButton.setText("Записаться на тренировку");
                    updateBookButtonState();
                }
            } else {
                timeSelectionPanel.setVisibility(View.GONE);
                statusText.setText("Тренер не работает в этот день");
                bookButton.setEnabled(false);
            }
        });

        bookButton.setOnClickListener(v -> {
            if (isBookingInProgress) return;

            String dateStr = formatDate(selectedCalendar);
            ClientBooking existing = myBookings.stream()
                    .filter(b -> b.bookingDate.equals(dateStr) && b.active)
                    .findFirst().orElse(null);

            if (existing != null) {
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setTitle("Отменить тренировку?")
                        .setMessage("Вы действительно хотите отменить тренировку на " + dateStr + " в " + existing.hour + ":00?")
                        .setPositiveButton("Да", (dialog, which) -> cancelBooking(existing))
                        .setNegativeButton("Нет", null)
                        .show();
                return;
            }

            if (selectedTime == null) {
                Toast.makeText(getContext(), "Выберите время", Toast.LENGTH_SHORT).show();
                return;
            }

            int hour = Integer.parseInt(selectedTime.split(":")[0]);

            if (myBookings.size() >= 5) {
                Toast.makeText(getContext(), "Максимум 5 активных записей", Toast.LENGTH_SHORT).show();
                return;
            }
            if (myBookings.stream().anyMatch(b -> b.bookingDate.equals(dateStr))) {
                Toast.makeText(getContext(), "У вас уже есть запись на этот день", Toast.LENGTH_SHORT).show();
                return;
            }

            isBookingInProgress = true;
            bookButton.setEnabled(false);

            Map<String, Object> body = new HashMap<>();
            body.put("date", dateStr);
            body.put("hour", hour);
            RetrofitClient.getInstance(getContext()).getApiService().bookTraining(body)
                    .enqueue(new Callback<ClientBooking>() {
                        @Override
                        public void onResponse(Call<ClientBooking> call, Response<ClientBooking> response) {
                            isBookingInProgress = false;
                            if (response.isSuccessful() && response.body() != null) {
                                ClientBooking newBooking = response.body();
                                myBookings.add(newBooking);
                                refreshEvents(); // обновляем маркеры немедленно
                                displayBookingTime(newBooking);
                                bookButton.setText("Отменить тренировку");
                                bookButton.setEnabled(true);
                                statusText.setText("Запись создана");
                                Toast.makeText(getContext(), "Запись создана", Toast.LENGTH_SHORT).show();
                            } else {
                                String errorMsg = "Ошибка записи";
                                try {
                                    if (response.errorBody() != null) {
                                        errorMsg = response.errorBody().string();
                                    }
                                } catch (Exception ignored) {}
                                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                                updateBookButtonState();
                            }
                        }

                        @Override
                        public void onFailure(Call<ClientBooking> call, Throwable t) {
                            isBookingInProgress = false;
                            updateBookButtonState();
                            Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        updateBookButtonState();
        return view;
    }

    private void loadMyBookings() {
        RetrofitClient.getInstance(getContext()).getApiService().getMyBookings()
                .enqueue(new Callback<List<ClientBooking>>() {
                    @Override
                    public void onResponse(Call<List<ClientBooking>> call, Response<List<ClientBooking>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ClientBooking> fresh = new ArrayList<>();
                            for (ClientBooking b : response.body()) {
                                if (!b.active) continue;
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    Date bookingDate = sdf.parse(b.bookingDate);
                                    if (bookingDate.before(new Date())) {
                                        RetrofitClient.getInstance(getContext()).getApiService().cancelBooking(b.id)
                                                .enqueue(new Callback<Void>() {
                                                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                                                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                                                });
                                        continue;
                                    }
                                } catch (Exception ignored) {}
                                fresh.add(b);
                            }
                            myBookings = fresh;
                        }
                        bookingsLoaded = true;
                        tryRefreshEvents();
                        updateBookButtonState();
                    }
                    @Override
                    public void onFailure(Call<List<ClientBooking>> call, Throwable t) {
                        bookingsLoaded = true;
                        tryRefreshEvents();
                    }
                });
    }

    private void loadWorkingDays() {
        Calendar currentPage = calendarView.getCurrentPageDate();
        Calendar from = (Calendar) currentPage.clone();
        from.set(Calendar.DAY_OF_MONTH, 1);
        Calendar to = (Calendar) currentPage.clone();
        to.set(Calendar.DAY_OF_MONTH, to.getActualMaximum(Calendar.DAY_OF_MONTH));
        String fromStr = formatDate(from);
        String toStr = formatDate(to);

        RetrofitClient.getInstance(getContext()).getApiService().getWorkingDays(fromStr, toStr)
                .enqueue(new Callback<List<TrainerWorkingDay>>() {
                    @Override
                    public void onResponse(Call<List<TrainerWorkingDay>> call, Response<List<TrainerWorkingDay>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            workingDays = response.body();
                        }
                        workingDaysLoaded = true;
                        tryRefreshEvents();
                    }
                    @Override
                    public void onFailure(Call<List<TrainerWorkingDay>> call, Throwable t) {
                        workingDaysLoaded = true;
                        tryRefreshEvents();
                    }
                });
    }

    private void tryRefreshEvents() {
        if (workingDaysLoaded && bookingsLoaded) {
            refreshEvents();
        }
    }

    private void refreshEvents() {
        List<EventDay> events = new ArrayList<>();
        Set<String> bookedDates = new HashSet<>();

        // Сначала добавляем фиолетовые круги для дней с записями
        for (ClientBooking b : myBookings) {
            if (!b.active) continue;
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(b.bookingDate));
                events.add(new EventDay(cal, R.drawable.ic_training_dot));
                bookedDates.add(b.bookingDate);
            } catch (Exception ignored) {}
        }

        // Затем добавляем жёлтые круги только для рабочих дней без записей
        for (TrainerWorkingDay day : workingDays) {
            if (!bookedDates.contains(day.date)) {
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(day.date));
                    events.add(new EventDay(cal, R.drawable.ic_yellow_circle));
                } catch (Exception ignored) {}
            }
        }

        calendarView.setEvents(events);
    }

    // ... остальные методы без изменений: displayAvailableHours, displayBookingTime, highlightTimeButton, updateBookButtonState, findWorkingDay, formatDate, cancelBooking
    // В cancelBooking также вызываем refreshEvents() после удаления из myBookings

    private void displayAvailableHours(List<Integer> hours) {
        timeButtonsContainer.removeAllViews();
        timeButtonList.clear();
        lastSelectedTimeButton = null;
        selectedTime = null;

        for (int hour : hours) {
            Button timeBtn = new Button(getContext());
            timeBtn.setText(hour + ":00");
            timeBtn.setTextSize(12);
            timeBtn.setAllCaps(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 4, 8, 4);
            timeBtn.setLayoutParams(params);
            timeBtn.setBackgroundResource(R.drawable.field_border);
            timeBtn.setPadding(16, 8, 16, 8);
            int h = hour;
            timeBtn.setOnClickListener(v -> {
                selectedTime = h + ":00";
                highlightTimeButton(timeBtn);
                updateBookButtonState();
            });
            timeButtonList.add(timeBtn);
            timeButtonsContainer.addView(timeBtn);
        }
        updateBookButtonState();
    }

    private void displayBookingTime(ClientBooking myBooking) {
        timeButtonsContainer.removeAllViews();
        timeButtonList.clear();
        lastSelectedTimeButton = null;
        selectedTime = null;

        Button timeBtn = new Button(getContext());
        timeBtn.setText(myBooking.hour + ":00");
        timeBtn.setTextSize(12);
        timeBtn.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 4, 8, 4);
        timeBtn.setLayoutParams(params);
        timeBtn.setBackgroundColor(Color.parseColor("#FF6200EE"));
        timeBtn.setTextColor(Color.WHITE);
        timeBtn.setEnabled(false);
        timeButtonsContainer.addView(timeBtn);
        statusText.setText("У вас тренировка в " + myBooking.hour + ":00");
    }

    private void highlightTimeButton(Button selectedBtn) {
        for (Button btn : timeButtonList) {
            btn.setBackgroundResource(R.drawable.field_border);
            btn.setTextColor(Color.BLACK);
        }
        selectedBtn.setBackgroundColor(Color.parseColor("#FF6200EE"));
        selectedBtn.setTextColor(Color.WHITE);
        lastSelectedTimeButton = selectedBtn;
    }

    private void updateBookButtonState() {
        boolean hasTrainer = MainActivity.currentUser != null
                && MainActivity.currentUser.trainerId != null
                && MainActivity.currentUser.trainerId != 0;
        if (!hasTrainer) {
            bookButton.setEnabled(false);
            statusText.setText("Вы не занимаетесь с тренером");
            return;
        }
        if (isBookingInProgress) {
            bookButton.setEnabled(false);
            return;
        }
        if (bookButton.getText().equals("Отменить тренировку")) {
            bookButton.setEnabled(true);
            return;
        }
        boolean timeSelected = selectedTime != null && findWorkingDay(formatDate(selectedCalendar)) != null;
        boolean maxBookings = myBookings.size() >= 5;
        bookButton.setEnabled(timeSelected && !maxBookings);
        if (maxBookings) {
            statusText.setText("Достигнут лимит записей (5)");
        }
    }

    private TrainerWorkingDay findWorkingDay(String dateStr) {
        for (TrainerWorkingDay day : workingDays) {
            if (day.date.equals(dateStr)) return day;
        }
        return null;
    }

    private String formatDate(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    private void cancelBooking(ClientBooking booking) {
        RetrofitClient.getInstance(getContext()).getApiService().cancelBooking(booking.id)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Тренировка отменена", Toast.LENGTH_SHORT).show();
                            myBookings.removeIf(b -> b.id == booking.id);
                            refreshEvents();
                            timeSelectionPanel.setVisibility(View.GONE);
                            selectedTime = null;
                            bookButton.setText("Записаться на тренировку");
                            updateBookButtonState();
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка отмены", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}