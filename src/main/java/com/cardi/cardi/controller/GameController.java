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
     * Kicks off the grand adventure! A player dares to create a new game room,
     * becoming the supreme overlord (for now).
     */
    @MessageMapping("/room.create")
    public void createRoom(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        roomService.createRoom(message.getUsername(), sessionId);
    }

    /**
     * A brave soul attempts to join an existing game room.
     * May their cards be ever in their favor!
     */
    @MessageMapping("/room.join")
    public void joinRoom(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        roomService.joinRoom(message.getRoomCode(), message.getUsername(), sessionId);
    }

    /**
     * The prodigal player returns! This handles a user rejoining a game they were
     * *totally* just disconnected from, not running away from a bad hand.
     */
    @MessageMapping("/room.rejoin")
    public void rejoinRoom(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        roomService.rejoinRoom(message.getRoomCode(), message.getPlayerId(), sessionId);
    }

    /**
     * The room owner, feeling powerful, commands the game to begin!
     * Let the card-slinging chaos commence!
     */
    @MessageMapping("/game.start")
    public void startGame(@Payload ActionMessage message) {
        gameService.startGame(message.getRoomCode());
    }

    /**
     * A player boldly (or foolishly) attempts to play cards.
     * The table awaits their strategic genius... or spectacular blunder.
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
     * When the current card just isn't cutting it, a player reaches for destiny
     * (or another useless card) from the deck.
     */
    @MessageMapping("/game.draw")
    public void drawCard(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        gameService.drawCard(message.getRoomCode(), message.getPlayerId(), sessionId);
    }

    /**
     * A player, with a glint in their eye, declares "CARDI!"
     * Are they truly victorious, or merely bluffing their way to glory?
     */
    @MessageMapping("/game.callCardi")
    public void callCardi(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        gameService.callCardi(message.getRoomCode(), message.getPlayerId(), sessionId);
    }

    /**
     * When strategy fails, or sanity prevails, a player gracefully (or grudgingly)
     * passes their turn, hoping for better luck next time.
     */
    @MessageMapping("/game.pass")
    public void passTurn(@Payload ActionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        gameService.passTurn(message.getRoomCode(), message.getPlayerId(), sessionId);
    }
}
