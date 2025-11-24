package com.cardi.cardi.services;

import java.util.Optional;

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
     * Kicks off a new game room, crowning the creator as its first glorious ruler.
     * A welcome party (the initial game state) is sent directly to the creator.
     *
     * @param creatorUsername The mastermind behind this new chaos chamber.
     * @param sessionId The creator's secret handshake with the server.
     */

    public void createRoom(String creatorUsername, String sessionId) {

        String roomCode = generateRoomCode();

        GameRoom room = new GameRoom(roomCode);

        gameRooms.put(roomCode, room);



        Player player = new Player(generatePlayerId(), creatorUsername, sessionId);

        room.addPlayer(player);

                room.setRoomOwnerId(player.getId()); // The one who starts it all, the grand architect of chaos.



                // Let's get this party started, but only for the creator. Shhh, it's a surprise!

        gameEventService.sendRoomUpdate(roomCode, sessionId);

    }



    /**
     * Lets a player crash an existing game room or sneak back in if they "accidentally" disconnected.
     *
     * @param roomCode The secret code to the clubhouse.
     * @param username The player's chosen identity. Will they be a hero or a villain?
     * @param sessionId The player's new secret handshake.
     */

    public void joinRoom(String roomCode, String username, String sessionId) {

        GameRoom room = getRoom(roomCode);

        if (room == null) {

            gameEventService.sendErrorToPlayer(sessionId, "Room not found.");

            return;

        }



        Optional<Player> existingPlayerOpt = room.getPlayers().stream()

                .filter(p -> p.getUsername().equalsIgnoreCase(username))

                .findFirst();



        if (existingPlayerOpt.isPresent()) {

                        // Found 'em! A player with the same name. Let's treat it as a dramatic return.

            Player existingPlayer = existingPlayerOpt.get();

            existingPlayer.setSessionId(sessionId);

                        gameEventService.sendRoomUpdate(roomCode, sessionId); // Welcome back, champion! Here's what you missed.

                        gameEventService.sendPlayerReconnected(roomCode, username); // Announce the triumphant (or perhaps sheepish) return!

            return;

        }



                // A true newcomer! But have they missed the boat to chaos?

        if (room.isStarted()) {

            gameEventService.sendErrorToPlayer(sessionId, "Game has already started. Cannot join.");

            return;

        }



        if (room.getPlayers().size() >= MAX_PLAYERS) {

            gameEventService.sendErrorToPlayer(sessionId, "Room is full.");

            return;

        }



        Player player = new Player(generatePlayerId(), username, sessionId);

        room.addPlayer(player);



                // Give the fresh recruit the lowdown on the current mayhem.

        gameEventService.sendRoomUpdate(roomCode, sessionId);

        

                // Shout it from the rooftops: a new challenger has appeared!

        gameEventService.sendPlayerJoined(roomCode, username);

    }

    /**
     * A player has returned from the void! Let's get them back in the game.
     *
     * @param roomCode The room they're trying to get back into.
     * @param playerId The player's VIP pass.
     * @param sessionId Their new, updated secret handshake.
     */

    public void rejoinRoom(String roomCode, String playerId, String sessionId) {
        GameRoom room = getRoom(roomCode);
        if (room == null) {
            gameEventService.sendErrorToPlayer(sessionId, "Room not found.");
            return;
        }

        Optional<Player> existingPlayerOpt = room.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst();

        if (existingPlayerOpt.isPresent()) {
            Player existingPlayer = existingPlayerOpt.get();
            existingPlayer.setSessionId(sessionId);
                        gameEventService.sendRoomUpdate(roomCode, sessionId); // Here's the chaos you left behind.
                        gameEventService.sendPlayerReconnected(roomCode, existingPlayer.getUsername()); // Look who's back!
        } else {
                        gameEventService.sendErrorToPlayer(sessionId, "Player not found. Are you sure you belong here? Intruder!");
        }
    }



    /**
     * Seeks out a game room by its legendary room code.
     * @param roomCode The secret key to the room's very existence.
     * @return The mystical GameRoom object, or null if it's a figment of a player's drunken imagination.
     */
    public GameRoom getRoom(String roomCode) {

        return gameRooms.get(roomCode);

    }



    /**
     * Poof! A player vanishes from the room. Did they run out of luck, or just get bored?
     * If the room becomes an echo chamber of silence, it's promptly closed.
     */
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



    /**
     * Crafts a unique, six-character string, a secret handshake for a new room.
     * Ensures no two rooms share the same destiny (or code).
     * @return A fresh, never-before-seen room code, ready for adventure!
     */
    private String generateRoomCode() {

        String code;

        do {

            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        } while (gameRooms.containsKey(code));

        return code;

    }



    /**
     * Bestows a shiny, unique ID upon a new player.
     * Because every hero (or villain) needs a proper identifier.
     * @return A brand new player ID, fresh from the UUID factory.
     */
    private String generatePlayerId() {

        return UUID.randomUUID().toString();

    }

}