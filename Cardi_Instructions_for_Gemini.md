# Cardi! — Comprehensive Development Instructions for Gemini CLI

## Overview
This document provides the **complete set of detailed development instructions** for the Gemini CLI tool to generate the logic and structure for the **Cardi! Multiplayer Card Game**, built in **Spring Boot (Java)**.  
Gemini should read and follow these instructions carefully to build a functioning multiplayer backend that adheres to the full Cardi! ruleset and design outlined below.

The current Spring Boot project is already set up with:
- Core dependencies (Spring Boot Web, WebSocket, Data JPA, Lombok, H2/MySQL, Security)
- Base package structure
- Basic configuration (`pom.xml` and `application.properties`)

Gemini **must not modify** the `pom.xml` or `application.properties` files under any circumstance.  
If additional dependencies or configurations are required, Gemini should **prompt the developer at the end** of the generation process with a clear list of what needs to be added.

---

## 🎯 Project Objective
Develop a **real-time multiplayer card game** named **Cardi!**, playable via WebSockets and capable of supporting multiple simultaneous game rooms (up to 100).  
Players can create or join a room, draw and play cards according to the rules, and win by finishing all cards in hand.

---

## 🧠 Key Functional Features
1. **Room Management**
   - Players can create or join rooms using a room code.
   - Up to 6 players per room.
   - Once a game starts, no one else can join.
   - Room creator can start and reset games.

2. **Gameplay Loop**
   - Game starts by dealing 4 cards per player.
   - Random top card placed in the middle as the starting card.
   - Turns progress in circular order.
   - On a player’s turn:
     - They can play a card matching the top card (shape or number).
     - If they cannot play, they must draw one card.
     - Play continues to the next player.
   - The game continues until a player finishes all their cards.
   - Players can click **Cardi!** to declare intent to win on their next valid play.

3. **Real-Time Communication**
   - Use WebSockets for live updates and actions.
   - Broadcast every move, card draw, or state change to all players in the room instantly.

4. **Player Management**
   - Players connect via username (simple authentication or guest mode).
   - Persistent tracking of player wins (via MySQL).

---

## 🃏 Card Rules & Logic

### Basic Rules
- Deck has infinite random cards for simplicity.
- Match top card by **shape** or **number** to play.
- Multiple same-number cards can be played together if the first one matches the top card.
- If no playable card → player must **draw** from the deck.

### Special Cards
| Card | Effect |
|------|---------|
| **2 / 3 / Joker** | Next player must draw 2, 3, or 5 cards respectively. The next player can counter using: <br>• Another 2/3/Joker to stack penalty<br>• **J (Jump)** → skips next player, penalty passes<br>• **K (Kickback)** → reverses order, penalty passes backward<br>• **A (Ace)** → cancels penalty |
| **J (Jump)** | Skips the next player. Can chain. |
| **K (Kickback)** | Reverses order. Can chain (two Ks return turn to the same player). |
| **Q / 8 (Question)** | Must be answered by a card of the same **shape**. If not, player must draw one card. |
| **A (Ace)** | Wild card; can change shape. Multiple Aces can allow specifying a **specific card**. |
| **Finishing Play Restriction** | Players cannot finish with: 2, 3, J, K, 8, Q (if unanswered), Joker, or A. |

---

## ⚙️ System Architecture

### Layers
1. **Controller Layer**
   - Handles WebSocket endpoints (`/ws/game`).
   - Manages message routing for actions (play, draw, call Cardi!, finish).

2. **Service Layer**
   - `GameService` — Implements all game logic, card actions, and validation.
   - `RoomService` — Handles room creation, joining, and player order.
   - `PlayerService` — Optional authentication and stats tracking.

3. **Model Layer**
   - `Card`, `Player`, `GameRoom`, `GameState`, `ActionMessage`.

4. **Config Layer**
   - `WebSocketConfig` (STOMP setup).
   - Security config (optional, JWT not required for base logic).

---

## 🧩 Entities Overview

### `Card`
- `String suit` (Hearts, Spades, Diamonds, Clubs)
- `String value` (2,3,4,...,A,J,K,Q,Joker)
- Utility methods for comparison.

### `Player`
- `String id`
- `String username`
- `List<Card> hand`
- `int wins`

### `GameRoom`
- `String roomCode`
- `List<Player> players`
- `Stack<Card> drawPile`
- `Stack<Card> playedPile`
- `int currentPlayerIndex`
- `boolean isReversed`
- `boolean started`
- Config flags (see next section)

### `GameState`
- Holds the dynamic snapshot of the current game for broadcast to all clients.

---

## ⚙️ Configurable Room Rules
Each `GameRoom` should store:
```java
private boolean matchShapeForCounter = false; // Default: false
private Integer maxCardsAllowed = null;       // Default: no limit
private boolean restrictJKCounters = false;   // Default: false
```

---

## 🧱 Game Logic Flow Summary
1. Create room → players join.
2. Deal 4 cards → reveal one as `topCard`.
3. Random player starts.
4. On each turn:
   - If player has playable card(s), play one or a valid multiple.
   - If none, draw one.
   - Resolve any special actions (2/3/Joker penalties, skips, reversals, etc.)
   - Pass turn to next player (order reversed if K played).
5. If player plays all cards → win detected → broadcast win.
6. Record win → allow replay in same room.

---

## 🧠 Implementation Suggestions for Gemini
- Build core logic first: `GameService`, `GameRoom`, `Player`, `Card`, and helper utils.
- Implement card validation and turn mechanics before adding WebSocket broadcast.
- Keep data structures thread-safe (`ConcurrentHashMap`, `CopyOnWriteArrayList`).
- Test logic with simulated players (unit tests in `src/test/java`).
- Only after logic passes tests → integrate WebSocket controllers.
- Add comments for each special case in logic for clarity.

---

## ⚠️ Restrictions for Gemini CLI
1. **Do not edit**:
   - `pom.xml`
   - `application.properties`
2. If new dependencies or configurations are required, **prompt the user** at the end of generation with:
   - Dependency name
   - Purpose
   - Example snippet to add manually

3. **Focus Scope:**
   - Game logic and service classes only
   - No frontend or UI
   - No external APIs

---

## ✅ Deliverables
Gemini CLI should generate:
1. Models:
   - `Card.java`
   - `Player.java`
   - `GameRoom.java`
   - `GameState.java`
2. Services:
   - `GameService.java` (core rules + round flow)
   - `RoomService.java`
3. Controller:
   - `GameController.java`
4. Optional Utils:
   - `DeckGenerator.java`
   - `CardValidator.java`

---

## 📣 Final Reminder for Gemini
After logic generation is complete, display a final summary message:
> “✅ Cardi! backend logic generated successfully.  
> Please review suggested dependency or config changes below before running the application.”

This will ensure all logic remains isolated and easily testable without unintended configuration edits.
