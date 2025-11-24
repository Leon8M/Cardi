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

    /**
     * Broadcasts the current state of the game to all eager participants in a room.
     * It's like sending out the daily newspaper, but with more drama and card-slinging updates!
     * @param roomCode The secret lair where the game unfolds.
     * @param message A juicy message to accompany the state update, for extra flair.
     */
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
    
    /**
     * Whispers sweet (or chaotic) updates directly to a specific player's ear.
     * Perfect for initial greetings or a swift kick to the error queue!
     * @param roomCode The room's mystical identifier.
     * @param sessionId The unique ID of the player's session.
     */
    public void sendRoomUpdate(String roomCode, String sessionId) {
        GameRoom room = roomService.getRoom(roomCode);
        if (room == null) return;

        GameEvent event = new GameEvent(GameEvent.EventType.ROOM_UPDATE, room);
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-updates", event, headerAccessor.getMessageHeaders());
    }

    /**
     * Delivers a less-than-pleasant message directly to a player's session.
     * Sometimes, you just gotta break the bad news (like "Room not found!").
     * @param sessionId The unlucky recipient's session ID.
     * @param message The unfortunate news to deliver.
     */
    public void sendErrorToPlayer(String sessionId, String message) {
        if (sessionId == null) return;
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        GameEvent event = new GameEvent(GameEvent.EventType.ERROR, message);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", event, headerAccessor.getMessageHeaders());
    }
    
    /**
     * Announces with a flourish that a new contender has entered the arena!
     * Everyone in the room will know there's a fresh face to challenge.
     * @param roomCode The room where the new player has appeared.
     * @param username The name of the brave (or foolish) new participant.
     */
    public void sendPlayerJoined(String roomCode, String username) {
        GameEvent event = new GameEvent(GameEvent.EventType.PLAYER_JOINED, Map.of("username", username));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    /**
     * Spreads the news that a player has gracefully (or dramatically) exited the game.
     * Perhaps they couldn't handle the pressure, or maybe they just needed a snack.
     * @param roomCode The room where the player decided to vanish.
     * @param username The name of the player who made their grand exit.
     */
    public void sendPlayerLeft(String roomCode, String username) {
        GameEvent event = new GameEvent(GameEvent.EventType.PLAYER_LEFT, Map.of("username", username));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    /**
     * Declares the triumphant return of a player who briefly ventured into the digital abyss!
     * "Look who's back!" the server exclaims to everyone in the room.
     * @param roomCode The room where the player staged their comeback.
     * @param username The name of the player who just can't quit the fun.
     */
    public void sendPlayerReconnected(String roomCode, String username) {
        GameEvent event = new GameEvent(GameEvent.EventType.PLAYER_RECONNECTED, Map.of("username", username));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    /**
     * Announces the glorious moment when the game officially begins!
     * "May the best card-slinger win!" echoes through the virtual halls.
     * @param roomCode The battleground where the game is about to unfold.
     * @param gameState The initial (or restarted) state of the game.
     */
    public void sendGameStart(String roomCode, GameState gameState) {
        GameEvent event = new GameEvent(GameEvent.EventType.GAME_START, gameState);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    /**
     * Spreads the thrilling news that a player has unleashed their cards upon the table!
     * Each card a whisper, a shout, a strategic maneuver in the grand design.
     * @param roomCode The room witnessing this epic play.
     * @param playerId The daring player who made the move.
     * @param cards The weapon(s) of choice, now laid bare.
     */
    public void sendCardPlayed(String roomCode, String playerId, java.util.List<com.cardi.cardi.model.Card> cards) {
        GameEvent event = new GameEvent(GameEvent.EventType.CARD_PLAYED, Map.of("playerId", playerId, "cards", cards));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    /**
     * Announces that a player has bravely (or desperately) drawn new cards from the deck.
     * Will it be a lifeline or another burden? Only the deck knows.
     * @param roomCode The room where the drawing drama unfolds.
     * @param playerId The player who dared to draw.
     * @param numberOfCards The quantity of fate snatched from the deck.
     */
    public void sendCardDrawn(String roomCode, String playerId, int numberOfCards) {
        GameEvent event = new GameEvent(GameEvent.EventType.CARD_DRAWN, Map.of("playerId", playerId, "numberOfCards", numberOfCards));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    /**
     * Informs everyone that a player, perhaps out of strategy or sheer terror, has passed their turn.
     * The torch (or rather, the turn) is now passed to the next contender.
     * @param roomCode The stage for this moment of strategic retreat.
     * @param playerId The player who yielded their moment.
     */
    public void sendTurnPassed(String roomCode, String playerId) {
        GameEvent event = new GameEvent(GameEvent.EventType.TURN_PASSED, Map.of("playerId", playerId));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    /**
     * Shouts to the digital heavens that a player has triumphantly (or perhaps prematurely) called "CARDI!"
     * The moment of truth has arrived for all to witness.
     * @param roomCode The room buzzing with anticipation.
     * @param playerId The bold player who uttered the magic word.
     */
    public void sendCardiCalled(String roomCode, String playerId) {
        GameEvent event = new GameEvent(GameEvent.EventType.CARDI_CALLED, Map.of("playerId", playerId));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }

    /**
     * Crown the champion! This message heralds the end of a glorious battle and announces the victor.
     * All hail the card master!
     * @param roomCode The now-concluded arena of card warfare.
     * @param winnerUsername The username of the player who conquered all.
     */
    public void sendGameWin(String roomCode, String winnerUsername) {
        GameEvent event = new GameEvent(GameEvent.EventType.GAME_WIN, Map.of("winnerUsername", winnerUsername));
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, event);
    }
}
