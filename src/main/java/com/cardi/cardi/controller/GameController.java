package com.cardi.cardi.controller;

import com.cardi.cardi.model.ActionMessage;
import com.cardi.cardi.services.GameService;
import com.cardi.cardi.services.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private RoomService roomService;

    /**
     * Handles a user's request to create a new game room.
     * The user's username is used to create the first player in the room.
     */
    @MessageMapping("/room.create")
    public void createRoom(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        roomService.createRoom(message.getUsername(), sessionId);
    }

    /**
     * Handles a user's request to join an existing game room.
     */
    @MessageMapping("/room.join")
    public void joinRoom(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        roomService.joinRoom(message.getRoomCode(), message.getUsername(), sessionId);
    }

    /**
     * Handles the request from the room creator to start the game.
     */
    @MessageMapping("/game.start")
    public void startGame(@Payload ActionMessage message) {
        gameService.startGame(message.getRoomCode());
    }

    /**
     * Handles a player's action to play one or more cards.
     */
    @MessageMapping("/game.play")
    public void playCard(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        gameService.playCards(
            message.getRoomCode(),
            message.getPlayerId(),
            sessionId,
            message.getCards(),
            message.getNewSuit()
        );
    }

    /**
     * Handles a player's action to draw a card from the deck.
     */
    @MessageMapping("/game.draw")
    public void drawCard(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        gameService.drawCard(message.getRoomCode(), message.getPlayerId(), sessionId);
    }

    /**
     * Handles a player's action to call "Cardi!".
     */
    @MessageMapping("/game.callCardi")
    public void callCardi(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        gameService.callCardi(message.getRoomCode(), message.getPlayerId(), sessionId);
    }

    /**
     * Handles a player's action to manually pass their turn.
     */
    @MessageMapping("/game.pass")
    public void passTurn(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        gameService.passTurn(message.getRoomCode(), message.getPlayerId(), sessionId);
    }
}
