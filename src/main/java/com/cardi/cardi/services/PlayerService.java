package com.cardi.cardi.services;

import com.cardi.cardi.model.Player;
import com.cardi.cardi.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    public Player getOrCreatePlayer(String username) {
        return playerRepository.findByUsername(username)
                .orElseGet(() -> playerRepository.save(new Player(username)));
    }

    public void incrementWins(String playerId) {
        playerRepository.findById(playerId).ifPresent(player -> {
            player.setWins(player.getWins() + 1);
            playerRepository.save(player);
        });
    }
}
