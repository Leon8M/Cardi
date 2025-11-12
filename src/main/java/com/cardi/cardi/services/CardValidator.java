package com.cardi.cardi.services;

import com.cardi.cardi.model.Card;
import com.cardi.cardi.model.GameRoom;
import org.springframework.stereotype.Component;

@Component
public class CardValidator {

    public boolean isValidPlay(Card cardToPlay, Card topCard, GameRoom room) {
        if (cardToPlay == null) {
            return false;
        }

        // If activeSuit is set from an Ace, the played card's suit must match.
        if (room.getActiveSuit() != null) {
            return cardToPlay.getSuit().equals(room.getActiveSuit());
        }
        
        if (topCard == null) { // Should only happen for the very first card, which is not validated here.
            return true;
        }

        // Ace is always playable
        if (cardToPlay.getValue().equals("A")) {
            return true;
        }

        // Joker is always playable
        if (cardToPlay.getValue().equals("Joker")) {
            return true;
        }

        // Match suit or value
        return cardToPlay.getSuit().equals(topCard.getSuit()) || cardToPlay.getValue().equals(topCard.getValue());
    }
}
