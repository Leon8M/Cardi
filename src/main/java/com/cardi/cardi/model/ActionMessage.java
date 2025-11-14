package com.cardi.cardi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionMessage {
    private String playerId;
    private String username;
    private String roomCode;
    private String action; // e.g., "PLAY", "DRAW", "CALL_CARDI"
    private List<Card> cards;
    private String newSuit; // For Ace card
}