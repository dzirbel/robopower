## Robo Power

[![Build status](https://github.com/dzirbel/robopower/workflows/Build/badge.svg)](https://github.com/dzirbel/robopower/actions/workflows/build.yml)

Robo Power© is a simple card game invented by my brother [Alex Zirbel](https://github.com/azirbel)
at the precocious age of 8. This project is an implementation of the game mechanics (in
[/lib](/lib)) and AI players (in various `/player-*` modules).

For game rules, see the [rule book](/rulebook.pdf).

### Creating a new player

Players (as implementations of [Player](/lib/src/main/kotlin/com/dzirbel/robopower/Player.kt))
should be added in their own module (like [player-dominic](/player-dominic)). The easiest way to
set up a module is to copy from another one, but it must minimally consist of a `build.gradle.kts`
file and code in `src/main/kotlin` (or `../java`). The module must also be added to
`settings.gradle.kts` and as a dependency in `/runner/build.gradle.kts`. Players may not be placed
in `/lib`, since fields like other players' cards are restricted as `internal` (players are also
forbidden to use reflection to access these fields).

Implementing a `Player` requires only implementing functions to choose discarded cards, dueling
cards, and players to spy. To help with the logic there are a number of hooks built into the engine:
- various `onX()` callbacks in `Player` (for events that only this player has knowledge of)
- various `GameEvent`s which can be listened to via `Game.onEvent()` or accessed in `Game.eventLog`
- built-in tracking of known cards held by other players via `CardTracker`, which can be 
  conveniently used by extending `PlayerWithCardTracker`

### Setup and Gradle reference

The recommended programming environment is [IntelliJ IDEA](https://www.jetbrains.com/idea/)
(Community Edition is fine). The project is built and run with [Gradle](https://gradle.org/), which
integrates well with IntelliJ (after allowing it to sync after opening the project). The project can
also be run from the command line with these commands (among others, see `./gradlew tasks`):
- `./gradlew :runner:run` runs the main program in [Main.kt](/runner/src/main/kotlin/com/dzirbel/robopower/Main.kt)
- `./gradlew check` compiles and lints code and runs tests in all modules

These are also available as IntelliJ run configurations checked into git alongside the project.
