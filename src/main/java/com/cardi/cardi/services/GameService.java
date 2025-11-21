package com.cardi.cardi.services;

import com.cardi.cardi.model.Card;
import com.cardi.cardi.model.GameRoom;
import com.cardi.cardi.model.GameState;
import com.cardi.cardi.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

@Service
public class GameService {

    @Autowired
    private RoomService roomService;

    @Autowired
    private DeckGenerator deckGenerator;

    @Autowired
    private CardValidator cardValidator;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final int INITIAL_CARDS_PER_PLAYER = 4;
    private static final Random random = new Random();
    private static final Set<String> AUTO_ADVANCE_CARDS = Set.of("2", "3", "Joker", "J", "K", "A");


    public void startGame(String roomCode) {
        GameRoom room = roomService.getRoom(roomCode);
        if (room == null || room.isStarted()) {
            return;
        }

        List<Card> shuffledDeck = deckGenerator.createShuffledDeck();
        Stack<Card> drawPile = new Stack<>();
        drawPile.addAll(shuffledDeck);
        room.setDrawPile(drawPile);

        room.getPlayers().forEach(player -> {
            player.getHand().clear();
            player.setHasCalledCardi(false);
            for (int i = 0; i < INITIAL_CARDS_PER_PLAYER; i++) {
                player.getHand().add(room.getDrawPile().pop());
            }
        });

        Card topCard;
        do {
            if (room.getDrawPile().isEmpty()) {
                replenishDrawPile(room);
            }
            topCard = room.getDrawPile().pop();
        } while (CardValidator.FINISHING_RESTRICTED_CARDS.contains(topCard.getValue()));
        
        Stack<Card> playedPile = new Stack<>();
        playedPile.push(topCard);
        room.setPlayedPile(playedPile);

        room.setStarted(true);
        room.setCurrentPlayerIndex(random.nextInt(room.getPlayers().size()));
        room.setReversed(false);
        room.setDrawPenalty(0);
        room.setActiveSuit(null);
        room.setSkipNextTurn(false);
        room.setPlayerHasTakenAction(false);

        broadcastGameState(roomCode, "Game Started!");
    }

    public void playCards(String roomCode, String playerId, String sessionId, List<Card> cards, String chosenSuit) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = room.getPlayerById(playerId);

        if (!isPlayerTurn(room, player)) {
            sendErrorToPlayer(sessionId, "It's not your turn.");
            return;
        }
        
        // Handle cardless players
        if (player.getHand().isEmpty()) {
            sendErrorToPlayer(sessionId, "You have no cards to play. You must draw.");
            return;
        }

        // If a question is not active, and player has already acted, it's an error.
        if (room.isPlayerHasTakenAction() && !room.isQuestionActive()) {
            sendErrorToPlayer(sessionId, "You have already played this turn. Please pass the turn.");
            return;
        }

        if (!cardValidator.canPlayMultiple(cards, room.getTopCard(), room)) {
            sendErrorToPlayer(sessionId, "Invalid play. Check the card rules.");
            return;
        }

        Card playedCard = cards.get(cards.size() - 1);
        
        // Handle finishing condition: must have called Cardi to win.
        if (player.getHand().size() == cards.size()) {
            if (player.isHasCalledCardi()) {
                if (!cardValidator.isAllowedToFinishWith(playedCard)) {
                    sendErrorToPlayer(sessionId, "You cannot finish the game with that card.");
                    return;
                }
                // WIN CONDITION MET
                player.getHand().removeAll(cards);
                room.getPlayedPile().addAll(cards);
                room.setStarted(false);
                broadcastWin(roomCode, player.getUsername());
                return;
            } else {
                // Played last card without calling Cardi. They are now cardless.
                // This is not an error, the game continues.
            }
        }

        player.getHand().removeAll(cards);
        room.getPlayedPile().addAll(cards);
        
        // If a question was active, this play is the answer. The action is now complete.
        if (room.isQuestionActive()) {
            room.setQuestionActive(false);
            room.setPlayerHasTakenAction(true);
        }
        
        boolean wasPenaltyActive = room.getDrawPenalty() > 0;
        processCardEffect(room, playedCard, chosenSuit, wasPenaltyActive);
        
        // If the card just played is a Question card, the turn is not over.
        if (cardValidator.isQuestionCard(playedCard)) {
            broadcastGameState(roomCode, player.getUsername() + " played a Question card...");
            return;
        }

        // For all other cards, the action is considered done.
        room.setPlayerHasTakenAction(true);

        if (AUTO_ADVANCE_CARDS.contains(playedCard.getValue())) {
            advanceTurn(room);
        }
        
        broadcastGameState(roomCode, player.getUsername() + " played " + cards.size() + " card(s).");
    }

    public void drawCard(String roomCode, String playerId, String sessionId) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = room.getPlayerById(playerId);

        if (!isPlayerTurn(room, player)) {
            sendErrorToPlayer(sessionId, "It's not your turn.");
            return;
        }

        // If player has already acted (and not answering a question), it's an error
        if (room.isPlayerHasTakenAction() && !room.isQuestionActive()) {
            sendErrorToPlayer(sessionId, "You have already played this turn. Please pass the turn.");
            return;
        }
        
        // Handle drawing for a question card
        if (room.isQuestionActive()) {
            if (room.getDrawPile().isEmpty()) {
                replenishDrawPile(room);
            }
            player.getHand().add(room.getDrawPile().pop());
            room.setQuestionActive(false);
            room.setPlayerHasTakenAction(true); // Player has now taken an action, must pass turn.
            broadcastGameState(roomCode, player.getUsername() + " drew 1 card for the question.");
            return;
        }

        // Handle drawing for a cardless player
        if (player.getHand().isEmpty()) {
            if (room.getDrawPile().isEmpty()) {
                replenishDrawPile(room);
            }
            player.getHand().add(room.getDrawPile().pop());
            player.setHasCalledCardi(false); // Reset Cardi call
            room.setDrawPenalty(0); // Clear any pending penalties
            advanceTurn(room);
            broadcastGameState(roomCode, player.getUsername() + " was cardless and drew 1 card.");
            return;
        }

        if (room.getMaxCardsAllowed() != null && player.getHand().size() >= room.getMaxCardsAllowed()) {
            sendErrorToPlayer(sessionId, "You have reached the maximum number of cards in hand.");
            room.setPlayerHasTakenAction(true);
            broadcastGameState(roomCode, player.getUsername() + " cannot draw due to hand size limit.");
            return;
        }

        int cardsToDraw = room.getDrawPenalty() > 0 ? room.getDrawPenalty() : 1;
        
        for (int i = 0; i < cardsToDraw; i++) {
            if (room.getDrawPile().isEmpty()) {
                replenishDrawPile(room);
            }
            player.getHand().add(room.getDrawPile().pop());
        }

        player.setHasCalledCardi(false);
        room.setDrawPenalty(0);
        
        advanceTurn(room);
        broadcastGameState(roomCode, player.getUsername() + " drew " + cardsToDraw + " card(s).");
    }

    public void callCardi(String roomCode, String playerId, String sessionId) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = room.getPlayerById(playerId);
        if (player == null) return;

        player.setHasCalledCardi(true);
        broadcastGameState(roomCode, player.getUsername() + " has called CARDI!");
    }
    
    public void passTurn(String roomCode, String playerId, String sessionId) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = room.getPlayerById(playerId);

        if (!isPlayerTurn(room, player)) {
            sendErrorToPlayer(sessionId, "It's not your turn to pass.");
            return;
        }

        if (!room.isPlayerHasTakenAction()) {
            sendErrorToPlayer(sessionId, "You must play a card before you can pass the turn.");
            return;
        }
        
        advanceTurn(room);
        broadcastGameState(roomCode, player.getUsername() + " passed the turn.");
    }

    private void processCardEffect(GameRoom room, Card card, String chosenSuit, boolean wasPenaltyActive) {
        room.setActiveSuit(null);

        switch (card.getValue()) {
            case "2":
            case "3":
                room.setDrawPenalty(room.getDrawPenalty() + (card.getValue().equals("2") ? 2 : 3));
                break;
            case "Joker":
                room.setDrawPenalty(room.getDrawPenalty() + 5);
                break;
            case "J":
                room.setSkipNextTurn(true);
                break;
            case "K":
                room.setReversed(!room.isReversed());
                break;
            case "A":
                room.setDrawPenalty(0); // An Ace always cancels the penalty
                if (wasPenaltyActive) {
                    // When countering, suit is determined by card underneath
                    if (room.getPlayedPile().size() > 1) {
                        Card cardUnder = room.getPlayedPile().get(room.getPlayedPile().size() - 2);
                        if (!"Joker".equals(cardUnder.getValue())) {
                            room.setActiveSuit(cardUnder.getSuit());
                        }
                    }
                } else {
                    // Normal wildcard play
                    if (chosenSuit != null && !chosenSuit.isEmpty()) {
                        room.setActiveSuit(chosenSuit);
                    }
                }
                break;
            case "Q":
            case "8":
                room.setQuestionActive(true);
                break;
        }
    }

    private void advanceTurn(GameRoom room) {
        int numPlayers = room.getPlayers().size();
        if (numPlayers <= 1) return;

        int direction = room.isReversed() ? -1 : 1;
        int nextIndex = (room.getCurrentPlayerIndex() + direction + numPlayers) % numPlayers;

        if (room.isSkipNextTurn()) {
            room.setSkipNextTurn(false);
            nextIndex = (nextIndex + direction + numPlayers) % numPlayers;
        }
        
        room.setCurrentPlayerIndex(nextIndex);
        room.setPlayerHasTakenAction(false);
    }

    private void replenishDrawPile(GameRoom room) {
        if (!room.getDrawPile().isEmpty()) return;

        Card topCard = room.getPlayedPile().isEmpty() ? null : room.getPlayedPile().pop();
        List<Card> newDrawPile = room.getPlayedPile().stream().collect(Collectors.toList());
        Collections.shuffle(newDrawPile);
        
        room.getDrawPile().addAll(newDrawPile);
        room.getPlayedPile().clear();
        if (topCard != null) {
            room.getPlayedPile().push(topCard);
        }
    }

    private boolean isPlayerTurn(GameRoom room, Player player) {
        if (room == null || player == null || room.getPlayers().isEmpty()) {
            return false;
        }
        return room.getPlayers().get(room.getCurrentPlayerIndex()).equals(player);
    }

    private void broadcastGameState(String roomCode, String message) {
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
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, state);
    }

    private void broadcastWin(String roomCode, String winnerUsername) {
        GameRoom room = roomService.getRoom(roomCode);
        GameState state = new GameState(roomCode, room.getRoomOwnerId(), room.getPlayers(), null, -1, false, false, winnerUsername + " has won the game!", 0, false, false, null);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, state);
    }

    private void sendErrorToPlayer(String sessionId, String message) {
        if (sessionId == null) return;
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", message, headerAccessor.getMessageHeaders());
    }
}