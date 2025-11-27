# Cardi! - Multiplayer Card Game Backend

Cardi! is a real-time multiplayer card game backend built with Java and Spring Boot. It provides the server-side logic for managing game rooms, player actions, and game rules over WebSockets.

## ✨ Features

*   **Real-Time Multiplayer:** Supports multiple simultaneous games with up to 6 players per room.
*   **Room Management:** Players can create or join game rooms using a unique room code.
*   **WebSocket Communication:** All game events are broadcast instantly to players in the room for a seamless, live experience.
*   **Dynamic Game Logic:** Implements the complete ruleset for the Cardi card game, including special card actions.
*   **Player Statistics:** Persists player win counts to a database.
*   **Configurable Rules:** Game rooms can be configured with specific rule variations.

## 🃏 How to Play

Cardi! is a card game where the objective is to be the first to get rid of all your cards.

### Basic Rules
- The game starts with 4 cards dealt to each player.
- Players take turns playing a card that matches the **shape (suit)** or **number (value)** of the top card on the played pile.
- If a player cannot make a move, they must draw one card.
- Multiple cards of the same number can be played together in a single turn.

### Special Cards
| Card            | Effect                                                                                                                                                                                            |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **2 / 3 / Joker**| The next player must draw 2, 3, or 5 cards, respectively. This penalty can be stacked with another penalty card, or countered with a **J**, **K**, or **A**.                                     |
| **J (Jump)**    | Skips the next player in line.                                                                                                                                                                    |
| **K (Kickback)**| Reverses the order of play.                                                                                                                                                                       |
| **Q / 8**       | "Question" cards. The next player must respond with a card of the same **shape**, or they must draw a card.                                                                                        |
| **A (Ace)**     | A wild card that can be played on any card. The player who plays it can change the active shape.                                                                                                    |
| **Finishing**   | Players cannot win the game by playing a special card (2, 3, J, K, 8, Q, Joker, or A) as their final card.                                                                                          |

## 🛠️ Technologies Used

*   **Java 21**
*   **Spring Boot 3.5.7**
    *   Spring Web
    *   Spring WebSocket (for STOMP-based messaging)
    *   Spring Data JPA
    *   Spring Security
*   **Maven** - Dependency Management
*   **H2 Database** - In-memory database for development
*   **MySQL** - For persistent storage of player data
*   **Lombok** - To reduce boilerplate code

## 🏛️ Architecture

The backend follows a classic layered architecture:

*   **Controller Layer (`GameController.java`):** Handles WebSocket connections and routes messages from clients to the appropriate services.
*   **Service Layer (`GameService`, `RoomService`, etc.):** Contains all the core business logic, including game rules, player actions, and state management.
*   **Model Layer (`Card`, `Player`, `GameRoom`, etc.):** Defines the data structures and entities used throughout the application.
*   **Repository Layer (`PlayerRepository.java`):** Manages database operations for persistent entities using Spring Data JPA.
*   **Configuration (`WebSocketConfig.java`, `SecurityConfig.java`):** Configures WebSocket message brokers and security policies.

## 🚀 Getting Started

Follow these instructions to get the backend server up and running on your local machine.

### Prerequisites

*   **JDK 21** or later
*   **Apache Maven**
*   **MySQL Server** (Optional, for production-like setup)

### Installation & Configuration

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Leon8M/Cardi
    cd Cardi
    ```

2.  **Configure the Database:**
    Open `src/main/resources/application.properties`. By default, the application uses an in-memory H2 database.

    To use MySQL, comment out the H2 properties and uncomment the MySQL properties. Make sure to update the `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` to match your MySQL setup.

    ```properties
    # H2 Database Settings (Default)
    spring.h2.console.enabled=true
    spring.datasource.url=jdbc:h2:mem:testdb
    spring.datasource.driverClassName=org.h2.Driver
    spring.datasource.username=sa
    spring.datasource.password=password
    spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

    # MySQL Settings (Uncomment to use)
    # spring.datasource.url=jdbc:mysql://localhost:3306/cardi_db?createDatabaseIfNotExist=true
    # spring.datasource.username=your_mysql_username
    # spring.datasource.password=your_mysql_password
    # spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
    # spring.jpa.hibernate.ddl-auto=update
    # spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
    ```

### Running the Application

You can run the application using the Maven wrapper included in the project.

*   On Linux/macOS:
    ```bash
    ./mvnw spring-boot:run
    ```
*   On Windows:
    ```bash
    ./mvnw.cmd spring-boot:run
    ```

The server will start, and by default, it will be accessible at `http://localhost:8080`.

## 🔌 API Endpoints

The game communicates over WebSockets using the STOMP protocol. The main endpoint is located at:

`ws://localhost:8080/ws/game`

Clients can subscribe to topics and send messages to destinations to perform actions.

### Subscriptions
*   `/topic/game/{roomCode}`: Subscribe to receive real-time `GameState` updates for a specific room.
*   `/user/queue/errors`: Subscribe to receive error messages specific to the user.

### Message Destinations
*   `/app/game.join`: Join a room.
*   `/app/game.start`: Start the game (room creator only).
*   `/app/game.play`: Play one or more cards.
*   `/app/game.draw`: Draw a card from the deck.

The `client.html` file in the root directory provides a basic client for testing the WebSocket communication.

## 📁 Project Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/com/cardi/cardi/
│   │   │   ├── config/          # Spring Security and WebSocket configuration
│   │   │   ├── controller/      # WebSocket message handlers
│   │   │   ├── model/           # Data models (Card, Player, GameRoom)
│   │   │   ├── repository/      # JPA repositories for database access
│   │   │   └── services/        # Core game logic and services
│   │   └── resources/
│   │       └── application.properties # Application configuration
│   └── test/                    # Unit and integration tests
├── pom.xml                      # Maven project configuration
└── client.html                  # Basic HTML client for testing
```
