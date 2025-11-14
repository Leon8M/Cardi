package com.cardi.cardi.model;

import lombok.Data;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class GameRoom {
    private String roomCode;
    private List<Player> players = new CopyOnWriteArrayList<>();
    private Stack<Card> drawPile = new Stack<>();
    private Stack<Card> playedPile = new Stack<>();
    private int currentPlayerIndex = 0;
    private boolean isReversed = false;
    private boolean started = false;

    // Configurable room rules
    private boolean matchShapeForCounter = false;
    private Integer maxCardsAllowed = null;
    private boolean restrictJKCounters = false;

    // Dynamic game state
    private int drawPenalty = 0;
    private boolean questionActive = false;
    private String lastPlayerIdToDraw;
    private String activeSuit; // For Ace card
    private boolean skipNextTurn = false;
    private boolean playerHasTakenAction = false;

    public GameRoom(String roomCode) {
        this.roomCode = roomCode;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public Player getPlayerById(String playerId) {
        return this.players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public Card getTopCard() {
        if (playedPile.isEmpty()) {
            return null;
        }
        return playedPile.peek();
    }
}
