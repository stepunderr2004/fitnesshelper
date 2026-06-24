package com.example.fitnesshelper.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter @Setter
public abstract class BaseEntity {
    private Long lastModified; // epoch millis

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastModified = System.currentTimeMillis();
    }
}