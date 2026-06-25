package com.example.fitnesshelper.dto;

import java.io.Serializable;
import java.util.List;

public class TrainerScheduleDay implements Serializable {
    public int id;
    public int dayOfWeek;
    public List<Integer> enabledHours;
}