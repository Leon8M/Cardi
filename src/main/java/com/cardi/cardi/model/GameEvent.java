package com.cardi.cardi.model;

import lombok.Data;

@Data
public class GameEvent {
/**
 * Represents a single, dramatic event that unfolds in the game.
 * It's how the backend whispers (or shouts) secrets to the frontend.
 * Each event has a type, indicating what kind of chaos just occurred,
 * and a payload, carrying the juicy details.
 */
    private final EventType type;
    private final Object payload;

    public GameEvent(EventType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    /**
     * The grand menu of all possible happenings in our glorious card game.
     * From triumphant entries to heartbreaking defeats, it's all here!
     */
    public enum EventType {
        GAME_START,
        PLAYER_JOINED,
        PLAYER_LEFT,
        PLAYER_RECONNECTED,
        CARD_PLAYED,
        CARD_DRAWN,
        TURN_PASSED,
        CARDI_CALLED,
        GAME_WIN,
        ERROR,
        GAME_STATE_UPDATE,
        ROOM_UPDATE
    }
}
