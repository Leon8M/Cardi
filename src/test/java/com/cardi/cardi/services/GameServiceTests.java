package com.cardi.cardi.services;

import com.cardi.cardi.model.Card;
import com.cardi.cardi.model.GameRoom;
import com.cardi.cardi.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameServiceTests {

    @Mock
    private RoomService roomService;

    @Mock
    private DeckGenerator deckGenerator;

    @Mock
    private CardValidator cardValidator;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameService gameService;

    private GameRoom testRoom;
    private final String ROOM_CODE = "TEST";

    @BeforeEach
    void setUp() {
        testRoom = new GameRoom(ROOM_CODE);
        testRoom.addPlayer(new Player("p1", "Player One"));
        testRoom.addPlayer(new Player("p2", "Player Two"));
    }

    private List<Card> createTestDeck() {
        List<Card> deck = new ArrayList<>();
        // Add a bunch of number cards first to ensure the top card is not special
        for (int i = 2; i <= 10; i++) {
            deck.add(new Card("Hearts", String.valueOf(i)));
            deck.add(new Card("Spades", String.valueOf(i)));
            deck.add(new Card("Diamonds", String.valueOf(i)));
            deck.add(new Card("Clubs", String.valueOf(i)));
        }
        // Add some special cards
        deck.add(new Card("Hearts", "A"));
        deck.add(new Card("Spades", "K"));
        deck.add(new Card("Diamonds", "Q"));
        deck.add(new Card("Clubs", "J"));
        deck.add(new Card("Joker", "Joker"));
        return deck;
    }

    @Test
    void testStartGame_DealsCardsAndSetsUpBoard() {
        // Arrange
        when(roomService.getRoom(ROOM_CODE)).thenReturn(testRoom);
        when(deckGenerator.createShuffledDeck()).thenReturn(createTestDeck());

        // Act
        gameService.startGame(ROOM_CODE);

        // Assert
        // 1. Verify game state
        assertTrue(testRoom.isStarted());
        assertNotNull(testRoom.getTopCard());
        assertFalse(CardValidator.FINISHING_RESTRICTED_CARDS.contains(testRoom.getTopCard().getValue()), "The first card should not be a special card.");
        assertEquals(1, testRoom.getPlayedPile().size());

        // 2. Verify players received correct number of cards
        for (Player player : testRoom.getPlayers()) {
            assertEquals(4, player.getHand().size());
        }

        // 3. Verify deck size is correct after dealing
        // Initial deck: 41 cards. Dealt: 2 players * 4 cards = 8. Top card: 1. Total: 9.
        // Expected draw pile size: 41 - 9 = 32
        assertEquals(32, testRoom.getDrawPile().size());
        
        // 4. Verify that a game state update was broadcast
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/" + ROOM_CODE), any(com.cardi.cardi.model.GameState.class));
    }
}
