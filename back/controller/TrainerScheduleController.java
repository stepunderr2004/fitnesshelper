package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.*;
import com.example.fitnesshelper.repository.*;
import com.example.fitnesshelper.service.FcmService;
import com.example.fitnesshelper.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trainer/schedule")
public class TrainerScheduleController {

    private final TrainerScheduleTemplateRepository templateRepo;
    private final TrainerScheduleDayRepository dayRepo;
    private final TrainerWorkingDayRepository workingDayRepo;
    private final ClientBookingRepository bookingRepo;
    private final UserService userService;
    private final FcmService fcmService;

    public TrainerScheduleController(TrainerScheduleTemplateRepository templateRepo,
                                     TrainerScheduleDayRepository dayRepo,
                                     TrainerWorkingDayRepository workingDayRepo,
                                     ClientBookingRepository bookingRepo,
                                     UserService userService, FcmService fcmService) {
        this.templateRepo = templateRepo;
        this.dayRepo = dayRepo;
        this.workingDayRepo = workingDayRepo;
        this.bookingRepo = bookingRepo;
        this.userService = userService;
        this.fcmService = fcmService;
    }

    private User getCurrentTrainer(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        User user = userService.findById(userId);
        if (user == null || !"TRAINER".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return user;
    }

    // ========== Шаблоны ==========
    @GetMapping("/templates")
    public List<TrainerScheduleTemplate> getTemplates(HttpServletRequest request) {
        User trainer = getCurrentTrainer(request);
        return templateRepo.findByTrainerId(trainer.getId());
    }

    @GetMapping("/templates/{id}")
    public TrainerScheduleTemplate getTemplate(@PathVariable int id, HttpServletRequest request) {
        User trainer = getCurrentTrainer(request);
        return templateRepo.findByIdAndTrainerId(id, trainer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/templates")
    public TrainerScheduleTemplate createTemplate(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        User trainer = getCurrentTrainer(request);
        if (templateRepo.countByTrainerId(trainer.getId()) >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 5 templates");
        }
        TrainerScheduleTemplate template = new TrainerScheduleTemplate();
        template.setTrainer(trainer);
        template.setName((String) payload.get("name"));
        template.setAllHoursEnabled((Boolean) payload.getOrDefault("allHoursEnabled", false));
        Set<TrainerScheduleDay> days = new HashSet<>();
        for (int d = 1; d <= 7; d++) {
            TrainerScheduleDay day = new TrainerScheduleDay();
            day.setTemplate(template);
            day.setDayOfWeek(d);
            if (template.isAllHoursEnabled()) {
                for (int h = 9; h <= 22; h++) {
                    day.getEnabledHours().add(h);
                }
            }
            days.add(day);
        }
        template.setDays(days);
        template = templateRepo.save(template);
        notifyClients(trainer);
        return template;
    }

    @DeleteMapping("/templates/{id}")
    public void deleteTemplate(@PathVariable int id, HttpServletRequest request) {
        User trainer = getCurrentTrainer(request);
        TrainerScheduleTemplate template = templateRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (template.getTrainer().getId() != trainer.getId()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        templateRepo.delete(template);
        notifyClients(trainer);
    }

    @PutMapping("/days/{dayId}/hours")
    public TrainerScheduleDay toggleHour(@PathVariable int dayId, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        User trainer = getCurrentTrainer(request);
        TrainerScheduleDay day = dayRepo.findByIdAndTemplate_TrainerId(dayId, trainer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int hour = (int) payload.get("hour");
        boolean enable = (boolean) payload.get("enable");
        if (enable) {
            day.getEnabledHours().add(hour);
        } else {
            day.getEnabledHours().remove(hour);
        }
        day = dayRepo.save(day);
        return day;
    }

    // ========== Рабочие дни (фактическое расписание) ==========
    @PostMapping("/apply-template")
    public List<TrainerWorkingDay> applyTemplate(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        User trainer = getCurrentTrainer(request);
        int templateId = (int) payload.get("templateId");
        String startDateStr = (String) payload.get("startDate");
        LocalDate startDate = LocalDate.parse(startDateStr);
        TrainerScheduleTemplate template = templateRepo.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (template.getTrainer().getId() != trainer.getId()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        List<TrainerWorkingDay> createdDays = new ArrayList<>();
        for (TrainerScheduleDay templateDay : template.getDays()) {
            LocalDate date = startDate.plusDays(templateDay.getDayOfWeek() - 1);
            Optional<TrainerWorkingDay> existing = workingDayRepo.findByTrainerIdAndDate(trainer.getId(), date);
            TrainerWorkingDay workingDay = existing.orElseGet(TrainerWorkingDay::new);
            workingDay.setTrainer(trainer);
            workingDay.setDate(date);
            workingDay.setEnabledHours(new HashSet<>(templateDay.getEnabledHours()));
            createdDays.add(workingDayRepo.save(workingDay));
        }
        notifyClients(trainer);
        return createdDays;
    }

    @GetMapping("/working-days")
    public List<TrainerWorkingDay> getWorkingDays(@RequestParam String from, @RequestParam String to, HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        User user = userService.findById(userId);
        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);
        if ("TRAINER".equals(user.getRole())) {
            return workingDayRepo.findByTrainerIdAndDateBetween(user.getId(), dateFrom, dateTo);
        } else if ("CLIENT".equals(user.getRole())) {
            User trainer = user.getTrainer();
            if (trainer == null) return Collections.emptyList();
            return workingDayRepo.findByTrainerIdAndDateBetween(trainer.getId(), dateFrom, dateTo);
        }
        return Collections.emptyList();
    }

    @PutMapping("/working-days/{dayId}/hours")
    public TrainerWorkingDay editWorkingDayHours(@PathVariable int dayId, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        User trainer = getCurrentTrainer(request);
        TrainerWorkingDay day = workingDayRepo.findById(dayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (day.getTrainer().getId() != trainer.getId()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        if (bookingRepo.existsByTrainerIdAndBookingDateAndActiveTrue(trainer.getId(), day.getDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change hours when there are active bookings");
        }

        if (payload.containsKey("enabledHours")) {
            List<Integer> hours = (List<Integer>) payload.get("enabledHours");
            day.setEnabledHours(new HashSet<>(hours));
        }
        day = workingDayRepo.save(day);
        notifyClients(trainer);
        return day;
    }

    // ========== Записи клиентов ==========
    @GetMapping("/bookings")
    public List<ClientBooking> getBookings(@RequestParam String date, HttpServletRequest request) {
        User trainer = getCurrentTrainer(request);
        LocalDate localDate = LocalDate.parse(date);
        List<ClientBooking> bookings = bookingRepo.findByTrainerIdAndBookingDateAndActiveTrue(trainer.getId(), localDate);
        bookings.forEach(b -> b.setClientName(b.getClient().getName()));
        return bookings;
    }

    @PostMapping("/book")
    public ClientBooking book(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        User client = userService.findById(userId);
        if (client == null || !"CLIENT".equals(client.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        User trainer = client.getTrainer();
        if (trainer == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No trainer assigned");

        LocalDate date = LocalDate.parse((String) payload.get("date"));
        int hour = (int) payload.get("hour");

        TrainerWorkingDay workingDay = workingDayRepo.findByTrainerIdAndDate(trainer.getId(), date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trainer does not work on this date"));
        if (!workingDay.getEnabledHours().contains(hour)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hour not available");

        if (date.isBefore(LocalDate.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book in the past");

        if (bookingRepo.countByClientIdAndActiveTrue(client.getId()) >= 5)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 5 active bookings");

        if (bookingRepo.existsByClientIdAndBookingDateAndHour(client.getId(), date, hour))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already booked at this time");

        ClientBooking booking = new ClientBooking();
        booking.setClient(client);
        booking.setTrainer(trainer);
        booking.setBookingDate(date);
        booking.setHour(hour);
        booking.setActive(true);
        booking.setCreatedDate(LocalDateTime.now());
        booking = bookingRepo.save(booking);
        booking.setClientName(client.getName());
        return booking;
    }

    // Удаление рабочего дня (новый метод)
    @DeleteMapping("/working-days/{dayId}")
    public void deleteWorkingDay(@PathVariable int dayId, HttpServletRequest request) {
        User trainer = getCurrentTrainer(request);
        TrainerWorkingDay day = workingDayRepo.findById(dayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (day.getTrainer().getId() != trainer.getId())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        if (bookingRepo.existsByTrainerIdAndBookingDateAndActiveTrue(trainer.getId(), day.getDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete working day with active bookings");
        }
        workingDayRepo.delete(day);
        notifyClients(trainer);
    }

    // Отмена записи (теперь доступна и клиенту, и тренеру)
    @DeleteMapping("/bookings/{id}")
    public void cancelBooking(@PathVariable int id, HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        ClientBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if ("TRAINER".equals(role)) {
            // Тренер отменяет запись только у своего клиента
            if (booking.getTrainer().getId() != userId) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        } else {
            // Клиент отменяет свою запись
            if (booking.getClient().getId() != userId) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        booking.setActive(false);
        bookingRepo.save(booking);

        // Уведомление клиенту (если отменил тренер)
        if ("TRAINER".equals(role) && booking.getClient().getFcmToken() != null && !booking.getClient().getFcmToken().isEmpty()) {
            fcmService.sendSyncNotification(booking.getClient().getFcmToken());
        }
        // Если клиент сам отменил, можно уведомить тренера, но пока опустим
    }

    @GetMapping("/my-bookings")
    public List<ClientBooking> getMyBookings(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        List<ClientBooking> bookings = bookingRepo.findByClientIdAndActiveTrue(userId);
        bookings.forEach(b -> b.setClientName(b.getClient().getName()));
        return bookings;
    }

    private void notifyClients(User trainer) {
        List<User> clients = userService.getAllUsers().stream()
                .filter(u -> u.getTrainer() != null && u.getTrainer().getId() == trainer.getId())
                .collect(Collectors.toList());
        for (User client : clients) {
            if (client.getFcmToken() != null && !client.getFcmToken().isEmpty()) {
                fcmService.sendSyncNotification(client.getFcmToken());
            }
        }
    }
}