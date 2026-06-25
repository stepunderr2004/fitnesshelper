package com.example.fitnesshelper.network;

import com.example.fitnesshelper.db.*;
import com.example.fitnesshelper.dto.*;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;

public interface ApiService {

    // ========== Аутентификация ==========
    @POST("api/auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> body);
    @POST("api/auth/refresh")
    Call<Map<String, Object>> refresh(@Body Map<String, String> body);

    // ========== Программы (свои) ==========
    @GET("api/programs")
    Call<List<Program>> getPrograms();
    @POST("api/programs")
    Call<Program> createProgram(@Body Program program);
    @DELETE("api/programs/{id}")
    Call<Void> deleteProgram(@Path("id") int programId);

    // ========== Тренировочные дни (свои) ==========
    @GET("api/programs/{programId}/days")
    Call<List<TrainingDay>> getTrainingDays(@Path("programId") int programId);
    @POST("api/programs/{programId}/days")
    Call<TrainingDay> createTrainingDay(@Path("programId") int programId, @Body TrainingDay day);
    @DELETE("api/programs/{programId}/days/{dayId}")
    Call<Void> deleteTrainingDay(@Path("programId") int programId, @Path("dayId") int dayId);

    // ========== Упражнения (свои) ==========
    @GET("api/days/{dayId}/exercises")
    Call<List<Exercise>> getExercises(@Path("dayId") int dayId);
    @POST("api/days/{dayId}/exercises")
    Call<Exercise> createExercise(@Path("dayId") int dayId, @Body Exercise exercise);
    @PUT("api/days/{dayId}/exercises/{exerciseId}")
    Call<Exercise> updateExercise(@Path("dayId") int dayId, @Path("exerciseId") int exerciseId, @Body Exercise exercise);
    @DELETE("api/days/{dayId}/exercises/{exerciseId}")
    Call<Void> deleteExercise(@Path("dayId") int dayId, @Path("exerciseId") int exerciseId);

    // ========== Подходы упражнений (свои) ==========
    @GET("api/exercises/{exerciseId}/sets")
    Call<List<ExerciseSet>> getExerciseSets(@Path("exerciseId") int exerciseId);
    @POST("api/exercises/{exerciseId}/sets")
    Call<ExerciseSet> createExerciseSet(@Path("exerciseId") int exerciseId, @Body ExerciseSet set);
    @PUT("api/exercises/{exerciseId}/sets/{setId}")
    Call<ExerciseSet> updateExerciseSet(@Path("exerciseId") int exerciseId, @Path("setId") int setId, @Body ExerciseSet set);
    @DELETE("api/exercises/{exerciseId}/sets/{setId}")
    Call<Void> deleteExerciseSet(@Path("exerciseId") int exerciseId, @Path("setId") int setId);

    // ========== Записи к тренеру ==========
    @GET("api/training-dates")
    Call<List<TrainingDate>> getTrainingDates();
    @POST("api/training-dates")
    Call<TrainingDate> createTrainingDate(@Body TrainingDate date);
    @DELETE("api/training-dates/{id}")
    Call<Void> deleteTrainingDate(@Path("id") int id);

    // ========== Сессии тренировок ==========
    @GET("api/training-sessions/last")
    Call<TrainingSession> getLastTrainingSession();
    @GET("api/training-sessions/last")
    Call<ResponseBody> getLastTrainingSessionAsBody();
    @POST("api/training-sessions")
    Call<TrainingSession> createTrainingSession(@Body TrainingSession session);

    // ========== Дневник (свой) ==========
    @GET("api/diary-entries")
    Call<List<DiaryEntry>> getDiaryEntries();
    @POST("api/diary-entries")
    Call<DiaryEntry> createDiaryEntry(@Body DiaryEntry entry);
    @DELETE("api/diary-entries/{id}")
    Call<Void> deleteDiaryEntry(@Path("id") int entryId);

    // ========== Упражнения дневника (свои) ==========
    @GET("api/diary-entries/{entryId}/exercises")
    Call<List<DiaryExercise>> getDiaryExercises(@Path("entryId") int entryId);
    @POST("api/diary-entries/{entryId}/exercises")
    Call<DiaryExercise> createDiaryExercise(@Path("entryId") int entryId, @Body DiaryExercise exercise);
    @PUT("api/diary-entries/{entryId}/exercises/{exerciseId}")
    Call<DiaryExercise> updateDiaryExercise(@Path("entryId") int entryId, @Path("exerciseId") int exerciseId, @Body DiaryExercise exercise);
    @DELETE("api/diary-entries/{entryId}/exercises/{exerciseId}")
    Call<Void> deleteDiaryExercise(@Path("entryId") int entryId, @Path("exerciseId") int exerciseId);

    // ========== Подходы дневника (свои) ==========
    @GET("api/diary-exercises/{exerciseId}/sets")
    Call<List<DiaryExerciseSet>> getDiaryExerciseSets(@Path("exerciseId") int exerciseId);
    @POST("api/diary-exercises/{exerciseId}/sets")
    Call<DiaryExerciseSet> createDiaryExerciseSet(@Path("exerciseId") int exerciseId, @Body DiaryExerciseSet set);
    @PUT("api/diary-exercises/{exerciseId}/sets/{setId}")
    Call<DiaryExerciseSet> updateDiaryExerciseSet(@Path("exerciseId") int exerciseId, @Path("setId") int setId, @Body DiaryExerciseSet set);
    @DELETE("api/diary-exercises/{exerciseId}/sets/{setId}")
    Call<Void> deleteDiaryExerciseSet(@Path("exerciseId") int exerciseId, @Path("setId") int setId);

    // ========== Пользователь ==========
    @GET("api/users/{id}")
    Call<Map<String, Object>> getUser(@Path("id") int userId);
    @PUT("api/users/{id}")
    Call<User> updateUser(@Path("id") int userId, @Body User user);
    @GET("api/users/{userId}/last-modified")
    Call<Long> getLastModified(@Path("userId") int userId);
    @PUT("api/users/{id}/fcm-token")
    Call<Void> updateFcmToken(@Path("id") int userId, @Body Map<String, String> body);

    // ========== Администрирование ==========
    @GET("api/admin/users")
    Call<List<Map<String, Object>>> getAllUsers();
    @POST("api/admin/users")
    Call<Map<String, Object>> createUser(@Body Map<String, Object> body);
    @PUT("api/admin/users/{id}")
    Call<Map<String, Object>> updateUserByAdmin(@Path("id") int userId, @Body Map<String, Object> body);
    @DELETE("api/admin/users/{id}")
    Call<Void> deleteUser(@Path("id") int userId);

    // ========== Тренерские методы ==========
    @GET("api/trainer/clients")
    Call<Map<String, List<Map<String, Object>>>> getTrainerClients();
    @PUT("api/trainer/clients/{clientId}/assign")
    Call<Void> assignClient(@Path("clientId") int clientId);
    @DELETE("api/trainer/clients/{clientId}/unassign")
    Call<Void> unassignClient(@Path("clientId") int clientId);

    // ========== Тренерские методы для данных клиента ==========
    @GET("api/trainer/clients/{clientId}/programs")
    Call<List<Program>> getProgramsForClient(@Path("clientId") int clientId);
    @POST("api/trainer/clients/{clientId}/programs")
    Call<Program> createProgramForClient(@Path("clientId") int clientId, @Body Program program);
    @DELETE("api/trainer/clients/{clientId}/programs/{programId}")
    Call<Void> deleteProgramForClient(@Path("clientId") int clientId, @Path("programId") int programId);

    @DELETE("api/trainer/schedule/working-days/{dayId}")
    Call<Void> deleteWorkingDay(@Path("dayId") int dayId);

    @DELETE("api/trainer/schedule/bookings/{id}/trainer") // новый endpoint для тренера
    Call<Void> cancelBookingForTrainer(@Path("id") int bookingId);

    @GET("api/trainer/clients/{clientId}/programs/{programId}/days")
    Call<List<TrainingDay>> getTrainingDaysForClient(@Path("clientId") int clientId, @Path("programId") int programId);
    @POST("api/trainer/clients/{clientId}/programs/{programId}/days")
    Call<TrainingDay> createTrainingDayForClient(@Path("clientId") int clientId, @Path("programId") int programId, @Body TrainingDay day);
    @DELETE("api/trainer/clients/{clientId}/programs/{programId}/days/{dayId}")
    Call<Void> deleteTrainingDayForClient(@Path("clientId") int clientId, @Path("programId") int programId, @Path("dayId") int dayId);

    @GET("api/trainer/clients/{clientId}/days/{dayId}/exercises")
    Call<List<Exercise>> getExercisesForClient(@Path("clientId") int clientId, @Path("dayId") int dayId);
    @POST("api/trainer/clients/{clientId}/days/{dayId}/exercises")
    Call<Exercise> createExerciseForClient(@Path("clientId") int clientId, @Path("dayId") int dayId, @Body Exercise exercise);
    @PUT("api/trainer/clients/{clientId}/days/{dayId}/exercises/{exerciseId}")
    Call<Exercise> updateExerciseForClient(@Path("clientId") int clientId, @Path("dayId") int dayId, @Path("exerciseId") int exerciseId, @Body Exercise exercise);
    @DELETE("api/trainer/clients/{clientId}/days/{dayId}/exercises/{exerciseId}")
    Call<Void> deleteExerciseForClient(@Path("clientId") int clientId, @Path("dayId") int dayId, @Path("exerciseId") int exerciseId);

    @GET("api/trainer/clients/{clientId}/exercises/{exerciseId}/sets")
    Call<List<ExerciseSet>> getExerciseSetsForClient(@Path("clientId") int clientId, @Path("exerciseId") int exerciseId);
    @POST("api/trainer/clients/{clientId}/exercises/{exerciseId}/sets")
    Call<ExerciseSet> createExerciseSetForClient(@Path("clientId") int clientId, @Path("exerciseId") int exerciseId, @Body ExerciseSet set);
    @PUT("api/trainer/clients/{clientId}/exercises/{exerciseId}/sets/{setId}")
    Call<ExerciseSet> updateExerciseSetForClient(@Path("clientId") int clientId, @Path("exerciseId") int exerciseId, @Path("setId") int setId, @Body ExerciseSet set);
    @DELETE("api/trainer/clients/{clientId}/exercises/{exerciseId}/sets/{setId}")
    Call<Void> deleteExerciseSetForClient(@Path("clientId") int clientId, @Path("exerciseId") int exerciseId, @Path("setId") int setId);

    @GET("api/trainer/clients/{clientId}/diary-entries")
    Call<List<DiaryEntry>> getDiaryEntriesForClient(@Path("clientId") int clientId);
    @POST("api/trainer/clients/{clientId}/diary-entries")
    Call<DiaryEntry> createDiaryEntryForClient(@Path("clientId") int clientId, @Body DiaryEntry entry);
    @DELETE("api/trainer/clients/{clientId}/diary-entries/{entryId}")
    Call<Void> deleteDiaryEntryForClient(@Path("clientId") int clientId, @Path("entryId") int entryId);

    @GET("api/trainer/clients/{clientId}/diary-entries/{entryId}/exercises")
    Call<List<DiaryExercise>> getDiaryExercisesForClient(@Path("clientId") int clientId, @Path("entryId") int entryId);
    @POST("api/trainer/clients/{clientId}/diary-entries/{entryId}/exercises")
    Call<DiaryExercise> createDiaryExerciseForClient(@Path("clientId") int clientId, @Path("entryId") int entryId, @Body DiaryExercise exercise);
    @PUT("api/trainer/clients/{clientId}/diary-entries/{entryId}/exercises/{exerciseId}")
    Call<DiaryExercise> updateDiaryExerciseForClient(@Path("clientId") int clientId, @Path("entryId") int entryId, @Path("exerciseId") int exerciseId, @Body DiaryExercise exercise);
    @DELETE("api/trainer/clients/{clientId}/diary-entries/{entryId}/exercises/{exerciseId}")
    Call<Void> deleteDiaryExerciseForClient(@Path("clientId") int clientId, @Path("entryId") int entryId, @Path("exerciseId") int exerciseId);

    @GET("api/trainer/clients/{clientId}/diary-exercises/{exerciseId}/sets")
    Call<List<DiaryExerciseSet>> getDiaryExerciseSetsForClient(@Path("clientId") int clientId, @Path("exerciseId") int exerciseId);
    @POST("api/trainer/clients/{clientId}/diary-exercises/{exerciseId}/sets")
    Call<DiaryExerciseSet> createDiaryExerciseSetForClient(@Path("clientId") int clientId, @Path("exerciseId") int exerciseId, @Body DiaryExerciseSet set);
    @PUT("api/trainer/clients/{clientId}/diary-exercises/{exerciseId}/sets/{setId}")
    Call<DiaryExerciseSet> updateDiaryExerciseSetForClient(@Path("clientId") int clientId, @Path("exerciseId") int exerciseId, @Path("setId") int setId, @Body DiaryExerciseSet set);
    @DELETE("api/trainer/clients/{clientId}/diary-exercises/{exerciseId}/sets/{setId}")
    Call<Void> deleteDiaryExerciseSetForClient(@Path("clientId") int clientId, @Path("exerciseId") int exerciseId, @Path("setId") int setId);

    // ========== Расписание тренера ==========
    @GET("api/trainer/schedule/templates")
    Call<List<TrainerScheduleTemplate>> getScheduleTemplates();

    @GET("api/trainer/schedule/templates/{id}")
    Call<TrainerScheduleTemplate> getScheduleTemplate(@Path("id") int templateId);

    @POST("api/trainer/schedule/templates")
    Call<TrainerScheduleTemplate> createScheduleTemplate(@Body Map<String, Object> body);

    @DELETE("api/trainer/schedule/templates/{id}")
    Call<Void> deleteScheduleTemplate(@Path("id") int templateId);

    @PUT("api/trainer/schedule/days/{dayId}/hours")
    Call<TrainerScheduleDay> toggleScheduleDayHour(@Path("dayId") int dayId, @Body Map<String, Object> body);

    @POST("api/trainer/schedule/apply-template")
    Call<List<TrainerWorkingDay>> applyScheduleTemplate(@Body Map<String, Object> body);

    @GET("api/trainer/schedule/working-days")
    Call<List<TrainerWorkingDay>> getWorkingDays(@Query("from") String from, @Query("to") String to);

    @PUT("api/trainer/schedule/working-days/{dayId}/hours")
    Call<TrainerWorkingDay> editWorkingDayHours(@Path("dayId") int dayId, @Body Map<String, Object> body);

    @GET("api/trainer/schedule/bookings")
    Call<List<ClientBooking>> getTrainerBookings(@Query("date") String date);

    // ========== Клиент: запись на тренировку ==========
    @POST("api/trainer/schedule/book")
    Call<ClientBooking> bookTraining(@Body Map<String, Object> body);

    @DELETE("api/trainer/schedule/bookings/{id}")
    Call<Void> cancelBooking(@Path("id") int bookingId);

    @GET("api/trainer/schedule/my-bookings")
    Call<List<ClientBooking>> getMyBookings();
}