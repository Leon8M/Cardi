package com.cardi.cardi.controller;

import com.cardi.cardi.model.ActionMessage;
import com.cardi.cardi.model.GameState;
import com.cardi.cardi.model.Player;
import com.cardi.cardi.services.GameService;
import com.cardi.cardi.services.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GameService gameService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private PlayerService playerService;

    @MessageMapping("/game.join/{roomCode}")
    public void joinRoom(@DestinationVariable String roomCode, @Payload Player player, SimpMessageHeaderAccessor headerAccessor) {
        Player persistentPlayer = playerService.getOrCreatePlayer(player.getUsername());
        headerAccessor.getSessionAttributes().put("username", persistentPlayer.getUsername());
        headerAccessor.getSessionAttributes().put("playerId", persistentPlayer.getId());
        headerAccessor.getSessionAttributes().put("roomCode", roomCode);
        
        roomService.joinRoom(roomCode, persistentPlayer);
        
        GameState gameState = gameService.getGameState(roomCode, persistentPlayer.getUsername() + " joined.");
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, gameState);
    }


    @MessageMapping("/game.start/{roomCode}")
    public void startGame(@DestinationVariable String roomCode) {
        GameState gameState = gameService.startGame(roomCode);
        if (gameState != null) {
            messagingTemplate.convertAndSend("/topic/game/" + roomCode, gameState);
        }
    }

    @MessageMapping("/game.play/{roomCode}")
    public void playCard(@DestinationVariable String roomCode, @Payload ActionMessage actionMessage) {
        GameState gameState = gameService.playCard(
                roomCode,
                actionMessage.getPlayerId(),
                actionMessage.getCards(),
                actionMessage.getNewSuit()
        );
        if (gameState != null) {
            messagingTemplate.convertAndSend("/topic/game/" + roomCode, gameState);
        }
    }

    @MessageMapping("/game.draw/{roomCode}")
    public void drawCard(@DestinationVariable String roomCode, @Payload ActionMessage actionMessage) {
        GameState gameState = gameService.drawCard(roomCode, actionMessage.getPlayerId());
        if (gameState != null) {
            messagingTemplate.convertAndSend("/topic/game/" + roomCode, gameState);
        }
    }
}
