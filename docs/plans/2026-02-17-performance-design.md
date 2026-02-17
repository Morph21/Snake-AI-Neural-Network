# Performance Improvement Design

## Problem

The application runs entirely on the Swing EDT (single thread). With 200 snakes, each performing raycasting (`look()`) and neural network forward passes (`think()`) every tick, only one CPU core is utilized (~10% on a multi-core machine). The app is slow when most snakes are alive.

## Solution

Decouple simulation from the EDT. Introduce a `SimulationEngine` that runs the game loop on a background thread with a thread pool for parallel snake processing. Add headless mode for maximum training speed.

## Architecture

### SimulationEngine (new class)

- Owns `List<Snake>`, GA parameters, generation counter, and the main simulation loop
- Runs on a dedicated background thread (not the EDT)
- Uses `ExecutorService` (fixed thread pool, `availableProcessors()` threads) to parallelize `look()`/`think()`/`move()` across alive snakes each tick
- GA operations (`calculateFitness`, `naturalSelection`, `setBestSnake`) remain single-threaded at generation boundaries
- Extracted from `Board` — all simulation logic moves here

### Board (modified)

- Becomes a pure renderer: `paintComponent` draws current snake state, no longer drives simulation via `actionPerformed`
- `Timer` removed as simulation driver
- Holds reference to `SimulationEngine` for reading snake state and dispatching hotkey actions

### Headless vs Visual Mode

- **Headless** (default): tight loop, no repaint, generations run continuously at full CPU speed. Console still prints generation/score/fitness.
- **Visual**: simulation thread sleeps ~20ms per tick, calls `SwingUtilities.invokeLater(repaint)` to render. Watch snakes live.
- Toggle with `V` key at any time.

### Per-Tick Data Flow

1. Simulation thread collects alive snakes
2. Submits batches to thread pool via `invokeAll()`
3. Each task: `snake.look()` / `snake.think()` / `snake.move()`
4. Wait for all tasks to complete
5. Check dead count, update high score, check generation end
6. If visual: invokeLater(repaint) + sleep(delay)

### Generation Boundary (single-threaded)

1. `calculateFitness()` all snakes
2. `naturalSelection()` — sort, select best, breed
3. Update scores panel via `SwingUtilities.invokeLater()`
4. If headless: immediately start next generation

### Thread Safety

- Each snake only reads/writes its own state during look/think/move. `walls` is immutable, board dimensions are final.
- `volatile boolean visualMode` — toggled by EDT, read by simulation thread
- `volatile boolean running` — for pause/resume
- Snake list reference swap (`snakes = newSnakes`) at generation boundary is a single assignment; use `volatile` reference so EDT sees the update for painting.
- Save/load triggered by flag, handled at generation boundary to avoid concurrent modification.

### Error Handling

- `Future.get()` after `invokeAll()` catches exceptions; failed snakes marked dead
- Executor shutdown in `finally` block on simulation stop or window close

### Unchanged

- `Snake`, `NeuralNetwork`, `Matrix`, `Direction`, `Apple`, `Wall`, `WallManager` — no modifications
- Save/load serialization format — backward compatible
- All existing hotkeys preserved
