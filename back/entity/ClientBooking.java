package com.example.fitnesshelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ClientBooking extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clientId", nullable = false)
    @JsonIgnore
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainerId", nullable = false)
    @JsonIgnore
    private User trainer;

    private LocalDate bookingDate;
    private int hour; // 9-22
    private boolean active = true;

    private LocalDateTime createdDate;

    @Transient
    @JsonProperty
    private String clientName;
}