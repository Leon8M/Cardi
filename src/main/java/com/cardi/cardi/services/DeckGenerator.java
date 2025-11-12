package com.cardi.cardi.services;

import com.cardi.cardi.model.Card;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class DeckGenerator {

    private static final String[] SUITS = {"Hearts", "Spades", "Diamonds", "Clubs"};
    private static final String[] VALUES = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
    private static final Random RANDOM = new Random();

    public Card drawRandomCard() {
        // Simple random card generation for an "infinite" deck
        String suit = SUITS[RANDOM.nextInt(SUITS.length)];
        String value = VALUES[RANDOM.nextInt(VALUES.length)];
        return new Card(suit, value);
    }

    public Card drawRandomJoker() {
        return new Card("Joker", "Joker");
    }
}
