package com.cardi.cardi.services;

import com.cardi.cardi.model.Card;
import com.cardi.cardi.model.GameRoom;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CardValidator {

    public static final Set<String> FINISHING_RESTRICTED_CARDS = Set.of("2", "3", "J", "K", "8", "Q", "Joker", "A");
    private static final Set<String> QUESTION_CARDS = Set.of("Q", "8");
    private static final Set<String> COUNTER_CARDS = Set.of("2", "3", "Joker", "J", "K", "A");

    public boolean isQuestionCard(Card card) {
        if (card == null) {
            return false;
        }
        return QUESTION_CARDS.contains(card.getValue());
    }

    /**
     * Validates if a single card can be played on the current top card.
     *
     * @param cardToPlay The card the player wants to play.
     * @param topCard    The current card on top of the played pile.
     * @param room       The game room, containing state like activeSuit and drawPenalty.
     * @return True if the play is valid, false otherwise.
     */
    public boolean isValidPlay(Card cardToPlay, Card topCard, GameRoom room) {
        if (cardToPlay == null || topCard == null) {
            return false;
        }

        // Rule: If top card is a Joker, any card is a valid play (unless there's a penalty)
        if ("Joker".equals(topCard.getValue())) {
            return true;
        }

        // Rule: If a draw penalty is active, only counter cards are valid.
        if (room.getDrawPenalty() > 0) {
            // Configurable Rule: Restrict J and K as counters
            if (room.isRestrictJKCounters() && (cardToPlay.getValue().equals("J") || cardToPlay.getValue().equals("K"))) {
                return false;
            }
            // Configurable Rule: Counters must match shape
            if (room.isMatchShapeForCounter() && !cardToPlay.getSuit().equals(topCard.getSuit())) {
                // Aces are an exception, they can always counter
                if (!cardToPlay.getValue().equals("A")) {
                    return false;
                }
            }
            return COUNTER_CARDS.contains(cardToPlay.getValue());
        }

        // Rule: Handle active suit declared by a previous Ace
        if (room.getActiveSuit() != null) {
            return cardToPlay.getSuit().equals(room.getActiveSuit()) || "A".equals(cardToPlay.getValue());
        }

        // Rule: Handle "Question" cards (Q, 8) that set the questionActive flag
        if (room.isQuestionActive()) {
            return cardToPlay.getSuit().equals(topCard.getSuit()) || "A".equals(cardToPlay.getValue());
        }

        // Rule: Wild cards (Ace, Joker) are always playable in a normal turn
        if ("A".equals(cardToPlay.getValue()) || "Joker".equals(cardToPlay.getValue())) {
            return true;
        }

        // Basic Rule: Match suit or value
        return cardToPlay.getSuit().equals(topCard.getSuit()) || cardToPlay.getValue().equals(topCard.getValue());
    }

    /**
     * Validates if a list of cards can be played.
     * All cards must have the same value, and the first card must be a valid play.
     *
     * @param cardsToPlay The list of cards to play.
     * @param topCard     The current card on top of the played pile.
     * @param room        The game room.
     * @return True if the multiple-card play is valid.
     */
        public boolean canPlayMultiple(List<Card> cardsToPlay, Card topCard, GameRoom room) {
            if (cardsToPlay == null || cardsToPlay.isEmpty()) {
                return false;
            }
    
            // Standard rule: all cards have the same value
            final String firstValue = cardsToPlay.get(0).getValue();
            boolean allSameValue = cardsToPlay.stream().allMatch(c -> c.getValue().equals(firstValue));
    
            if (allSameValue) {
                return cardsToPlay.stream().anyMatch(c -> isValidPlay(c, topCard, room));
            }
            
            // Special rule: 8s and Qs of the same suit
            boolean all8sAndQs = cardsToPlay.stream().allMatch(c -> "8".equals(c.getValue()) || "Q".equals(c.getValue()));
            if (all8sAndQs) {
                final String firstSuit = cardsToPlay.get(0).getSuit();
                boolean allSameSuit = cardsToPlay.stream().allMatch(c -> c.getSuit().equals(firstSuit));
                if (allSameSuit) {
                    return cardsToPlay.stream().anyMatch(c -> isValidPlay(c, topCard, room));
                }
            }
    
            return false;
        }

    /**
     * Checks if a player is allowed to finish the game with a specific card.
     *
     * @param card The card being played as the player's last card.
     * @return True if the card is NOT a restricted finishing card, false otherwise.
     */
    public boolean isAllowedToFinishWith(Card card) {
        if (card == null) {
            return false;
        }
        // Rule: Players cannot finish with special action cards.
        return !FINISHING_RESTRICTED_CARDS.contains(card.getValue());
    }
}
