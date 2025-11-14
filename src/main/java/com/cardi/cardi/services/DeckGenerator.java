package com.cardi.cardi.services;

import com.cardi.cardi.model.Card;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DeckGenerator {

    private static final String[] SUITS = {"Hearts", "Spades", "Diamonds", "Clubs"};
    private static final String[] VALUES = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

    /**
     * Creates a standard 54-card deck (including 2 Jokers) and shuffles it.
     *
     * @return A shuffled List of {@link Card}.
     */
    public List<Card> createShuffledDeck() {
        List<Card> deck = new ArrayList<>();

        // Add standard cards
        for (String suit : SUITS) {
            for (String value : VALUES) {
                deck.add(new Card(suit, value));
            }
        }

        // Add Jokers
        deck.add(new Card("Joker", "Joker"));
        deck.add(new Card("Joker", "Joker"));

        // Shuffle the deck
        Collections.shuffle(deck);

        return deck;
    }
}
