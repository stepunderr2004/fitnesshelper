package com.example.fitnesshelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    private String name;
    private String age;
    private String gender;
    private boolean trainerEnabled;

    @Column(nullable = false)
    private String role = "CLIENT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainerId")
    @JsonIgnore
    private User trainer;

    @OneToMany(mappedBy = "trainer", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<User> clients = new ArrayList<>();

    // FCM-токен
    @Column(length = 512)
    @JsonIgnore
    private String fcmToken;

    // Refresh-токен для автоматического продления сессии
    @Column(length = 512)
    @JsonIgnore
    private String refreshToken;

    private Long lastModified;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastModified = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return username != null ? username : "";
    }
}