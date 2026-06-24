package com.example.fitnesshelper.service;

import com.example.fitnesshelper.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LastModifiedService {

    private final EntityManager em;

    public LastModifiedService(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public long getMaxLastModified(int userId) {
        long max = 0;

        // Программы – связь user (ManyToOne)
        max = Math.max(max, getMaxByUser("Program", "user.id", userId));
        // TrainingDay – program.user.id
        max = Math.max(max, getMaxByUser("TrainingDay", "program.user.id", userId));
        // Exercise – trainingDay.program.user.id
        max = Math.max(max, getMaxByUser("Exercise", "trainingDay.program.user.id", userId));
        // ExerciseSet – exercise.trainingDay.program.user.id
        max = Math.max(max, getMaxByUser("ExerciseSet", "exercise.trainingDay.program.user.id", userId));
        // TrainingDate – user.id
        max = Math.max(max, getMaxByUser("TrainingDate", "user.id", userId));
        // TrainingSession – user.id
        max = Math.max(max, getMaxByUser("TrainingSession", "user.id", userId));
        // DiaryEntry – user.id
        max = Math.max(max, getMaxByUser("DiaryEntry", "user.id", userId));
        // DiaryExercise – diaryEntry.user.id
        max = Math.max(max, getMaxByUser("DiaryExercise", "diaryEntry.user.id", userId));
        // DiaryExerciseSet – diaryExercise.diaryEntry.user.id
        max = Math.max(max, getMaxByUser("DiaryExerciseSet", "diaryExercise.diaryEntry.user.id", userId));

        // Пользователь
        User user = em.find(User.class, userId);
        if (user != null && user.getLastModified() != null) {
            max = Math.max(max, user.getLastModified());
        }
        return max;
    }

    private long getMaxByUser(String entityName, String userPath, int userId) {
        String jpql = "SELECT COALESCE(MAX(e.lastModified), 0) FROM " + entityName + " e WHERE e." + userPath + " = :userId";
        Query query = em.createQuery(jpql);
        query.setParameter("userId", userId);
        return (long) query.getSingleResult();
    }
}