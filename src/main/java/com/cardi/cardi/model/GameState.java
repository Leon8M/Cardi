package com.cardi.cardi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    private String roomCode;
    private String roomOwnerId;
    private List<Player> players;
    private Card topCard;
    private int currentPlayerIndex;
    private boolean isReversed;
    private boolean started;
    private String message;
    private int drawPenalty;
    private boolean playerHasTakenAction;
    private boolean questionActive;
    private String activeSuit;
}