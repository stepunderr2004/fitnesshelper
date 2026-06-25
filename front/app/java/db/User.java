package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.util.Map;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username;
    public String password;
    public String name;
    public String age;
    public String gender;
    public boolean trainerEnabled;

    public String role;          // ADMIN, TRAINER, CLIENT
    public Integer trainerId;    // ID тренера (null, если нет)

    @Ignore
    public User() {}

    public User(String username, String password, String name, String age, String gender, boolean trainerEnabled) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.trainerEnabled = trainerEnabled;
    }

    public static User fromMap(Map<String, Object> map) {
        User user = new User();
        user.id = map.get("id") != null ? ((Double) map.get("id")).intValue() : 0;
        user.username = (String) map.get("username");
        user.name = (String) map.get("name");
        user.age = (String) map.get("age");
        user.gender = (String) map.get("gender");
        user.trainerEnabled = map.get("trainerEnabled") != null && (Boolean) map.get("trainerEnabled");
        user.role = (String) map.get("role");
        user.trainerId = map.get("trainerId") != null ? ((Double) map.get("trainerId")).intValue() : null;
        return user;
    }

    @Override
    public String toString() {
        return username != null ? username : "";
    }
}