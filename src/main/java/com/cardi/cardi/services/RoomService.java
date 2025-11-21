package com.cardi.cardi.services;

import com.cardi.cardi.model.GameRoom;
import com.cardi.cardi.model.GameState;
import com.cardi.cardi.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private static final int MAX_PLAYERS = 6;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Creates a new game room, adds the creator as the first player,
     * and sends the initial game state directly back to the creator.
     *
     * @param creatorUsername The username of the player creating the room.
     * @param sessionId The WebSocket session ID of the creator.
     */
    public void createRoom(String creatorUsername, String sessionId) {
        String roomCode = generateRoomCode();
        GameRoom room = new GameRoom(roomCode);
        gameRooms.put(roomCode, room);

        Player player = new Player(generatePlayerId(), creatorUsername);
        room.addPlayer(player);
        room.setRoomOwnerId(player.getId()); // Set the room owner

        // Send the initial state directly to the creator
        GameState initialState = getGameState(roomCode, "Room created successfully.");
        
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-updates", initialState, headerAccessor.getMessageHeaders());
    }

    /**
     * Adds a player to an existing game room.
     *
     * @param roomCode The code of the room to join.
     * @param username The username of the player joining.
     */
    public void joinRoom(String roomCode, String username, String sessionId) {
        GameRoom room = getRoom(roomCode);
        if (room == null) {
            // TODO: Send error back to user that room doesn't exist.
            System.err.println("Attempted to join non-existent room: " + roomCode);
            return;
        }

        if (room.isStarted()) {
            // TODO: Send error back to user that game has started.
            System.err.println("Attempted to join room that has already started: " + roomCode);
            return;
        }
        
        // Handle re-joining
        if (room.getPlayers().stream().anyMatch(p -> p.getUsername().equalsIgnoreCase(username))) {
            System.err.println("Player " + username + " is already in room " + roomCode);
            
            // Send the current state privately to the re-joining user
            GameState initialState = getGameState(roomCode, "Re-joined room " + roomCode);
            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headerAccessor.setSessionId(sessionId);
            headerAccessor.setLeaveMutable(true);
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-updates", initialState, headerAccessor.getMessageHeaders());

            // Notify others
            broadcastGameState(roomCode, username + " re-joined the room.");
            return;
        }

        if (room.getPlayers().size() >= MAX_PLAYERS) {
            // TODO: Send error back to user that room is full.
            System.err.println("Attempted to join full room: " + roomCode);
            return;
        }

        Player player = new Player(generatePlayerId(), username);
        room.addPlayer(player);

        // Send the initial state directly to the joining player
        GameState initialState = getGameState(roomCode, "Joined room successfully.");
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-updates", initialState, headerAccessor.getMessageHeaders());
        
        // Broadcast the updated state to everyone in the room
        broadcastGameState(roomCode, username + " has joined the room.");
    }

    public GameRoom getRoom(String roomCode) {
        return gameRooms.get(roomCode);
    }

    public void removePlayer(String roomCode, String playerId) {
        GameRoom room = getRoom(roomCode);
        if (room != null) {
            String username = room.getPlayerById(playerId).getUsername();
            room.getPlayers().removeIf(p -> p.getId().equals(playerId));
            if (room.getPlayers().isEmpty()) {
                gameRooms.remove(roomCode);
            } else {
                broadcastGameState(roomCode, username + " has left the room.");
            }
        }
    }

    private String generateRoomCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (gameRooms.containsKey(code));
        return code;
    }

    private String generatePlayerId() {
        return UUID.randomUUID().toString();
    }

    private GameState getGameState(String roomCode, String message) {
        GameRoom room = getRoom(roomCode);
        if (room == null) return null;

        return new GameState(
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
    }

    private void broadcastGameState(String roomCode, String message) {
        GameState state = getGameState(roomCode, message);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, state);
    }
}