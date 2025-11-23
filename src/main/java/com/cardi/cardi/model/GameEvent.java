package com.cardi.cardi.model;

import lombok.Data;

@Data
public class GameEvent {
    private final EventType type;
    private final Object payload;

    public GameEvent(EventType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

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
