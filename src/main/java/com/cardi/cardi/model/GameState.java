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
    private List<Player> players;
    private Card topCard;
    private int currentPlayerIndex;
    private boolean isReversed;
    private boolean isGameStarted;
    private String message;
}
