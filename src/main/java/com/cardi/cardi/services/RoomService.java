package com.cardi.cardi.services;

import com.cardi.cardi.model.GameRoom;
import com.cardi.cardi.model.Player;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private static final int MAX_PLAYERS = 6;

    private final GameEventService gameEventService;

    public RoomService(GameEventService gameEventService) {
        this.gameEventService = gameEventService;
    }

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
        gameEventService.sendRoomUpdate(roomCode, sessionId);
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
            gameEventService.sendErrorToPlayer(sessionId, "Room not found.");
            return;
        }

        if (room.isStarted()) {
            gameEventService.sendErrorToPlayer(sessionId, "Game has already started.");
            return;
        }
        
        // Handle re-joining
        if (room.getPlayers().stream().anyMatch(p -> p.getUsername().equalsIgnoreCase(username))) {
            gameEventService.sendErrorToPlayer(sessionId, "You are already in this room.");
            return;
        }

        if (room.getPlayers().size() >= MAX_PLAYERS) {
            gameEventService.sendErrorToPlayer(sessionId, "Room is full.");
            return;
        }

        Player player = new Player(generatePlayerId(), username);
        room.addPlayer(player);

        // Send the initial state directly to the joining player
        gameEventService.sendRoomUpdate(roomCode, sessionId);
        
        // Broadcast the updated state to everyone in the room
        gameEventService.sendPlayerJoined(roomCode, username);
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
                gameEventService.sendPlayerLeft(roomCode, username);
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
}