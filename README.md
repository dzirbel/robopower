## Robo Power

Robo Power© is a simple card game invented by my brother [Alex Zirbel](https://github.com/azirbel)
at the precocious age of 8. This project is an implementation of the game mechanics (in
[/lib](/lib)) and various AI players (in various `/player-*` modules).

For game rules, see the [rule book](/rulebook.pdf).

### Creating a new player

Players (as implementations of [Player](/lib/src/main/kotlin/com/dzirbel/robopower/Player.kt))
should be added in their own module (like [player-dominic](/player-dominic)). The easiest way to
setup a module is to copy from another one, but it must minimally consist of a `build.gradle.kts`
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

### Gradle reference

The project is built and run with [Gradle](https://gradle.org/). Some common commands:
- `./gradlew :runner:run` runs the main program in [Main.kt](/runner/src/main/kotlin/com/dzirbel/robopower/Main.kt)
- `./gradlew check` compiles and lints code and runs tests in all modules
