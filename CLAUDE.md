# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
mvn clean package

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=MatrixTest

# Run the application (Swing GUI)
mvn exec:java -Dexec.mainClass="pl.morph.ai.snake.page.SnakeGame"
# Or: java -jar target/SnakeAi.jar
```

## Constraints

- **Java 8 only** (source/target 8). Do not use Java 9+ APIs (e.g., `ObjectInputFilter`, `List.of()`).
- No external dependencies beyond JUnit 5 (test scope).
- All domain classes implement `Serializable` for save/load via Java serialization (`.ser` files).

## Architecture

This is a **neuroevolution Snake AI** — snakes learn to play via genetic algorithm + neural networks, no backpropagation.

### Simulation Engine (engine/SimulationEngine.java)

`SimulationEngine` runs the simulation on a background thread (not the Swing EDT). It parallelizes snake `look()`/`think()`/`move()` across a thread pool (`ExecutorService`, one thread per CPU core).

1. Per tick: alive snakes are processed in parallel via `invokeAll()`
2. When all snakes die: `calculateFitness()` → `naturalSelection()` → new generation (single-threaded)
3. GA uses **tournament selection** (size 5), **elitism** (top 5 preserved), **single-point crossover**, and **Gaussian mutation**
4. **Headless mode** (default): no rendering, generations run at full CPU speed
5. **Visual mode**: renders snakes with configurable delay, toggle with `V` key

### Board (page/Board.java)

Pure renderer — draws snakes/walls from `SimulationEngine.getSnakes()`. Delegates all hotkey actions to `SimulationEngine`. Human mode still uses a local `Timer` for single-snake play.

### Neural Network (engine/)

- `NeuralNetwork`: Feedforward network with configurable hidden layers. ReLU activation for hidden layers, softmax for output. Weights stored as `Matrix[]`.
- `Matrix`: All linear algebra operations (dot product, crossover, mutation, softmax). Uses `ThreadLocalRandom`.

### Snake Vision & Decision

- **26 inputs**: current direction, tail direction, then 8 directions × 3 values (food seen, body distance, wall distance)
- **3 outputs**: turn left, turn right, go straight (relative to current heading)
- Vision directions are relative to the snake's facing direction, handled by `Direction.look()` which maps relative directions to absolute grid offsets

### Replay System

`Snake.cloneForReplay()` preserves the snake's `foodList` (apple spawn history) so the best snake from each generation can replay its exact game. The best snake clone is added to each new generation with `bestSnake=true` and replays from its recorded food positions.

### Key Hotkeys (AI mode)

- `V`: Toggle headless/visual mode
- `P`/`R`: Pause/Resume
- `S`: Toggle show only best snake
- `D`: Toggle delay (0ms ↔ 20ms)
- `+`/`-`: Double/halve mutation rate
- `W`: Save snakes to .ser file
- `L`: Load snakes from .ser file
- `A`: Enable auto-save
- `B`: Toggle "best only" breeding mode

## Package Structure

- `pl.morph.ai.snake.page` — UI: `SnakeGame` (JFrame entry), `Board` (renderer + key input), `Scores` (stats panel)
- `pl.morph.ai.snake.element` — Game entities: `Snake`, `Apple`, `Direction`, `Wall`, `WallManager`, `Score`
- `pl.morph.ai.snake.engine` — `SimulationEngine` (game loop + GA + threading), `NeuralNetwork`, `Matrix`
