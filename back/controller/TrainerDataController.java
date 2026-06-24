package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.*;
import com.example.fitnesshelper.repository.*;
import com.example.fitnesshelper.service.FcmService;
import com.example.fitnesshelper.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/trainer/clients/{clientId}")
public class TrainerDataController {

    private final UserService userService;
    private final FcmService fcmService;
    private final ProgramRepository programRepo;
    private final TrainingDayRepository dayRepo;
    private final ExerciseRepository exerciseRepo;
    private final ExerciseSetRepository setRepo;
    private final DiaryEntryRepository diaryEntryRepo;
    private final DiaryExerciseRepository diaryExerciseRepo;
    private final DiaryExerciseSetRepository diarySetRepo;

    public TrainerDataController(UserService userService, FcmService fcmService,
                                 ProgramRepository programRepo, TrainingDayRepository dayRepo,
                                 ExerciseRepository exerciseRepo, ExerciseSetRepository setRepo,
                                 DiaryEntryRepository diaryEntryRepo, DiaryExerciseRepository diaryExerciseRepo,
                                 DiaryExerciseSetRepository diarySetRepo) {
        this.userService = userService;
        this.fcmService = fcmService;
        this.programRepo = programRepo;
        this.dayRepo = dayRepo;
        this.exerciseRepo = exerciseRepo;
        this.setRepo = setRepo;
        this.diaryEntryRepo = diaryEntryRepo;
        this.diaryExerciseRepo = diaryExerciseRepo;
        this.diarySetRepo = diarySetRepo;
    }

    // ---------- общие проверки ----------
    private void checkAccess(HttpServletRequest request, int clientId) {
        int trainerId = (int) request.getAttribute("userId");
        if (!userService.validateTrainerAccess(trainerId, clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void sendSyncToClient(int clientId) {
        User client = userService.findById(clientId);
        if (client != null && client.getFcmToken() != null && !client.getFcmToken().isEmpty()) {
            fcmService.sendSyncNotification(client.getFcmToken());
        }
    }

    // ================== Программы ==================
    @GetMapping("/programs")
    public List<Program> getPrograms(@PathVariable int clientId, HttpServletRequest request) {
        checkAccess(request, clientId);
        return programRepo.findByUserId(clientId);
    }

    @PostMapping("/programs")
    public Program createProgram(@PathVariable int clientId, @RequestBody Program program, HttpServletRequest request) {
        checkAccess(request, clientId);
        if (programRepo.countByUserId(clientId) >= 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 3 programs");
        }
        program.setUser(userService.findById(clientId));
        Program saved = programRepo.save(program);
        sendSyncToClient(clientId);
        return saved;
    }

    @DeleteMapping("/programs/{programId}")
    public void deleteProgram(@PathVariable int clientId, @PathVariable int programId, HttpServletRequest request) {
        checkAccess(request, clientId);
        Program p = programRepo.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        programRepo.delete(p);
        sendSyncToClient(clientId);
    }

    // ================== Тренировочные дни ==================
    @GetMapping("/programs/{programId}/days")
    public List<TrainingDay> getDays(@PathVariable int clientId, @PathVariable int programId, HttpServletRequest request) {
        checkAccess(request, clientId);
        Program p = programRepo.findById(programId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return dayRepo.findByProgramId(programId);
    }

    @PostMapping("/programs/{programId}/days")
    public TrainingDay createDay(@PathVariable int clientId, @PathVariable int programId,
                                 @RequestBody TrainingDay day, HttpServletRequest request) {
        checkAccess(request, clientId);
        Program p = programRepo.findById(programId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (dayRepo.countByProgramId(programId) >= 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 8 days");
        day.setProgram(p);
        TrainingDay saved = dayRepo.save(day);
        sendSyncToClient(clientId);
        return saved;
    }

    @DeleteMapping("/programs/{programId}/days/{dayId}")
    public void deleteDay(@PathVariable int clientId, @PathVariable int programId, @PathVariable int dayId, HttpServletRequest request) {
        checkAccess(request, clientId);
        Program p = programRepo.findById(programId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        TrainingDay day = dayRepo.findById(dayId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (day.getProgram().getId() != programId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        dayRepo.delete(day);
        sendSyncToClient(clientId);
    }

    // ================== Упражнения ==================
    @GetMapping("/days/{dayId}/exercises")
    public List<Exercise> getExercises(@PathVariable int clientId, @PathVariable int dayId, HttpServletRequest request) {
        checkAccess(request, clientId);
        TrainingDay day = dayRepo.findById(dayId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (day.getProgram().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return exerciseRepo.findByTrainingDayIdOrderByOrderIndex(dayId);
    }

    @PostMapping("/days/{dayId}/exercises")
    public Exercise createExercise(@PathVariable int clientId, @PathVariable int dayId,
                                   @RequestBody Exercise exercise, HttpServletRequest request) {
        checkAccess(request, clientId);
        TrainingDay day = dayRepo.findById(dayId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (day.getProgram().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (exerciseRepo.findByTrainingDayIdOrderByOrderIndex(dayId).size() >= 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 10 exercises");
        }
        exercise.setTrainingDay(day);
        Exercise saved = exerciseRepo.save(exercise);
        sendSyncToClient(clientId);
        return saved;
    }

    @PutMapping("/days/{dayId}/exercises/{exerciseId}")
    public Exercise updateExercise(@PathVariable int clientId, @PathVariable int dayId, @PathVariable int exerciseId,
                                   @RequestBody Exercise updated, HttpServletRequest request) {
        checkAccess(request, clientId);
        TrainingDay day = dayRepo.findById(dayId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (day.getProgram().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Exercise ex = exerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getTrainingDay().getId() != dayId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        ex.setName(updated.getName());
        ex.setMuscleGroup(updated.getMuscleGroup());
        ex.setOrderIndex(updated.getOrderIndex());
        Exercise saved = exerciseRepo.save(ex);
        sendSyncToClient(clientId);
        return saved;
    }

    @DeleteMapping("/days/{dayId}/exercises/{exerciseId}")
    public void deleteExercise(@PathVariable int clientId, @PathVariable int dayId, @PathVariable int exerciseId,
                               HttpServletRequest request) {
        checkAccess(request, clientId);
        TrainingDay day = dayRepo.findById(dayId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (day.getProgram().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Exercise ex = exerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getTrainingDay().getId() != dayId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        exerciseRepo.delete(ex);
        sendSyncToClient(clientId);
    }

    // ================== Подходы упражнений ==================
    @GetMapping("/exercises/{exerciseId}/sets")
    public List<ExerciseSet> getSets(@PathVariable int clientId, @PathVariable int exerciseId, HttpServletRequest request) {
        checkAccess(request, clientId);
        Exercise ex = exerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getTrainingDay().getProgram().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return setRepo.findByExerciseIdOrderBySetNumber(exerciseId);
    }

    @PostMapping("/exercises/{exerciseId}/sets")
    public ExerciseSet createSet(@PathVariable int clientId, @PathVariable int exerciseId,
                                 @RequestBody ExerciseSet set, HttpServletRequest request) {
        checkAccess(request, clientId);
        Exercise ex = exerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getTrainingDay().getProgram().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        set.setExercise(ex);
        ExerciseSet saved = setRepo.save(set);
        sendSyncToClient(clientId);
        return saved;
    }

    @PutMapping("/exercises/{exerciseId}/sets/{setId}")
    public ExerciseSet updateSet(@PathVariable int clientId, @PathVariable int exerciseId, @PathVariable int setId,
                                 @RequestBody ExerciseSet updated, HttpServletRequest request) {
        checkAccess(request, clientId);
        Exercise ex = exerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getTrainingDay().getProgram().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        ExerciseSet set = setRepo.findById(setId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (set.getExercise().getId() != exerciseId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        set.setReps(updated.getReps());
        set.setWeight(updated.getWeight());
        set.setSetNumber(updated.getSetNumber());
        ExerciseSet saved = setRepo.save(set);
        sendSyncToClient(clientId);
        return saved;
    }

    @DeleteMapping("/exercises/{exerciseId}/sets/{setId}")
    public void deleteSet(@PathVariable int clientId, @PathVariable int exerciseId, @PathVariable int setId,
                          HttpServletRequest request) {
        checkAccess(request, clientId);
        Exercise ex = exerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getTrainingDay().getProgram().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        ExerciseSet set = setRepo.findById(setId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (set.getExercise().getId() != exerciseId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        setRepo.delete(set);
        sendSyncToClient(clientId);
    }

    // ================== Дневник ==================
    @GetMapping("/diary-entries")
    public List<DiaryEntry> getDiaryEntries(@PathVariable int clientId, HttpServletRequest request) {
        checkAccess(request, clientId);
        return diaryEntryRepo.findByUserIdOrderByDateDesc(clientId);
    }

    @PostMapping("/diary-entries")
    public DiaryEntry createDiaryEntry(@PathVariable int clientId, @RequestBody DiaryEntry entry, HttpServletRequest request) {
        checkAccess(request, clientId);
        entry.setUser(userService.findById(clientId));
        DiaryEntry saved = diaryEntryRepo.save(entry);
        sendSyncToClient(clientId);
        return saved;
    }

    @DeleteMapping("/diary-entries/{entryId}")
    public void deleteDiaryEntry(@PathVariable int clientId, @PathVariable int entryId, HttpServletRequest request) {
        checkAccess(request, clientId);
        DiaryEntry e = diaryEntryRepo.findById(entryId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (e.getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        diaryEntryRepo.delete(e);
        sendSyncToClient(clientId);
    }

    // ================== Упражнения дневника ==================
    @GetMapping("/diary-entries/{entryId}/exercises")
    public List<DiaryExercise> getDiaryExercises(@PathVariable int clientId, @PathVariable int entryId, HttpServletRequest request) {
        checkAccess(request, clientId);
        DiaryEntry entry = diaryEntryRepo.findById(entryId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (entry.getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return diaryExerciseRepo.findByDiaryEntryIdOrderByOrderIndex(entryId);
    }

    @PostMapping("/diary-entries/{entryId}/exercises")
    public DiaryExercise createDiaryExercise(@PathVariable int clientId, @PathVariable int entryId,
                                             @RequestBody DiaryExercise exercise, HttpServletRequest request) {
        checkAccess(request, clientId);
        DiaryEntry entry = diaryEntryRepo.findById(entryId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (entry.getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        exercise.setDiaryEntry(entry);
        DiaryExercise saved = diaryExerciseRepo.save(exercise);
        sendSyncToClient(clientId);
        return saved;
    }

    @PutMapping("/diary-entries/{entryId}/exercises/{exerciseId}")
    public DiaryExercise updateDiaryExercise(@PathVariable int clientId, @PathVariable int entryId, @PathVariable int exerciseId,
                                             @RequestBody DiaryExercise updated, HttpServletRequest request) {
        checkAccess(request, clientId);
        DiaryEntry entry = diaryEntryRepo.findById(entryId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (entry.getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        DiaryExercise ex = diaryExerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getDiaryEntry().getId() != entryId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        ex.setName(updated.getName());
        ex.setMuscleGroup(updated.getMuscleGroup());
        ex.setOrderIndex(updated.getOrderIndex());
        DiaryExercise saved = diaryExerciseRepo.save(ex);
        sendSyncToClient(clientId);
        return saved;
    }

    @DeleteMapping("/diary-entries/{entryId}/exercises/{exerciseId}")
    public void deleteDiaryExercise(@PathVariable int clientId, @PathVariable int entryId, @PathVariable int exerciseId,
                                    HttpServletRequest request) {
        checkAccess(request, clientId);
        DiaryEntry entry = diaryEntryRepo.findById(entryId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (entry.getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        DiaryExercise ex = diaryExerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getDiaryEntry().getId() != entryId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        diaryExerciseRepo.delete(ex);
        sendSyncToClient(clientId);
    }

    // ================== Подходы дневника ==================
    @GetMapping("/diary-exercises/{exerciseId}/sets")
    public List<DiaryExerciseSet> getDiarySets(@PathVariable int clientId, @PathVariable int exerciseId, HttpServletRequest request) {
        checkAccess(request, clientId);
        DiaryExercise ex = diaryExerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getDiaryEntry().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return diarySetRepo.findByDiaryExerciseIdOrderBySetNumber(exerciseId);
    }

    @PostMapping("/diary-exercises/{exerciseId}/sets")
    public DiaryExerciseSet createDiarySet(@PathVariable int clientId, @PathVariable int exerciseId,
                                           @RequestBody DiaryExerciseSet set, HttpServletRequest request) {
        checkAccess(request, clientId);
        DiaryExercise ex = diaryExerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getDiaryEntry().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        set.setDiaryExercise(ex);
        DiaryExerciseSet saved = diarySetRepo.save(set);
        sendSyncToClient(clientId);
        return saved;
    }

    @PutMapping("/diary-exercises/{exerciseId}/sets/{setId}")
    public DiaryExerciseSet updateDiarySet(@PathVariable int clientId, @PathVariable int exerciseId, @PathVariable int setId,
                                           @RequestBody DiaryExerciseSet updated, HttpServletRequest request) {
        checkAccess(request, clientId);
        DiaryExercise ex = diaryExerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getDiaryEntry().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        DiaryExerciseSet set = diarySetRepo.findById(setId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (set.getDiaryExercise().getId() != exerciseId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        set.setReps(updated.getReps());
        set.setWeight(updated.getWeight());
        set.setSetNumber(updated.getSetNumber());
        DiaryExerciseSet saved = diarySetRepo.save(set);
        sendSyncToClient(clientId);
        return saved;
    }

    @DeleteMapping("/diary-exercises/{exerciseId}/sets/{setId}")
    public void deleteDiarySet(@PathVariable int clientId, @PathVariable int exerciseId, @PathVariable int setId,
                               HttpServletRequest request) {
        checkAccess(request, clientId);
        DiaryExercise ex = diaryExerciseRepo.findById(exerciseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getDiaryEntry().getUser().getId() != clientId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        DiaryExerciseSet set = diarySetRepo.findById(setId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (set.getDiaryExercise().getId() != exerciseId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        diarySetRepo.delete(set);
        sendSyncToClient(clientId);
    }
}