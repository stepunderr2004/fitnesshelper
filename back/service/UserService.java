package com.example.fitnesshelper.service;

import com.example.fitnesshelper.entity.*;
import com.example.fitnesshelper.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FcmService fcmService;

    private final ProgramRepository programRepo;
    private final TrainingDayRepository dayRepo;
    private final ExerciseRepository exerciseRepo;
    private final ExerciseSetRepository setRepo;
    private final TrainingDateRepository dateRepo;
    private final TrainingSessionRepository sessionRepo;
    private final DiaryEntryRepository diaryEntryRepo;
    private final DiaryExerciseRepository diaryExerciseRepo;
    private final DiaryExerciseSetRepository diarySetRepo;

    private final TrainerScheduleTemplateRepository templateRepo;
    private final ClientBookingRepository bookingRepo;
    private final TrainerWorkingDayRepository workingDayRepo;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, FcmService fcmService,
                       ProgramRepository programRepo, TrainingDayRepository dayRepo,
                       ExerciseRepository exerciseRepo, ExerciseSetRepository setRepo,
                       TrainingDateRepository dateRepo, TrainingSessionRepository sessionRepo,
                       DiaryEntryRepository diaryEntryRepo, DiaryExerciseRepository diaryExerciseRepo,
                       DiaryExerciseSetRepository diarySetRepo,
                       TrainerScheduleTemplateRepository templateRepo,
                       ClientBookingRepository bookingRepo,
                       TrainerWorkingDayRepository workingDayRepo) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fcmService = fcmService;
        this.programRepo = programRepo;
        this.dayRepo = dayRepo;
        this.exerciseRepo = exerciseRepo;
        this.setRepo = setRepo;
        this.dateRepo = dateRepo;
        this.sessionRepo = sessionRepo;
        this.diaryEntryRepo = diaryEntryRepo;
        this.diaryExerciseRepo = diaryExerciseRepo;
        this.diarySetRepo = diarySetRepo;
        this.templateRepo = templateRepo;
        this.bookingRepo = bookingRepo;
        this.workingDayRepo = workingDayRepo;
    }

    @PostConstruct
    public void initAdmin() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setName("Administrator");
            admin.setAge("");
            admin.setGender("male");
            admin.setTrainerEnabled(false);
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
    }

    public User findById(int id) {
        return userRepository.findById(id).orElse(null);
    }

    public User findByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken).orElse(null);
    }

    public void updateRefreshToken(int userId, String refreshToken) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setRefreshToken(refreshToken);
            userRepository.save(user);
        }
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    public User updateProfile(int id, String name, String age, String gender, boolean trainerEnabled) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setName(name);
            user.setAge(age);
            user.setGender(gender);
            user.setTrainerEnabled(trainerEnabled);
            userRepository.save(user);
        }
        return user;
    }

    public void updateFcmToken(int userId, String token) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setFcmToken(token);
            userRepository.save(user);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(Map<String, Object> payload) {
        User user = new User();
        user.setUsername((String) payload.get("username"));
        user.setPassword(passwordEncoder.encode((String) payload.get("password")));
        user.setName((String) payload.getOrDefault("name", ""));
        user.setAge((String) payload.getOrDefault("age", ""));
        user.setGender((String) payload.getOrDefault("gender", "male"));
        user.setRole((String) payload.getOrDefault("role", "CLIENT"));
        user.setTrainerEnabled(false);

        Object trainerIdObj = payload.get("trainerId");
        if (trainerIdObj != null && trainerIdObj instanceof Number) {
            int trainerId = ((Number) trainerIdObj).intValue();
            User trainer = userRepository.findById(trainerId).orElse(null);
            if (trainer != null && "TRAINER".equals(trainer.getRole())) {
                user.setTrainer(trainer);
            }
        }
        user.setLastModified(System.currentTimeMillis());
        return userRepository.save(user);
    }

    public User updateUserByAdmin(int id, Map<String, Object> payload) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            if (payload.containsKey("username")) user.setUsername((String) payload.get("username"));
            if (payload.containsKey("password")) {
                String pass = (String) payload.get("password");
                if (pass != null && !pass.isEmpty()) {
                    user.setPassword(passwordEncoder.encode(pass));
                }
            }
            if (payload.containsKey("name")) user.setName((String) payload.get("name"));
            if (payload.containsKey("age")) user.setAge((String) payload.get("age"));
            if (payload.containsKey("gender")) user.setGender((String) payload.get("gender"));
            if (payload.containsKey("role")) user.setRole((String) payload.get("role"));

            if (payload.containsKey("trainerId")) {
                Object trainerIdObj = payload.get("trainerId");
                if (trainerIdObj != null && trainerIdObj instanceof Number) {
                    int trainerId = ((Number) trainerIdObj).intValue();
                    User trainer = userRepository.findById(trainerId).orElse(null);
                    if (trainer != null && "TRAINER".equals(trainer.getRole())) {
                        user.setTrainer(trainer);
                    } else {
                        user.setTrainer(null);
                    }
                } else {
                    user.setTrainer(null);
                }
            }
            user.setLastModified(System.currentTimeMillis());
            userRepository.save(user);

            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                fcmService.sendSyncNotification(user.getFcmToken());
            }
        }
        return user;
    }

    @Transactional
    public void deleteUser(int id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            // 1. Открепляем клиентов от тренера
            List<User> clients = userRepository.findByTrainerId(id);
            for (User client : clients) {
                client.setTrainer(null);
                userRepository.save(client);
            }

            // 2. Удаляем все записи ClientBooking, где пользователь клиент или тренер
            // Удаляем как клиента
            List<ClientBooking> bookingsAsClient = bookingRepo.findByClientIdAndActiveTrue(id);
            bookingRepo.deleteAll(bookingsAsClient);
            // Удаляем все бронирования клиента (включая неактивные) – на всякий случай
            // Можно использовать deleteAllByClientId, если добавить. Пока так.
            bookingRepo.deleteAll(bookingRepo.findByClientIdAndActiveTrue(id)); // уже пусто

            // Удаляем бронирования, где пользователь - тренер
            if ("TRAINER".equals(user.getRole())) {
                List<TrainerWorkingDay> workingDays = workingDayRepo.findByTrainerIdAndDateBetween(id,
                        LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
                for (TrainerWorkingDay wd : workingDays) {
                    List<ClientBooking> bookings = bookingRepo.findByTrainerIdAndBookingDateAndActiveTrue(id, wd.getDate());
                    bookingRepo.deleteAll(bookings);
                }
            }

            // 3. Удаляем шаблоны расписания и рабочие дни (если тренер)
            if ("TRAINER".equals(user.getRole())) {
                List<TrainerScheduleTemplate> templates = templateRepo.findByTrainerId(id);
                for (TrainerScheduleTemplate t : templates) {
                    templateRepo.delete(t);
                }
                List<TrainerWorkingDay> allWorkingDays = workingDayRepo.findByTrainerIdAndDateBetween(id,
                        LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
                workingDayRepo.deleteAll(allWorkingDays);
            }

            // 4. Удаляем дневник
            List<DiaryEntry> diaryEntries = diaryEntryRepo.findByUserId(id);
            for (DiaryEntry entry : diaryEntries) {
                List<DiaryExercise> diaryExercises = diaryExerciseRepo.findByDiaryEntryId(entry.getId());
                for (DiaryExercise de : diaryExercises) {
                    diarySetRepo.deleteAllByDiaryExerciseId(de.getId());
                }
                diaryExerciseRepo.deleteAllByDiaryEntryId(entry.getId());
            }
            diaryEntryRepo.deleteAllByUserId(id);

            // 5. Удаляем программы и упражнения
            List<Program> programs = programRepo.findByUserId(id);
            for (Program p : programs) {
                List<TrainingDay> days = dayRepo.findByProgramId(p.getId());
                for (TrainingDay day : days) {
                    List<Exercise> exercises = exerciseRepo.findByTrainingDayId(day.getId());
                    for (Exercise ex : exercises) {
                        setRepo.deleteAllByExerciseId(ex.getId());
                    }
                    exerciseRepo.deleteAllByTrainingDayId(day.getId());
                }
                dayRepo.deleteAllByProgramId(p.getId());
            }
            programRepo.deleteAllByUserId(id);

            dateRepo.deleteAllByUserId(id);
            sessionRepo.deleteAllByUserId(id);

            // 6. Удаляем самого пользователя
            userRepository.deleteById(id);
        }
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean validateTrainerAccess(int trainerId, int clientId) {
        User trainer = userRepository.findById(trainerId).orElse(null);
        User client = userRepository.findById(clientId).orElse(null);
        if (trainer == null || client == null) return false;
        if (!"TRAINER".equals(trainer.getRole())) return false;
        if (!"CLIENT".equals(client.getRole())) return false;
        return client.getTrainer() != null && client.getTrainer().getId() == trainerId;
    }
}