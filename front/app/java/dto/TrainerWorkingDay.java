package com.example.fitnesshelper.dto;

import java.util.List;

public class TrainerWorkingDay {
    public int id;
    public int trainerId;
    public String date;          // "yyyy-MM-dd"
    public List<Integer> enabledHours;
}