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
/**
 * Represents a fearless player in the Cardi game.
 * Each player is a unique combination of skill, luck, and a growing pile of cards.
 */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // The player's unique identifier, their secret agent codename.
    private String username; // The name they bravely (or anonymously) choose for battle.
    @Transient
    private List<Card> hand = new ArrayList<>(); // The collection of powerful (or pitiful) cards in their possession.
    private int wins = 0; // The tally of glorious victories this player has achieved.
    @Transient
    private boolean hasCalledCardi = false; // True if they've declared "Cardi!" (and hopefully not too early).
    @Transient
    @Setter
    private String sessionId; // Their current connection to the game's heartbeat.

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
