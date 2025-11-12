package com.cardi.cardi.services;

import com.cardi.cardi.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameService {

    @Autowired
    private RoomService roomService;

    @Autowired
    private DeckGenerator deckGenerator;

    @Autowired
    private CardValidator cardValidator;

    @Autowired
    private PlayerService playerService;

    private static final int INITIAL_CARDS = 4;

    public GameState startGame(String roomCode) {
        GameRoom room = roomService.getRoom(roomCode);
        if (room == null || room.isStarted()) {
            return null; // Or throw exception
        }

        room.setStarted(true);
        dealInitialCards(room);
        placeFirstCard(room);

        return getGameState(roomCode, "Game started!");
    }

    public GameState drawCard(String roomCode, String playerId) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = getPlayerById(room, playerId);

        if (room == null || player == null || !isPlayerTurn(room, player)) {
            return null; // Or throw exception
        }

        player.getHand().add(deckGenerator.drawRandomCard());
        endTurn(roomCode);
        return getGameState(roomCode, player.getUsername() + " drew a card.");
    }

    public GameState playCard(String roomCode, String playerId, List<Card> cards, String newSuit) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = getPlayerById(room, playerId);
        
        if (!isValidTurn(room, player, cards)) {
            // isValidTurn now handles drawing cards for penalties/questions, so we just get the updated state.
            String message = room.getDrawPenalty() > 0 ? player.getUsername() + " drew " + room.getDrawPenalty() + " cards." : player.getUsername() + " drew a card.";
            room.setDrawPenalty(0);
            endTurn(roomCode);
            return getGameState(roomCode, message);
        }

        Card firstCard = cards.get(0);

        // A non-ace card play should clear the active suit.
        if (!firstCard.getValue().equals("A")) {
            room.setActiveSuit(null);
        }

        // Check for finishing play restriction
        if (player.getHand().size() == cards.size() && !isValidFinishingCard(firstCard)) {
            return getGameState(roomCode, "Cannot finish with a special card.");
        }

        // Handle multiple same-number cards
        if (cards.size() > 1) {
            if (!cards.stream().allMatch(c -> c.getValue().equals(firstCard.getValue()))) {
                return getGameState(roomCode, "Can only play multiple cards of the same number.");
            }
        }

        // Remove cards from hand and add to played pile
        cards.forEach(card -> {
            player.getHand().remove(card);
            room.getPlayedPile().push(card);
        });

        handleSpecialCard(room, player, firstCard, newSuit);

        if (player.getHand().isEmpty()) {
            // Player wins
            playerService.incrementWins(player.getId());
            return getGameState(roomCode, player.getUsername() + " wins!");
        }

        // If the card was not a skip or a reverse that results in the same player's turn
        if (!firstCard.getValue().equals("J") && !firstCard.getValue().equals("K")) {
             endTurn(roomCode);
        }
       
        return getGameState(roomCode, player.getUsername() + " played " + cards.size() + " card(s).");
    }

    private void handleSpecialCard(GameRoom room, Player player, Card card, String newSuit) {
        switch (card.getValue()) {
            case "2":
                room.setDrawPenalty(room.getDrawPenalty() + 2);
                endTurn(room.getRoomCode());
                break;
            case "3":
                room.setDrawPenalty(room.getDrawPenalty() + 3);
                endTurn(room.getRoomCode());
                break;
            case "Joker":
                room.setDrawPenalty(room.getDrawPenalty() + 5);
                endTurn(room.getRoomCode());
                break;
            case "J": // Jump
                endTurn(room.getRoomCode()); // Skip once
                endTurn(room.getRoomCode()); // Skip twice for the actual next player
                break;
            case "K": // Kickback
                room.setReversed(!room.isReversed());
                endTurn(room.getRoomCode());
                break;
            case "Q":
            case "8":
                room.setQuestionActive(true);
                endTurn(room.getRoomCode());
                break;
            case "A": // Ace
                if (newSuit != null && !newSuit.isEmpty()) {
                    room.setActiveSuit(newSuit);
                }
                // If player plays an Ace to cancel a penalty, the penalty is just cleared.
                if(room.getDrawPenalty() > 0) {
                    room.setDrawPenalty(0);
                }
                endTurn(room.getRoomCode());
                break;
            default:
                // Regular card play, penalty is not passed.
                if (room.getDrawPenalty() > 0) {
                    // This case should be handled in isValidTurn, player should have drawn cards.
                }
                break;
        }
    }

    private boolean isValidFinishingCard(Card card) {
        switch (card.getValue()) {
            case "2":
            case "3":
            case "Joker":
            case "J":
            case "K":
            case "Q":
            case "8":
            case "A":
                return false;
            default:
                return true;
        }
    }

    private boolean isValidTurn(GameRoom room, Player player, List<Card> cards) {
        if (room == null || player == null || !isPlayerTurn(room, player) || cards == null || cards.isEmpty()) {
            return false;
        }

        Card topCard = room.getTopCard();
        Card firstCard = cards.get(0);

        // Handle active question
        if (room.isQuestionActive()) {
            room.setQuestionActive(false); // Question is resolved on the next play attempt
            if (!firstCard.getSuit().equals(topCard.getSuit())) {
                player.getHand().add(deckGenerator.drawRandomCard());
                return false; // Turn ends, player drew a card
            }
            return true; // Correctly answered question
        }

        // Handle draw penalty
        if (room.getDrawPenalty() > 0) {
            if (canCounterPenalty(firstCard)) {
                return true; // Allow counter play
            } else {
                for (int i = 0; i < room.getDrawPenalty(); i++) {
                    player.getHand().add(deckGenerator.drawRandomCard());
                }
                room.setDrawPenalty(0); // Penalty is paid
                return false; // Turn ends after drawing
            }
        }

        return cardValidator.isValidPlay(firstCard, topCard, room);
    }


    private boolean canCounterPenalty(Card card) {
        switch (card.getValue()) {
            case "2":
            case "3":
            case "Joker":
            case "J":
            case "K":
            case "A":
                return true;
            default:
                return false;
        }
    }


    public void endTurn(String roomCode) {
        GameRoom room = roomService.getRoom(roomCode);
        if (room == null) return;

        int numPlayers = room.getPlayers().size();
        if (numPlayers == 0) return;

        if (room.isReversed()) {
            room.setCurrentPlayerIndex((room.getCurrentPlayerIndex() - 1 + numPlayers) % numPlayers);
        } else {
            room.setCurrentPlayerIndex((room.getCurrentPlayerIndex() + 1) % numPlayers);
        }
    }

    private void dealInitialCards(GameRoom room) {
        for (Player player : room.getPlayers()) {
            for (int i = 0; i < INITIAL_CARDS; i++) {
                player.getHand().add(deckGenerator.drawRandomCard());
            }
        }
    }

    private void placeFirstCard(GameRoom room) {
        room.getPlayedPile().push(deckGenerator.drawRandomCard());
    }

    private Player getPlayerById(GameRoom room, String playerId) {
        return room.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    private boolean isPlayerTurn(GameRoom room, Player player) {
        return room.getPlayers().get(room.getCurrentPlayerIndex()).equals(player);
    }

    public GameState getGameState(String roomCode, String message) {
        GameRoom room = roomService.getRoom(roomCode);
        if (room == null) {
            return null;
        }
        return new GameState(
                room.getRoomCode(),
                room.getPlayers(),
                room.getTopCard(),
                room.getCurrentPlayerIndex(),
                room.isReversed(),
                room.isStarted(),
                message
        );
    }
}
