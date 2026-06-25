package com.example.fitnesshelper.dto;

import java.util.List;

public class WeeklyScheduleDay {
    public int id;
    public int dayOfWeek;
    public String date;
    public List<Integer> enabledHours;
}