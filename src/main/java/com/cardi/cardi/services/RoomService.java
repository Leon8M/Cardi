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

    public GameRoom createRoom() {
        String roomCode = generateRoomCode();
        GameRoom gameRoom = new GameRoom(roomCode);
        gameRooms.put(roomCode, gameRoom);
        return gameRoom;
    }

    public GameRoom joinRoom(String roomCode, Player player) {
        GameRoom gameRoom = gameRooms.get(roomCode);
        if (gameRoom != null && !gameRoom.isStarted() && gameRoom.getPlayers().size() < 6) {
            gameRoom.addPlayer(player);
            return gameRoom;
        }
        return null; // Or throw an exception
    }

    public GameRoom getRoom(String roomCode) {
        return gameRooms.get(roomCode);
    }

    public void removePlayer(String roomCode, String playerId) {
        GameRoom gameRoom = gameRooms.get(roomCode);
        if (gameRoom != null) {
            gameRoom.getPlayers().removeIf(p -> p.getId().equals(playerId));
            if (gameRoom.getPlayers().isEmpty()) {
                gameRooms.remove(roomCode);
            }
        }
    }

    private String generateRoomCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
