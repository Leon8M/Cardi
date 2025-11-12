# Development Summary for Cardi!

This document summarizes the backend development work completed for the Cardi! multiplayer card game.

## Overall Progress

The core backend logic and communication layer for the Cardi! game are now implemented. We have a functional server that can manage multiple game rooms, handle player actions in real-time, and enforce the game's rules. Player wins are persisted to a database.

The next steps would typically involve:
1.  Thoroughly testing the backend logic with unit and integration tests.
2.  Developing a frontend client to interact with the WebSocket endpoints.
3.  Deploying the application to a server.

## File Structure

The final file structure for the implemented source code is as follows:

```
src/
└── main/
    └── java/
        └── com/
            └── cardi/
                └── cardi/
                    ├── CardiApplication.java
                    ├── config/
                    │   └── WebSocketConfig.java
                    ├── controller/
                    │   └── GameController.java
                    ├── model/
                    │   ├── ActionMessage.java
                    │   ├── Card.java
                    │   ├── GameRoom.java
                    │   ├── GameState.java
                    │   └── Player.java
                    ├── repository/
                    │   └── PlayerRepository.java
                    └── services/
                        ├── CardValidator.java
                        ├── DeckGenerator.java
                        ├── GameService.java
                        ├── PlayerService.java
                        └── RoomService.java
```

## File Breakdown

### Models (`src/main/java/com/cardi/cardi/model/`)

#### `Card.java`
- **Purpose:** Represents a single playing card.
- **Variables:**
    - `suit` (String): The suit of the card (e.g., "Hearts", "Spades").
    - `value` (String): The value of the card (e.g., "2", "K", "Ace").

#### `Player.java`
- **Purpose:** Represents a player in the game. This is a JPA entity, meaning it's mapped to a database table.
- **Variables:**
    - `id` (String): The unique identifier for the player (primary key).
    - `username` (String): The player's chosen name.
    - `hand` (List<Card>): The list of cards the player currently holds. Marked as `@Transient` so it's not stored in the database.
    - `wins` (int): The number of games the player has won. This value is persisted.

#### `GameRoom.java`
- **Purpose:** Represents a single game room and its state.
- **Variables:**
    - `roomCode` (String): The unique code to join the room.
    - `players` (List<Player>): A thread-safe list of players in the room.
    - `drawPile` (Stack<Card>): The pile of cards to draw from (conceptually infinite).
    - `playedPile` (Stack<Card>): The pile of cards that have been played.
    - `currentPlayerIndex` (int): The index of the current player in the `players` list.
    - `isReversed` (boolean): `true` if the turn order is reversed.
    - `started` (boolean): `true` if the game has started.
    - `matchShapeForCounter`, `maxCardsAllowed`, `restrictJKCounters`: Configurable room rules (as per instructions).
    - `drawPenalty` (int): The number of cards the next player must draw due to a 2, 3, or Joker.
    - `questionActive` (boolean): `true` if a Q or 8 has been played and needs to be answered.
    - `activeSuit` (String): The suit chosen by a player after playing an Ace.

#### `GameState.java`
- **Purpose:** A data transfer object (DTO) that represents a snapshot of the game state. This is sent to all clients over WebSockets to keep them in sync.
- **Variables:** Contains a subset of the `GameRoom` state necessary for the client to render the game (e.g., `players`, `topCard`, `currentPlayerIndex`).

#### `ActionMessage.java`
- **Purpose:** A DTO for receiving player actions over WebSockets.
- **Variables:**
    - `playerId` (String): The ID of the player performing the action.
    - `action` (String): The type of action (e.g., "PLAY", "DRAW").
    - `cards` (List<Card>): The card(s) being played.
    - `newSuit` (String): The suit chosen when playing an Ace.

### Repositories (`src/main/java/com/cardi/cardi/repository/`)

#### `PlayerRepository.java`
- **Purpose:** A Spring Data JPA repository for database operations on the `Player` entity.
- **Functions:**
    - `findByUsername(String username)`: A custom method to find a player by their username.
    - Inherits standard CRUD methods from `JpaRepository` (e.g., `save`, `findById`).

### Services (`src/main/java/com/cardi/cardi/services/`)

#### `RoomService.java`
- **Purpose:** Manages the lifecycle of game rooms.
- **Variables:**
    - `gameRooms` (Map<String, GameRoom>): A thread-safe map storing all active game rooms.
- **Functions:**
    - `createRoom()`: Creates a new room with a unique code.
    - `joinRoom(String roomCode, Player player)`: Adds a player to a room.
    - `getRoom(String roomCode)`: Retrieves a room.
    - `removePlayer(String roomCode, String playerId)`: Removes a player from a room.

#### `PlayerService.java`
- **Purpose:** Manages player data and persistence.
- **Functions:**
    - `getOrCreatePlayer(String username)`: Finds a player by username or creates a new one if they don't exist.
    - `incrementWins(String playerId)`: Increments the win count for a player and saves it to the database.

#### `DeckGenerator.java`
- **Purpose:** A utility to generate random cards, simulating an infinite deck.
- **Functions:**
    - `drawRandomCard()`: Returns a new random `Card`.

#### `CardValidator.java`
- **Purpose:** A utility to validate game moves.
- **Functions:**
    - `isValidPlay(Card cardToPlay, Card topCard, GameRoom room)`: Checks if a card can be legally played based on the current game state (top card, active penalties, active suit from an Ace, etc.).

#### `GameService.java`
- **Purpose:** The core of the application, containing all the game logic.
- **Functions:**
    - `startGame(String roomCode)`: Initializes the game, deals cards, and sets the first card.
    - `drawCard(String roomCode, String playerId)`: Handles a player drawing a card.
    - `playCard(String roomCode, String playerId, List<Card> cards, String newSuit)`: The main logic hub. It validates the move, handles all special card effects (penalties, skips, reverses, etc.), checks for win conditions, and updates the game state.
    - `endTurn(String roomCode)`: Advances the turn to the next player, considering the direction of play.
    - `getGameState(String roomCode, String message)`: Creates a `GameState` object to be sent to clients.

### Controllers (`src/main/java/com/cardi/cardi/controller/`)

#### `GameController.java`
- **Purpose:** Handles all real-time communication via WebSockets.
- **Functions:**
    - It uses `@MessageMapping` annotations to define endpoints for different game actions (`join`, `start`, `play`, `draw`).
    - It receives `ActionMessage` objects from clients.
    - It calls the appropriate service methods (`GameService`, `RoomService`, `PlayerService`) to process the actions.
    - It uses `SimpMessagingTemplate` to broadcast the updated `GameState` to all clients subscribed to a specific room's topic (e.g., `/topic/game/{roomCode}`).
