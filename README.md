# Deadline Dash

A 2D top-down adventure game set on the IUT campus. You have **36 in-game hours (18 real minutes)** to track down your teammates, complete two main tasks, and submit the project before the deadline hits.

## About

Deadline Dash drops you into a race against the clock on the IUT campus. Explore, talk to NPCs, pick up items, manage your reputation, and juggle side activities — all while the countdown ticks toward your project deadline. How you spend your limited time determines which of the multiple endings you reach.

## Features

- IUT campus exploration
- NPC dialogue and quests
- Item collection & backpack system
- Reputation system
- Football mini-game
- Countdown timer (36 in-game hours / 18 real-world minutes)
- Multiple endings based on your choices

##Developers

Shaquib Wasif - 226; Worked on initial project structure, all the maps, player animation and movement, camera, rep points, timer implementation, opening scene and 2 ending scenes, and quest 1 (football minigame).

Talha Bin Monir - 238; Worked on map collission, transition, generation spritsheets, quest 3 (intruder quest).

Shihab Alam Khan - 236; Worked on npc system modeling, dialogue system, quest manager, quest 2 (laptop quest).

## Built With

- [Java 21](https://openjdk.org/projects/jdk/21/)
- [LibGDX 1.14.2](https://libgdx.com/)
- [Tiled](https://www.mapeditor.org/) — map editing
- [Gradle](https://gradle.org/) — build system
- [JUnit](https://junit.org/) — testing

## Getting Started

### Prerequisites

- JDK 21 or higher installed
- Git

### Clone the Repository

```bash
git clone https://github.com/Shaquibbai/Deadline-Dash.git
cd Deadline-Dash
```

## Running the Game

This project uses the Gradle wrapper, so you don't need Gradle installed separately.

**Desktop (lwjgl3):**

```bash
# macOS / Linux
./gradlew lwjgl3:run

# Windows
gradlew.bat lwjgl3:run
```

## Running Tests

```bash
# macOS / Linux
./gradlew test

# Windows
gradlew.bat test
```

## Project Structure

```
Deadline-Dash/
├── assets/       # Sprites, tilemaps, audio, and other game assets
├── core/         # Core game logic shared across platforms
├── lwjgl3/       # Desktop launcher (LWJGL3 backend)
├── gradle/       # Gradle wrapper files
├── build.gradle
└── settings.gradle
```

## Links
GitHub Repository:
https://github.com/Shaquibbai/Deadline-Dash

Presentation Video:
https://www.youtube.com/watch?v=x2hmunJz5Ps
