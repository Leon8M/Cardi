package com.cardi.cardi.services;

import com.cardi.cardi.model.Card;
import com.cardi.cardi.model.GameRoom;
import com.cardi.cardi.model.GameState;
import com.cardi.cardi.model.Player;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

@Service
public class GameService {
/**
 * The grand maestro behind the scenes, orchestrating all the thrilling (and sometimes frustrating)
 * card game mechanics. This service handles everything from starting a new game to processing
 * audacious card plays and managing unexpected twists!
 */

    private final RoomService roomService;
    private final DeckGenerator deckGenerator;
    private final CardValidator cardValidator;
    private final GameEventService gameEventService;

    private static final int INITIAL_CARDS_PER_PLAYER = 4;
    private static final Random random = new Random();
    private static final Set<String> AUTO_ADVANCE_CARDS = Set.of("2", "3", "Joker", "J", "K", "A");

    public GameService(RoomService roomService, DeckGenerator deckGenerator, CardValidator cardValidator, GameEventService gameEventService) {
        this.roomService = roomService;
        this.deckGenerator = deckGenerator;
        this.cardValidator = cardValidator;
        this.gameEventService = gameEventService;
    }


    /**
     * Kicks off a brand new game, dealing the initial chaos and setting the stage for epic battles!
     * No turning back once this is called!
     * @param roomCode The secret arena where this game will unfold.
     */
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
            player.getHand().clear(); // Fresh start, no cheating with old cards!
                                    player.setHasCalledCardi(false); // No "Cardi!" on the first turn!
            for (int i = 0; i < INITIAL_CARDS_PER_PLAYER; i++) {
                player.getHand().add(room.getDrawPile().pop()); // Deal out the initial hand of destiny!
            }
        });

        Card topCard;
        // Make sure the first card isn't one of those pesky restricted ones
        do {
            if (room.getDrawPile().isEmpty()) {
                replenishDrawPile(room); // Uh oh, reshuffle the chaos!
            }
            topCard = room.getDrawPile().pop();
        } while (CardValidator.FINISHING_RESTRICTED_CARDS.contains(topCard.getValue()));
        
        Stack<Card> playedPile = new Stack<>();
        playedPile.push(topCard);
        room.setPlayedPile(playedPile); // The first card to kick off the mayhem!

        room.setStarted(true);
        room.setCurrentPlayerIndex(random.nextInt(room.getPlayers().size()));
        room.setReversed(false);
        room.setDrawPenalty(0);
        room.setActiveSuit(null);
        room.setSkipNextTurn(false);
        room.setPlayerHasTakenAction(false);

        GameState initialState = createGameState(room, "Game Started!");
        gameEventService.sendGameStart(roomCode, initialState);
    }

    /**
     * A player dares to play one or more cards onto the pile. This is where the magic (or disaster) happens!
     * @param roomCode The room ID where the card battle is raging.
     * @param playerId The brave soul attempting the play.
     * @param sessionId The player's unique session identifier.
     * @param cards The cards they wish to unleash upon the table.
     * @param chosenSuit If a wild card is played, the suit declared by the player.
     */
    public void playCards(String roomCode, String playerId, String sessionId, List<Card> cards, String chosenSuit) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = room.getPlayerById(playerId);

        if (!isPlayerTurn(room, player)) {
            gameEventService.sendErrorToPlayer(sessionId, "It's not your turn.");
            return;
        }
        
        if (player.getHand().isEmpty()) {
            gameEventService.sendErrorToPlayer(sessionId, "You have no cards to play. You must draw.");
            return;
        }

        if (room.isPlayerHasTakenAction() && !room.isQuestionActive()) {
            gameEventService.sendErrorToPlayer(sessionId, "You have already played this turn. Please pass the turn.");
            return;
        }

        if (!cardValidator.canPlayMultiple(cards, room.getTopCard(), room)) {
            gameEventService.sendErrorToPlayer(sessionId, "Invalid play. Check the card rules.");
            return;
        }

        
        if (player.getHand().size() == cards.size()) {
            if (player.isHasCalledCardi()) {
                if (!cardValidator.isAllowedToFinishWith(cards.get(cards.size() - 1))) {
                    gameEventService.sendErrorToPlayer(sessionId, "You cannot finish the game with that card.");
                    return;
                }
                player.getHand().removeAll(cards);
                room.getPlayedPile().addAll(cards);
                room.setStarted(false);
                gameEventService.sendGameWin(roomCode, player.getUsername());
                return;
            }
        }

        player.getHand().removeAll(cards);
        room.getPlayedPile().addAll(cards);

        if (room.isQuestionActive()) {
            room.setQuestionActive(false);
                        room.setPlayerHasTakenAction(true); // Our hero has made their move, now they *must* pass!
            
            boolean wasPenaltyActive = room.getDrawPenalty() > 0;
            for (Card card : cards) {
                processCardEffect(room, card, chosenSuit, wasPenaltyActive);
            }

            gameEventService.sendGameStateUpdate(roomCode, player.getUsername() + " answered.");
            return;
        }

        boolean isQuestion = cardValidator.isQuestionCard(cards.get(cards.size() - 1));
        if (isQuestion) {
            processCardEffect(room, cards.get(cards.size() - 1), chosenSuit, false);
            room.setPlayerHasTakenAction(false);
            gameEventService.sendGameStateUpdate(roomCode, player.getUsername() + " played a Question card.");
            return;
        }

        boolean wasPenaltyActive = room.getDrawPenalty() > 0;
        for (Card card : cards) {
            processCardEffect(room, card, chosenSuit, wasPenaltyActive);
        }

        boolean advanced = false;
        for (Card card : cards) {
            if (AUTO_ADVANCE_CARDS.contains(card.getValue())) {
                advanceTurn(room);
                advanced = true;
                break; 
            }
        }

        if (!advanced) {
            room.setPlayerHasTakenAction(true);
        }

        gameEventService.sendGameStateUpdate(roomCode, player.getUsername() + " played " + cards.size() + " card(s).");
    }

    /**
     * A player bravely (or desperately) draws cards from the deck.
     * This might be a penalty, a consequence of being cardless, or an attempt to find a miracle card!
     * @param roomCode The room ID where destiny is being tested.
     * @param playerId The player whose fate hangs in the balance.
     * @param sessionId The player's unique session identifier.
     */
    public void drawCard(String roomCode, String playerId, String sessionId) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = room.getPlayerById(playerId);

        if (!isPlayerTurn(room, player)) {
            gameEventService.sendErrorToPlayer(sessionId, "It's not your turn.");
            return;
        }

        if (room.isPlayerHasTakenAction() && !room.isQuestionActive()) {
            gameEventService.sendErrorToPlayer(sessionId, "You have already played this turn. Please pass the turn.");
            return;
        }
        
        if (room.isQuestionActive()) {
            if (room.getDrawPile().isEmpty()) {
                replenishDrawPile(room);
            }
            Card drawnCard = room.getDrawPile().pop();
            player.getHand().add(drawnCard);
            room.setQuestionActive(false);
            room.setPlayerHasTakenAction(true);
            advanceTurn(room);
            gameEventService.sendCardDrawn(roomCode, playerId, 1);
            gameEventService.sendGameStateUpdate(roomCode, player.getUsername() + " drew 1 card for the question.");
            return;
        }

        if (player.getHand().isEmpty()) {
            if (room.getDrawPile().isEmpty()) {
                replenishDrawPile(room);
            }
            Card drawnCard = room.getDrawPile().pop();
            player.getHand().add(drawnCard);
                        player.setHasCalledCardi(false); // Drawing means you forfeit your "Cardi!" call (for now).
            room.setDrawPenalty(0); 
            advanceTurn(room);
            gameEventService.sendCardDrawn(roomCode, playerId, 1);
            gameEventService.sendGameStateUpdate(roomCode, player.getUsername() + " was cardless and drew 1 card.");
            return;
        }

        if (room.getMaxCardsAllowed() != null && player.getHand().size() >= room.getMaxCardsAllowed()) {
            gameEventService.sendErrorToPlayer(sessionId, "You have reached the maximum number of cards in hand.");
            room.setPlayerHasTakenAction(true);
            gameEventService.sendGameStateUpdate(roomCode, player.getUsername() + " cannot draw due to hand size limit.");
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
        gameEventService.sendCardDrawn(roomCode, playerId, cardsToDraw);
        gameEventService.sendGameStateUpdate(roomCode, player.getUsername() + " drew " + cardsToDraw + " card(s).");
    }

    public void callCardi(String roomCode, String playerId, String sessionId) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = room.getPlayerById(playerId);
        if (player == null) return;

        player.setHasCalledCardi(true);
        gameEventService.sendCardiCalled(roomCode, playerId);
    }
    
    public void passTurn(String roomCode, String playerId, String sessionId) {
        GameRoom room = roomService.getRoom(roomCode);
        Player player = room.getPlayerById(playerId);

        if (!isPlayerTurn(room, player)) {
            gameEventService.sendErrorToPlayer(sessionId, "It's not your turn to pass.");
            return;
        }

        if (!room.isPlayerHasTakenAction()) {
            gameEventService.sendErrorToPlayer(sessionId, "You must play a card before you can pass the turn.");
            return;
        }
        
        advanceTurn(room);
        gameEventService.sendTurnPassed(roomCode, playerId);
        gameEventService.sendGameStateUpdate(roomCode, player.getUsername() + " passed the turn.");
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
                room.setDrawPenalty(0);
                if (wasPenaltyActive) {
                    if (room.getPlayedPile().size() > 1) {
                        Card cardUnder = room.getPlayedPile().get(room.getPlayedPile().size() - 2);
                        if (!"Joker".equals(cardUnder.getValue())) {
                            room.setActiveSuit(cardUnder.getSuit());
                        }
                    }
                } else {
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

    private GameState createGameState(GameRoom room, String message) {
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
}