package com.example.fitnesshelper.fragments.trainer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.CalendarWeekDay;
import com.applandeo.materialcalendarview.EventDay;
import com.example.fitnesshelper.R;
import com.example.fitnesshelper.dto.*;
import com.example.fitnesshelper.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainerScheduleFragment extends Fragment {

    private CalendarView calendarView;
    private LinearLayout templatesContainer;
    private Button createTemplateButton;
    private LinearLayout dayInfoPanel;
    private TextView dayInfoTitle;
    private LinearLayout dayInfoContent;
    private Button editDayButton;
    private ImageButton closeDayInfoButton;
    private ImageButton[] weekButtons = new ImageButton[6];

    private List<TrainerScheduleTemplate> templates = new ArrayList<>();
    private List<TrainerWorkingDay> workingDays = new ArrayList<>();
    private Map<String, List<ClientBooking>> bookingsCache = new HashMap<>();
    private TrainerWorkingDay selectedDay;

    private int pendingBookingRequests = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trainer_schedule, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        templatesContainer = view.findViewById(R.id.templatesContainer);
        createTemplateButton = view.findViewById(R.id.createTemplateButton);
        dayInfoPanel = view.findViewById(R.id.dayInfoPanel);
        dayInfoTitle = view.findViewById(R.id.dayInfoTitle);
        dayInfoContent = view.findViewById(R.id.dayInfoContent);
        editDayButton = view.findViewById(R.id.editDayButton);
        closeDayInfoButton = view.findViewById(R.id.closeDayInfoButton);

        calendarView.setFirstDayOfWeek(CalendarWeekDay.MONDAY);

        weekButtons[0] = view.findViewById(R.id.weekButton0);
        weekButtons[1] = view.findViewById(R.id.weekButton1);
        weekButtons[2] = view.findViewById(R.id.weekButton2);
        weekButtons[3] = view.findViewById(R.id.weekButton3);
        weekButtons[4] = view.findViewById(R.id.weekButton4);
        weekButtons[5] = view.findViewById(R.id.weekButton5);

        for (int i = 0; i < 6; i++) {
            final int weekIndex = i;
            weekButtons[i].setOnClickListener(v -> showWeekOptionsDialog(weekIndex));
        }

        createTemplateButton.setOnClickListener(v -> showCreateTemplateDialog());

        closeDayInfoButton.setOnClickListener(v -> {
            dayInfoPanel.setVisibility(View.GONE);
            selectedDay = null;
        });

        editDayButton.setOnClickListener(v -> {
            if (selectedDay != null) {
                if (hasBookings(selectedDay.date)) {
                    Toast.makeText(getContext(), "Нельзя изменить часы – есть активные записи", Toast.LENGTH_SHORT).show();
                } else {
                    showEditHoursDialog(selectedDay);
                }
            }
        });

        calendarView.setOnDayClickListener(eventDay -> {
            Calendar cal = eventDay.getCalendar();
            String dateStr = formatDate(cal);
            TrainerWorkingDay day = findWorkingDay(dateStr);
            if (day != null) {
                showDayInfo(day);
            } else {
                dayInfoPanel.setVisibility(View.GONE);
            }
        });

        calendarView.setOnPreviousPageChangeListener(() -> loadWorkingDaysForVisibleMonth());
        calendarView.setOnForwardPageChangeListener(() -> loadWorkingDaysForVisibleMonth());

        loadTemplates();
        loadWorkingDaysForVisibleMonth();
        return view;
    }

    private void loadTemplates() {
        if (!isAdded()) return;
        RetrofitClient.getInstance(getContext()).getApiService().getScheduleTemplates()
                .enqueue(new Callback<List<TrainerScheduleTemplate>>() {
                    @Override
                    public void onResponse(Call<List<TrainerScheduleTemplate>> call, Response<List<TrainerScheduleTemplate>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            templates = response.body();
                            renderTemplates();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<TrainerScheduleTemplate>> call, Throwable t) {}
                });
    }

    private void loadWorkingDaysForVisibleMonth() {
        if (!isAdded()) return;
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
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            workingDays = response.body();
                            loadBookingsForAllWorkingDays();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<TrainerWorkingDay>> call, Throwable t) {}
                });
    }

    private void loadBookingsForAllWorkingDays() {
        bookingsCache.clear();
        pendingBookingRequests = workingDays.size();
        if (pendingBookingRequests == 0) {
            highlightWorkingDays();
            return;
        }

        for (TrainerWorkingDay day : workingDays) {
            RetrofitClient.getInstance(getContext()).getApiService().getTrainerBookings(day.date)
                    .enqueue(new Callback<List<ClientBooking>>() {
                        @Override
                        public void onResponse(Call<List<ClientBooking>> call, Response<List<ClientBooking>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                bookingsCache.put(day.date, response.body());
                            } else {
                                bookingsCache.put(day.date, Collections.emptyList());
                            }
                            checkAllBookingsLoaded();
                        }
                        @Override
                        public void onFailure(Call<List<ClientBooking>> call, Throwable t) {
                            bookingsCache.put(day.date, Collections.emptyList());
                            checkAllBookingsLoaded();
                        }
                    });
        }
    }

    private synchronized void checkAllBookingsLoaded() {
        pendingBookingRequests--;
        if (pendingBookingRequests <= 0) {
            highlightWorkingDays();
        }
    }

    private void highlightWorkingDays() {
        List<EventDay> events = new ArrayList<>();
        Set<String> bookedDates = new HashSet<>();

        // сначала фиолетовые для дней с бронированиями
        for (Map.Entry<String, List<ClientBooking>> entry : bookingsCache.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                bookedDates.add(entry.getKey());
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.getKey()));
                    events.add(new EventDay(cal, R.drawable.ic_training_dot));
                } catch (Exception ignored) {}
            }
        }

        // затем жёлтые только для рабочих дней без бронирований
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

    private void renderTemplates() {
        if (!isAdded() || getContext() == null) return;
        templatesContainer.removeAllViews();
        for (TrainerScheduleTemplate t : templates) {
            View row = LayoutInflater.from(getContext()).inflate(R.layout.item_schedule_template, templatesContainer, false);
            TextView nameView = row.findViewById(R.id.templateName);
            ImageButton deleteBtn = row.findViewById(R.id.deleteTemplateButton);
            nameView.setText(t.name);
            nameView.setOnClickListener(v -> openTemplateEditor(t));
            deleteBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Удалить шаблон?")
                        .setMessage("Вы уверены?")
                        .setPositiveButton("Да", (dialog, which) -> {
                            RetrofitClient.getInstance(getContext()).getApiService().deleteScheduleTemplate(t.id)
                                    .enqueue(new Callback<Void>() {
                                        @Override
                                        public void onResponse(Call<Void> call, Response<Void> response) {
                                            if (response.isSuccessful()) loadTemplates();
                                        }
                                        @Override
                                        public void onFailure(Call<Void> call, Throwable t) {}
                                    });
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            });
            templatesContainer.addView(row);
        }
        createTemplateButton.setVisibility(templates.size() < 5 ? View.VISIBLE : View.GONE);
    }

    private void showCreateTemplateDialog() {
        if (!isAdded()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_template, null);
        EditText nameInput = dialogView.findViewById(R.id.templateNameInput);
        Switch allHoursSwitch = dialogView.findViewById(R.id.allHoursSwitch);
        builder.setView(dialogView);
        builder.setTitle("Новый шаблон");
        builder.setPositiveButton("Создать", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            boolean allHours = allHoursSwitch.isChecked();
            if (name.isEmpty()) return;
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("allHoursEnabled", allHours);
            RetrofitClient.getInstance(getContext()).getApiService().createScheduleTemplate(body)
                    .enqueue(new Callback<TrainerScheduleTemplate>() {
                        @Override
                        public void onResponse(Call<TrainerScheduleTemplate> call, Response<TrainerScheduleTemplate> response) {
                            if (response.isSuccessful()) loadTemplates();
                        }
                        @Override
                        public void onFailure(Call<TrainerScheduleTemplate> call, Throwable t) {}
                    });
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void openTemplateEditor(TrainerScheduleTemplate template) {
        Fragment fragment = TrainerTemplateEditorFragment.newInstance(template);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showWeekOptionsDialog(int weekIndex) {
        Calendar monday = getMondayOfWeek(weekIndex);
        if (monday == null) return;
        String mondayStr = formatDate(monday);

        if (templates.isEmpty()) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Неделя с " + mondayStr)
                    .setMessage("Шаблонов нет. Удалить расписание на этой неделе?")
                    .setPositiveButton("Удалить", (dialog, which) -> deleteWeekSchedule(monday))
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            String[] items = new String[templates.size() + 1];
            for (int i = 0; i < templates.size(); i++) items[i] = templates.get(i).name;
            items[templates.size()] = "Удалить расписание на этой неделе";

            new AlertDialog.Builder(getContext())
                    .setTitle("Неделя с " + mondayStr)
                    .setItems(items, (dialog, which) -> {
                        if (which == templates.size()) {
                            deleteWeekSchedule(monday);
                        } else {
                            TrainerScheduleTemplate chosen = templates.get(which);
                            applyTemplate(chosen.id, mondayStr);
                        }
                    })
                    .show();
        }
    }

    private Calendar getMondayOfWeek(int weekIndex) {
        Calendar pageDate = calendarView.getCurrentPageDate();
        Calendar firstDayOfMonth = (Calendar) pageDate.clone();
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        Calendar firstMonday = (Calendar) firstDayOfMonth.clone();
        while (firstMonday.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            firstMonday.add(Calendar.DAY_OF_MONTH, -1);
        }
        firstMonday.add(Calendar.DAY_OF_MONTH, weekIndex * 7);
        return firstMonday;
    }

    private void applyTemplate(int templateId, String startDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("templateId", templateId);
        body.put("startDate", startDate);
        RetrofitClient.getInstance(getContext()).getApiService().applyScheduleTemplate(body)
                .enqueue(new Callback<List<TrainerWorkingDay>>() {
                    @Override
                    public void onResponse(Call<List<TrainerWorkingDay>> call, Response<List<TrainerWorkingDay>> response) {
                        if (response.isSuccessful()) {
                            loadWorkingDaysForVisibleMonth();
                            Toast.makeText(getContext(), "Расписание применено", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<TrainerWorkingDay>> call, Throwable t) {}
                });
    }

    private void deleteWeekSchedule(Calendar monday) {
        Calendar sunday = (Calendar) monday.clone();
        sunday.add(Calendar.DAY_OF_MONTH, 6);
        String fromStr = formatDate(monday);
        String toStr = formatDate(sunday);

        RetrofitClient.getInstance(getContext()).getApiService().getWorkingDays(fromStr, toStr)
                .enqueue(new Callback<List<TrainerWorkingDay>>() {
                    @Override
                    public void onResponse(Call<List<TrainerWorkingDay>> call, Response<List<TrainerWorkingDay>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<TrainerWorkingDay> weekDays = response.body();
                            for (TrainerWorkingDay day : weekDays) {
                                if (!hasBookings(day.date)) {
                                    RetrofitClient.getInstance(getContext()).getApiService().deleteWorkingDay(day.id)
                                            .enqueue(new Callback<Void>() {
                                                @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                                                @Override public void onFailure(Call<Void> call, Throwable t) {}
                                            });
                                }
                            }
                            Toast.makeText(getContext(), "Расписание на неделю удалено", Toast.LENGTH_SHORT).show();
                            loadWorkingDaysForVisibleMonth();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<TrainerWorkingDay>> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка при удалении недели", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDayInfo(TrainerWorkingDay day) {
        selectedDay = day;
        dayInfoTitle.setText("День: " + day.date);
        loadBookingsForDay(day);
    }

    private void loadBookingsForDay(TrainerWorkingDay day) {
        RetrofitClient.getInstance(getContext()).getApiService().getTrainerBookings(day.date)
                .enqueue(new Callback<List<ClientBooking>>() {
                    @Override
                    public void onResponse(Call<List<ClientBooking>> call, Response<List<ClientBooking>> response) {
                        if (!isAdded()) return;
                        List<ClientBooking> bookings = response.isSuccessful() && response.body() != null ?
                                response.body() : Collections.emptyList();
                        bookingsCache.put(day.date, bookings);
                        renderDayInfo(day, bookings);
                        highlightWorkingDays();
                    }
                    @Override
                    public void onFailure(Call<List<ClientBooking>> call, Throwable t) {}
                });
    }

    private void renderDayInfo(TrainerWorkingDay day, List<ClientBooking> bookings) {
        dayInfoContent.removeAllViews();
        Map<Integer, List<ClientBooking>> byHour = new HashMap<>();
        for (ClientBooking b : bookings) {
            byHour.computeIfAbsent(b.hour, k -> new ArrayList<>()).add(b);
        }
        List<Integer> sortedHours = new ArrayList<>(day.enabledHours);
        Collections.sort(sortedHours);

        for (int hour : sortedHours) {
            LinearLayout hourLayout = new LinearLayout(getContext());
            hourLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView tv = new TextView(getContext());
            String timeStr = hour + ":00";
            List<ClientBooking> hourBookings = byHour.getOrDefault(hour, Collections.emptyList());
            String clientsStr = hourBookings.isEmpty() ? "нет записей" :
                    hourBookings.stream().map(b -> b.clientName != null ? b.clientName : "клиент").collect(Collectors.joining(", "));
            tv.setText(timeStr + " : " + clientsStr);
            tv.setPadding(8, 8, 8, 8);

            if (!hourBookings.isEmpty()) {
                ClientBooking firstBooking = hourBookings.get(0);
                Button clientBtn = new Button(getContext());
                clientBtn.setText(firstBooking.clientName != null ? firstBooking.clientName : "клиент");
                clientBtn.setTextSize(12);
                clientBtn.setAllCaps(false);
                clientBtn.setOnClickListener(v -> showCancelClientDialog(firstBooking));
                hourLayout.addView(tv);
                hourLayout.addView(clientBtn);
            } else {
                hourLayout.addView(tv);
            }

            dayInfoContent.addView(hourLayout);
        }

        dayInfoPanel.setVisibility(View.VISIBLE);
        boolean hasBookings = !bookings.isEmpty();
        editDayButton.setEnabled(!hasBookings);
        editDayButton.setAlpha(hasBookings ? 0.5f : 1.0f);
    }

    private void showCancelClientDialog(ClientBooking booking) {
        new AlertDialog.Builder(getContext())
                .setTitle("Отменить клиента?")
                .setMessage("Вы действительно хотите отменить запись " + booking.clientName + " на " + booking.bookingDate + " в " + booking.hour + ":00?")
                .setPositiveButton("Да", (dialog, which) -> {
                    RetrofitClient.getInstance(getContext()).getApiService().cancelBooking(booking.id)
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(getContext(), "Запись отменена", Toast.LENGTH_SHORT).show();
                                        loadWorkingDaysForVisibleMonth();
                                        if (selectedDay != null) loadBookingsForDay(selectedDay);
                                    } else {
                                        Toast.makeText(getContext(), "Ошибка отмены", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void showEditHoursDialog(TrainerWorkingDay day) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_hours, null);
        LinearLayout hoursContainer = view.findViewById(R.id.hoursContainer);

        Map<Integer, CheckBox> checkBoxes = new HashMap<>();
        for (int h = 9; h <= 22; h++) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(h + ":00");
            cb.setChecked(day.enabledHours.contains(h));
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
            Map<String, Object> body = new HashMap<>();
            body.put("enabledHours", newHours);
            RetrofitClient.getInstance(getContext()).getApiService()
                    .editWorkingDayHours(day.id, body)
                    .enqueue(new Callback<TrainerWorkingDay>() {
                        @Override
                        public void onResponse(Call<TrainerWorkingDay> call, Response<TrainerWorkingDay> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                day.enabledHours = response.body().enabledHours;
                                loadBookingsForDay(day);
                            }
                        }
                        @Override
                        public void onFailure(Call<TrainerWorkingDay> call, Throwable t) {}
                    });
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private boolean hasBookings(String date) {
        List<ClientBooking> bookings = bookingsCache.get(date);
        return bookings != null && !bookings.isEmpty();
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
}