package com.cardi.cardi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String username;
    @Transient
    private List<Card> hand = new ArrayList<>();
    private int wins = 0;
    @Transient
    private boolean hasCalledCardi = false;
    @Transient
    @Setter
    private String sessionId;

    public Player(String username) {
        this.username = username;
    }

    public Player(String id, String username) {
        this.id = id;
        this.username = username;
        this.hand = new ArrayList<>();
        this.wins = 0;
    }

    public Player(String id, String username, String sessionId) {
        this.id = id;
        this.username = username;
        this.sessionId = sessionId;
        this.hand = new ArrayList<>();
        this.wins = 0;
    }
}
