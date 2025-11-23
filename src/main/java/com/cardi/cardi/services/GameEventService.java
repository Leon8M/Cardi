package com.cardi.cardi.services;

import com.cardi.cardi.model.GameEvent;
import com.cardi.cardi.model.GameRoom;
import com.cardi.cardi.model.GameState;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GameEventService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;

    public GameEventService(SimpMessagingTemplate messagingTemplate, @Lazy RoomService roomService) {
        this.messagingTemplate = messagingTemplate;
        this.roomService = roomService;
    }

    public void sendGameStateUpdate(String roomCode, String message) {
        GameRoom room = roomService.getRoom(roomCode);
        if (room == null) return;

        GameState state = new GameState(
            room.getRoomCode(),
            room.getRoomOwnerId(),
            room.getPlayers(),
            room.getTopCard(),
            room.getCurrentPlayerIndex(),
            room.isReversed(),
            room.isStarted(),
            message,
            room.getDrawPenalty(),
            room.isPlayerHasTakenAction(),
            room.isQuestionActive(),
            room.getActiveSuit()
        );

        GameEvent event = new GameEvent(GameEvent.EventType.GAME_STATE_UPDATE, state);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }
    
    public void sendRoomUpdate(String roomCode, String sessionId) {
        GameRoom room = roomService.getRoom(roomCode);
        if (room == null) return;

        GameEvent event = new GameEvent(GameEvent.EventType.ROOM_UPDATE, room);
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-updates", event, headerAccessor.getMessageHeaders());
    }

    public void sendErrorToPlayer(String sessionId, String message) {
        if (sessionId == null) return;
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        GameEvent event = new GameEvent(GameEvent.EventType.ERROR, message);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", event, headerAccessor.getMessageHeaders());
    }
    
    public void sendPlayerJoined(String roomCode, String username) {
        GameEvent event = new GameEvent(GameEvent.EventType.PLAYER_JOINED, Map.of("username", username));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    public void sendPlayerLeft(String roomCode, String username) {
        GameEvent event = new GameEvent(GameEvent.EventType.PLAYER_LEFT, Map.of("username", username));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    public void sendGameStart(String roomCode, GameState gameState) {
        GameEvent event = new GameEvent(GameEvent.EventType.GAME_START, gameState);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    public void sendCardPlayed(String roomCode, String playerId, java.util.List<com.cardi.cardi.model.Card> cards) {
        GameEvent event = new GameEvent(GameEvent.EventType.CARD_PLAYED, Map.of("playerId", playerId, "cards", cards));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    public void sendCardDrawn(String roomCode, String playerId, int numberOfCards) {
        GameEvent event = new GameEvent(GameEvent.EventType.CARD_DRAWN, Map.of("playerId", playerId, "numberOfCards", numberOfCards));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    public void sendTurnPassed(String roomCode, String playerId) {
        GameEvent event = new GameEvent(GameEvent.EventType.TURN_PASSED, Map.of("playerId", playerId));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    public void sendCardiCalled(String roomCode, String playerId) {
        GameEvent event = new GameEvent(GameEvent.EventType.CARDI_CALLED, Map.of("playerId", playerId));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    public void sendGameWin(String roomCode, String winnerUsername) {
        GameEvent event = new GameEvent(GameEvent.EventType.GAME_WIN, Map.of("winnerUsername", winnerUsername));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }
}
