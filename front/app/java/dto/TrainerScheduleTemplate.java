package com.example.fitnesshelper.dto;

import java.io.Serializable;
import java.util.List;

public class TrainerScheduleTemplate implements Serializable {
    public int id;
    public String name;
    public boolean allHoursEnabled;
    public List<TrainerScheduleDay> days;
}