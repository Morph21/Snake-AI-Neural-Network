# Performance Improvement Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Move simulation off the Swing EDT into a multi-threaded engine with headless mode for maximum training speed.

**Architecture:** New `SimulationEngine` class runs the game loop on a background thread, parallelizing snake look/think/move across a thread pool. Board becomes a pure renderer. Headless mode skips rendering for continuous generation training.

**Tech Stack:** Java 8, Swing, `java.util.concurrent.ExecutorService`

---

### Task 1: Create SimulationEngine skeleton

Extract simulation state and GA logic from Board into a new SimulationEngine class. No threading yet — just move the code.

**Files:**
- Create: `src/main/java/pl/morph/ai/snake/engine/SimulationEngine.java`
- Modify: `src/main/java/pl/morph/ai/snake/page/Board.java`
- Modify: `src/main/java/pl/morph/ai/snake/page/Scores.java`
- Modify: `src/main/java/pl/morph/ai/snake/element/WallManager.java`

**Step 1: Create SimulationEngine with extracted fields and GA methods**

Create `SimulationEngine.java` with all simulation state moved from Board. The static fields that `Scores` currently reads from `Board` (`MUTATION_RATE`, `CROSSOVER_RATE`, `SAVE_SNAKE_RATIO`, `avgFitness`, `autoSave`, `bestOnly`) become instance fields on SimulationEngine, and Scores reads them via a reference to SimulationEngine instead.

```java
package pl.morph.ai.snake.engine;

import pl.morph.ai.snake.element.*;
import pl.morph.ai.snake.page.Scores;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.*;
import java.util.List;

public class SimulationEngine {

    public static final int B_WIDTH = 800;
    public static final int B_HEIGHT = 800;

    private int delay = 0;
    private int dotSize = 40;
    private String savePath = "C:\\repo\\github\\AI";

    private volatile List<Snake> snakes;
    private Snake bestSnake;
    private int bestSnakeScore;
    private int highScore = 0;
    private double bestFitness;
    private double fitnessSum;
    private int aiSnakeCount;

    public double mutationRate = 0.03;
    public double crossoverRate = 0.9;
    public double saveSnakeRatio = 0.5;
    public double avgFitness = 0;
    public boolean autoSave = false;
    public boolean bestOnly = false;

    private boolean showOnlyFirstSnake = false;
    private boolean saveWaiting = false;
    private String fileName = null;

    private List<Wall> walls;
    private Scores scores;
    private JPanel boardPanel;

    private volatile boolean running = false;
    private volatile boolean visualMode = false;

    private static final int ELITISM_COUNT = 5;
    private static final int TOURNAMENT_SIZE = 5;

    public SimulationEngine(Scores scores, int aiSnakeCount, JPanel boardPanel) {
        this.scores = scores;
        this.aiSnakeCount = aiSnakeCount;
        this.boardPanel = boardPanel;
        this.walls = WallManager.prepareWalls(dotSize, B_WIDTH, B_HEIGHT);
        createAISnakes();
    }

    private void createAISnakes() {
        snakes = new ArrayList<>();
        for (int i = 0; i < aiSnakeCount; i++) {
            Snake snake = new Snake(B_WIDTH, B_HEIGHT, delay, false, null, dotSize, walls);
            placeSnakeRandomly(snake);
            if (!showOnlyFirstSnake) {
                snake.setShowIt(true);
            }
            snake.spawnApple();
            snakes.add(snake);
        }
    }

    public void placeSnakeRandomly(Snake snake) {
        List<Snake.XY> boardList = snake.populate(B_WIDTH / dotSize, B_HEIGHT / dotSize);
        int index = (int) (Math.random() * boardList.size());
        Snake.XY xy = boardList.get(index);
        snake.setStartingPosition(xy);
    }

    public void simulateTick() {
        int deadCount = 0;
        for (Snake snake : snakes) {
            if (snake.getScore() == ((B_HEIGHT / dotSize) * (B_WIDTH / dotSize)) - 1) {
                running = false;
                return;
            }
            if (snake.isBestSnake()) {
                scores.setScore((new Score()).setScore(snake.getScore()));
            }
            if (snake.inGame) {
                snake.look();
                snake.think();
                snake.move();
                if (snake.getScore() > highScore) {
                    highScore = snake.getScore();
                    scores.setHighestScore(highScore);
                    saveWaiting = true;
                }
            } else {
                deadCount++;
            }
        }

        if (deadCount >= aiSnakeCount || deadCount >= snakes.size()) {
            scores.resetScores();
            calculateFitness();
            naturalSelection();
            scores.setHighestFitness(bestFitness);
            System.out.println(scores.getGeneration() - 1 + "\t" + bestSnakeScore + ". " + bestFitness);
            if (autoSave && saveWaiting) {
                autoSave();
            }
        }
    }

    void calculateFitness() {
        for (Snake snake : snakes) {
            snake.calculateFitness();
        }
    }

    void naturalSelection() {
        List<Snake> newSnakes = new ArrayList<>();

        Collections.sort(snakes, new Comparator<Snake>() {
            public int compare(Snake a, Snake b) {
                return Double.compare(b.getFitness(), a.getFitness());
            }
        });
        setBestSnake();
        calculateFitnessSum();

        Snake best = bestSnake.cloneForReplay();
        best.setBestSnake(true);
        best.setScores(scores);
        best.setShowIt(true);
        best.spawnApple();
        newSnakes.add(best);

        for (int i = 0; i < Math.min(ELITISM_COUNT, snakes.size()) && newSnakes.size() < aiSnakeCount; i++) {
            Snake elite = snakes.get(i);
            NeuralNetwork brain = elite.getBrain().clone();
            Snake eliteSnake = new Snake(B_WIDTH, B_HEIGHT, delay, false, null, dotSize, walls);
            eliteSnake.setBrain(brain);
            eliteSnake.setBestSnake(false);
            eliteSnake.setScores(null);
            placeSnakeRandomly(eliteSnake);
            if (!showOnlyFirstSnake) {
                eliteSnake.setShowIt(true);
            }
            eliteSnake.spawnApple();
            newSnakes.add(eliteSnake);
        }

        while (newSnakes.size() < aiSnakeCount) {
            Snake parent1 = tournamentSelect();
            Snake parent2 = tournamentSelect();
            Snake child;
            double rand = Matrix.random(0, 1);
            if (rand < crossoverRate) {
                child = parent1.crossover(parent2);
            } else {
                child = parent1.cloneThis();
            }
            child.mutate();
            NeuralNetwork brain = child.getBrain();
            child = new Snake(B_WIDTH, B_HEIGHT, delay, false, null, dotSize, walls);
            child.setBrain(brain);
            child.setBestSnake(false);
            child.setScores(null);
            placeSnakeRandomly(child);
            if (!showOnlyFirstSnake) {
                child.setShowIt(true);
            }
            child.spawnApple();
            newSnakes.add(child);
        }

        snakes = newSnakes;
        scores.increseGeneration();
    }

    void calculateFitnessSum() {
        fitnessSum = 0;
        for (Snake snake : snakes) {
            fitnessSum += snake.getFitness();
        }
        avgFitness = fitnessSum / snakes.size();
    }

    Snake tournamentSelect() {
        if (bestOnly) {
            return snakes.get(0);
        }
        int poolSize = Math.max(1, (int) (snakes.size() * saveSnakeRatio));
        Snake best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int idx = (int) (Math.random() * poolSize);
            Snake candidate = snakes.get(idx);
            if (best == null || candidate.getFitness() > best.getFitness()) {
                best = candidate;
            }
        }
        return best;
    }

    void setBestSnake() {
        int bestScoreIdx = 0;
        double bestFitnessIdx = 0;
        int maxIdx = 0;
        for (int i = 0; i < snakes.size(); i++) {
            Snake snake = snakes.get(i);
            if (snake.getScore() > snakes.get(bestScoreIdx).getScore()) {
                bestScoreIdx = i;
            }
            if (snake.getFitness() > bestFitnessIdx) {
                bestFitnessIdx = snake.getFitness();
                maxIdx = i;
            }
        }
        int chosen = bestScoreIdx;
        if (snakes.get(bestScoreIdx).getScore() == snakes.get(maxIdx).getScore()) {
            chosen = maxIdx;
        }
        Snake candidate = snakes.get(chosen);
        if (candidate.getScore() > bestSnakeScore || candidate.getFitness() > bestFitness) {
            bestFitness = candidate.getFitness();
            bestSnake = candidate.cloneForReplay();
            bestSnakeScore = candidate.getScore();
        } else {
            bestSnake = bestSnake.cloneForReplay();
        }
    }

    // --- Accessors for Board rendering and hotkeys ---

    public List<Snake> getSnakes() {
        return snakes;
    }

    public List<Wall> getWalls() {
        return walls;
    }

    public boolean isVisualMode() {
        return visualMode;
    }

    public void setVisualMode(boolean visualMode) {
        this.visualMode = visualMode;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isShowOnlyFirstSnake() {
        return showOnlyFirstSnake;
    }

    public void toggleShowOnlyFirstSnake() {
        showOnlyFirstSnake = !showOnlyFirstSnake;
        for (Snake snake : snakes) {
            if (showOnlyFirstSnake) {
                snake.setShowIt(snake.isBestSnake());
            } else {
                snake.setShowIt(true);
            }
        }
    }

    public void toggleDelay() {
        if (delay == 0) {
            delay = 20;
        } else {
            delay = 0;
        }
    }

    public int getDelay() {
        return delay;
    }

    public int getDotSize() {
        return dotSize;
    }

    public void increaseMutationRate() {
        mutationRate *= 2;
        if (mutationRate > 1) mutationRate = 1;
    }

    public void decreaseMutationRate() {
        mutationRate /= 2;
    }

    public void increaseSaveSnakeRatio() {
        saveSnakeRatio += 0.1;
        if (saveSnakeRatio > 1) saveSnakeRatio = 1;
    }

    public void decreaseSaveSnakeRatio() {
        saveSnakeRatio -= 0.1;
        if (saveSnakeRatio < 0.1) saveSnakeRatio = 0.1;
    }

    public void toggleBestOnly() {
        bestOnly = !bestOnly;
    }

    public void saveToFile(JPanel parent) {
        try {
            final JFileChooser fc = new JFileChooser(savePath);
            fc.setFileFilter(new FileNameExtensionFilter("*.ser", "ser"));
            int returnVal = fc.showSaveDialog(parent);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                fileName = file.getAbsolutePath();
                List<Snake> toSave = new ArrayList<>(snakes);
                Collections.sort(toSave, new Comparator<Snake>() {
                    public int compare(Snake a, Snake b) {
                        return Double.compare(b.getFitness(), a.getFitness());
                    }
                });
                try (FileOutputStream fout = new FileOutputStream(fileName);
                     ObjectOutputStream oos = new ObjectOutputStream(fout)) {
                    if (toSave.size() > 1000) {
                        oos.writeObject(new ArrayList<>(toSave.subList(0, 1000)));
                    } else {
                        oos.writeObject(toSave);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void enableAutoSave(JPanel parent) {
        autoSave = true;
        saveToFile(parent);
    }

    @SuppressWarnings("unchecked")
    public void readFromFile(JPanel parent) {
        try {
            final JFileChooser fc = new JFileChooser(savePath);
            fc.setFileFilter(new FileNameExtensionFilter("*.ser", "ser"));
            int returnVal = fc.showOpenDialog(parent);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                List<Snake> readCase;
                try (FileInputStream streamIn = new FileInputStream(file.getAbsoluteFile());
                     ObjectInputStream objectinputstream = new ObjectInputStream(streamIn)) {
                    readCase = (List<Snake>) objectinputstream.readObject();
                }
                System.out.println("Snakes loaded");
                for (Snake snake : readCase) {
                    int oldDotSize = snake.getDotSize();
                    snake.setDotSize(dotSize);
                    snake.setWalls(walls);
                    List<Apple> foodList = snake.getFoodList();
                    int diff;
                    boolean lesser;
                    if (oldDotSize > dotSize) {
                        diff = oldDotSize / dotSize;
                        lesser = false;
                    } else {
                        diff = dotSize / oldDotSize;
                        lesser = true;
                    }
                    if (foodList != null) {
                        for (Apple apple : foodList) {
                            shiftApple(apple, lesser, diff);
                        }
                        snake.setFoodList(foodList);
                    }
                    Apple appleToEat = snake.getAppleToEat();
                    if (appleToEat != null) {
                        shiftApple(appleToEat, lesser, diff);
                    }
                }
                snakes = readCase;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shiftApple(Apple apple, boolean lesser, int diff) {
        int ax = apple.getApple_x();
        int ay = apple.getApple_y();
        if (ax != 0) { ax = lesser ? ax * diff : ax / diff; }
        if (ay != 0) { ay = lesser ? ay * diff : ay / diff; }
        apple.setApple_x(ax);
        apple.setApple_y(ay);
    }

    private void autoSave() {
        try {
            if (fileName != null && !fileName.isEmpty()) {
                try (FileOutputStream fout = new FileOutputStream(fileName);
                     ObjectOutputStream oos = new ObjectOutputStream(fout)) {
                    oos.writeObject(snakes);
                    saveWaiting = false;
                    System.out.println("Auto save triggered");
                }
            }
        } catch (Exception e) {
            System.out.println("AutoSave failed");
            e.printStackTrace();
        }
    }
}
```

**Step 2: Update WallManager to accept dimensions as parameters**

Currently `WallManager.prepareWalls` references `Board.B_WIDTH` and `Board.B_HEIGHT` statically. Change it to accept parameters so it no longer depends on Board.

In `src/main/java/pl/morph/ai/snake/element/WallManager.java`, change the method signature:

```java
public static List<Wall> prepareWalls(int dotSize, int boardWidth, int boardHeight) {
    List<Wall> walls = new ArrayList<>();
    int rows = boardHeight / dotSize;
    int columns = boardWidth / dotSize;
    return walls;
}
```

Remove the `import pl.morph.ai.snake.page.Board;` line.

**Step 3: Update Scores to read from SimulationEngine instead of Board statics**

In `src/main/java/pl/morph/ai/snake/page/Scores.java`:
- Add a `SimulationEngine` field and setter
- Replace `Board.avgFitness`, `Board.MUTATION_RATE`, `Board.SAVE_SNAKE_RATIO`, `Board.autoSave`, `Board.bestOnly` references in `generationInfo()` with reads from the engine instance

```java
// Add field
private SimulationEngine engine;

public void setEngine(SimulationEngine engine) {
    this.engine = engine;
}
```

In `generationInfo()`, replace:
- `Board.avgFitness` → `engine.avgFitness`
- `Board.MUTATION_RATE` → `engine.mutationRate`
- `Board.SAVE_SNAKE_RATIO` → `engine.saveSnakeRatio`
- `Board.autoSave` → `engine.autoSave`
- `Board.bestOnly` → `engine.bestOnly`

Remove the `import pl.morph.ai.snake.page.Board;` from Scores (if it was only used for these statics — it's not imported currently, the statics are accessed via the class name within the same package).

**Step 4: Update Snake.java to read mutationRate from its own field**

`Snake.java:83` currently reads `Board.MUTATION_RATE`. Change this to accept mutation rate as a constructor parameter or setter. Since `mutationRate` is already an instance field on Snake, just stop initializing it from `Board.MUTATION_RATE`:

In `Snake.java` constructor, change line 83 from:
```java
this.mutationRate = Board.MUTATION_RATE;
```
to:
```java
this.mutationRate = 0.03;
```

Then in `SimulationEngine.createAISnakes()` and `naturalSelection()`, after creating each snake call `snake.setMutationRate(mutationRate)`.

Remove the `import pl.morph.ai.snake.page.Board;` from Snake.java.

**Step 5: Rewrite Board as a thin renderer**

Gut `Board.java` to only contain:
- Constructor that takes `SimulationEngine` reference
- `paintComponent()` — reads snakes and walls from engine
- Key adapter — delegates all hotkey actions to engine methods
- Human playing mode stays in Board (unchanged, still uses Timer for single-snake play)

```java
package pl.morph.ai.snake.page;

import pl.morph.ai.snake.element.*;
import pl.morph.ai.snake.engine.SimulationEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class Board extends JPanel implements ActionListener {

    public static final int B_WIDTH = 800;
    public static final int B_HEIGHT = 800;

    private int DOT_SIZE = 40;

    private Snake snake;
    private boolean humanPlaying;
    private SimulationEngine engine;
    private Scores scores;
    private List<Wall> walls;
    private Timer timer;

    // AI mode constructor
    public Board(Scores scores, SimulationEngine engine) {
        this.humanPlaying = false;
        this.scores = scores;
        this.engine = engine;
        this.walls = engine.getWalls();
        addKeyListener(new TAdapter());
        setBackground(Color.black);
        setFocusable(true);
        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));
    }

    // Human mode constructor
    public Board(Scores scores) {
        this.humanPlaying = true;
        this.scores = scores;
        this.walls = WallManager.prepareWalls(DOT_SIZE, B_WIDTH, B_HEIGHT);
        addKeyListener(new TAdapter());
        setBackground(Color.black);
        setFocusable(true);
        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));
        createHumanSnake();
        timer = new Timer(60, this);
    }

    private void createHumanSnake() {
        this.snake = new Snake(B_WIDTH, B_HEIGHT, 60, true, null, DOT_SIZE, walls);
        this.snake.setShowIt(true);
        List<Snake.XY> boardList = snake.populate(B_WIDTH / DOT_SIZE, B_HEIGHT / DOT_SIZE);
        int index = (int) (Math.random() * boardList.size());
        snake.setStartingPosition(boardList.get(index));
        snake.spawnApple();
    }

    public void startTimer() {
        if (timer != null) {
            timer.start();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (humanPlaying) {
            if (snake.inGame) {
                snake.doDrawing(g);
            } else {
                gameOver(g);
            }
        } else {
            List<Snake> currentSnakes = engine.getSnakes();
            if (currentSnakes != null) {
                for (Snake s : currentSnakes) {
                    if (s.inGame || s.isBestSnake()) {
                        s.doDrawing(g);
                    }
                }
            }
        }
        if (walls != null && !walls.isEmpty()) {
            for (Wall wall : walls) {
                wall.doDrawing(g);
            }
        }
        Toolkit.getDefaultToolkit().sync();
    }

    private void gameOver(Graphics g) {
        String msg = "Game Over";
        String restartGameMsg = "Press space bar to restart game";
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);
        Font info = new Font("Helvetica", Font.BOLD, 10);
        FontMetrics infoMetr = getFontMetrics(info);
        g.setColor(Color.white);
        g.setFont(small);
        g.drawString(msg, (B_WIDTH - metr.stringWidth(msg)) / 2, B_HEIGHT / 2);
        g.setFont(info);
        g.drawString(restartGameMsg, (B_WIDTH - infoMetr.stringWidth(restartGameMsg)) / 2, (B_HEIGHT / 2) + 40);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Only used for human mode
        if (humanPlaying && snake.inGame) {
            snake.checkApple(scores);
            snake.move();
        }
        repaint();
    }

    private class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (humanPlaying) {
                if (key == KeyEvent.VK_LEFT && snake.direction.canMoveThere(Direction.LEFT))
                    snake.direction = Direction.LEFT;
                if (key == KeyEvent.VK_RIGHT && snake.direction.canMoveThere(Direction.RIGHT))
                    snake.direction = Direction.RIGHT;
                if (key == KeyEvent.VK_UP && snake.direction.canMoveThere(Direction.UP))
                    snake.direction = Direction.UP;
                if (key == KeyEvent.VK_DOWN && snake.direction.canMoveThere(Direction.DOWN))
                    snake.direction = Direction.DOWN;
                if (key == KeyEvent.VK_SPACE) {
                    scores.resetScores();
                    createHumanSnake();
                    timer.restart();
                    repaint();
                }
            } else {
                if (key == KeyEvent.VK_P) engine.setRunning(false);
                if (key == KeyEvent.VK_R) engine.setRunning(true);
                if (key == KeyEvent.VK_S) engine.toggleShowOnlyFirstSnake();
                if (key == KeyEvent.VK_D) engine.toggleDelay();
                if (key == KeyEvent.VK_V) engine.setVisualMode(!engine.isVisualMode());
                if (key == 107 || key == 61) { engine.increaseMutationRate(); scores.repaint(); }
                if (key == 109 || key == 45) { engine.decreaseMutationRate(); scores.repaint(); }
                if (key == 106) { engine.increaseSaveSnakeRatio(); scores.repaint(); }
                if (key == 111) { engine.decreaseSaveSnakeRatio(); scores.repaint(); }
                if (key == KeyEvent.VK_W) engine.saveToFile(Board.this);
                if (key == KeyEvent.VK_A) { engine.enableAutoSave(Board.this); scores.repaint(); }
                if (key == KeyEvent.VK_B) { engine.toggleBestOnly(); scores.repaint(); }
                if (key == KeyEvent.VK_L) engine.readFromFile(Board.this);
            }
        }
    }
}
```

**Step 6: Update SnakeGame to wire everything together**

In `src/main/java/pl/morph/ai/snake/page/SnakeGame.java`, update `initAIUI()`:

```java
private void initAIUI() {
    GridLayout gridLayout = new GridLayout(1, 2);
    JPanel mainLayout = new JPanel(gridLayout);
    Scores scores = new Scores();
    mainLayout.add(scores);

    // Create a placeholder board panel first
    FlowLayout boardLayout = new FlowLayout();
    JPanel boardContainer = new JPanel(boardLayout);

    // Create engine, then board with engine reference
    SimulationEngine engine = new SimulationEngine(scores, AISnakesCount, null);
    scores.setEngine(engine);
    Board board = new Board(scores, engine);
    engine.setBoardPanel(board);

    boardContainer.add(board);
    mainLayout.add(boardContainer);
    Container cnt = getContentPane();
    cnt.add(mainLayout, BorderLayout.CENTER);

    // Start the simulation engine
    engine.setRunning(true);
    Thread simThread = new Thread(engine::run, "SimulationEngine");
    simThread.setDaemon(true);
    simThread.start();
}
```

Add `import pl.morph.ai.snake.engine.SimulationEngine;` to SnakeGame.

**Step 7: Compile and verify**

Run: `mvn compile`
Expected: BUILD SUCCESS

**Step 8: Run existing tests**

Run: `mvn test`
Expected: All existing tests pass (Snake, NeuralNetwork, Matrix tests unchanged)

**Step 9: Commit**

```bash
git add -A
git commit -m "Extract SimulationEngine from Board, decouple simulation from EDT"
```

---

### Task 2: Add thread pool parallelization to SimulationEngine

Add `ExecutorService` to parallelize snake look/think/move within each tick.

**Files:**
- Modify: `src/main/java/pl/morph/ai/snake/engine/SimulationEngine.java`

**Step 1: Add the run() method with thread pool**

Add these fields and the `run()` method to `SimulationEngine`:

```java
import java.util.concurrent.*;

// Field
private ExecutorService executor;

public void setBoardPanel(JPanel boardPanel) {
    this.boardPanel = boardPanel;
}

public void run() {
    int cores = Runtime.getRuntime().availableProcessors();
    executor = Executors.newFixedThreadPool(cores);
    try {
        while (true) {
            if (!running) {
                Thread.sleep(50);
                continue;
            }
            simulateTickParallel();

            if (visualMode && boardPanel != null) {
                SwingUtilities.invokeLater(() -> {
                    boardPanel.repaint();
                    scores.repaint();
                });
                Thread.sleep(delay > 0 ? delay : 20);
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        executor.shutdownNow();
    }
}
```

**Step 2: Add the parallel tick method**

Replace `simulateTick()` with `simulateTickParallel()`:

```java
private void simulateTickParallel() {
    // Collect alive snakes
    List<Snake> alive = new ArrayList<>();
    int deadCount = 0;
    for (Snake snake : snakes) {
        if (snake.getScore() == ((B_HEIGHT / dotSize) * (B_WIDTH / dotSize)) - 1) {
            running = false;
            return;
        }
        if (snake.isBestSnake()) {
            scores.setScore((new Score()).setScore(snake.getScore()));
        }
        if (snake.inGame) {
            alive.add(snake);
        } else {
            deadCount++;
        }
    }

    // Parallel look/think/move
    if (!alive.isEmpty()) {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (final Snake s : alive) {
            tasks.add(new Callable<Void>() {
                public Void call() {
                    s.look();
                    s.think();
                    s.move();
                    return null;
                }
            });
        }
        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    // Update high score (single-threaded)
    for (Snake snake : alive) {
        if (snake.inGame && snake.getScore() > highScore) {
            highScore = snake.getScore();
            scores.setHighestScore(highScore);
            saveWaiting = true;
        }
    }

    // Recount dead after move
    deadCount = 0;
    for (Snake snake : snakes) {
        if (!snake.inGame) deadCount++;
    }

    if (deadCount >= aiSnakeCount || deadCount >= snakes.size()) {
        scores.resetScores();
        calculateFitness();
        naturalSelection();
        scores.setHighestFitness(bestFitness);
        System.out.println(scores.getGeneration() - 1 + "\t" + bestSnakeScore + ". " + bestFitness);
        if (autoSave && saveWaiting) {
            autoSave();
        }
        // Update scores on EDT
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scores.repaint();
            }
        });
    }
}
```

**Step 3: Remove the old single-threaded simulateTick()**

Delete the `simulateTick()` method since `simulateTickParallel()` replaces it.

**Step 4: Compile and verify**

Run: `mvn compile`
Expected: BUILD SUCCESS

**Step 5: Run existing tests**

Run: `mvn test`
Expected: All tests pass

**Step 6: Commit**

```bash
git add src/main/java/pl/morph/ai/snake/engine/SimulationEngine.java
git commit -m "Add thread pool parallelization for snake simulation"
```

---

### Task 3: Add headless/visual mode toggle info to Scores

**Files:**
- Modify: `src/main/java/pl/morph/ai/snake/page/Scores.java`

**Step 1: Add visual mode display and V key help text**

In `Scores.generationInfo()`, add after the existing stats:

```java
if (engine != null) {
    msg = "Mode: " + (engine.isVisualMode() ? "VISUAL" : "HEADLESS");
    g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 220);
}
```

In `Scores.info()`, add:

```java
msg = "Press 'v' to toggle headless/visual mode ";
g.drawString(msg, 20, 500);
```

**Step 2: Compile and verify**

Run: `mvn compile`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/pl/morph/ai/snake/page/Scores.java
git commit -m "Add headless/visual mode indicator to scores panel"
```

---

### Task 4: Manual integration test

**Step 1: Run the application**

Run: `mvn clean package && java -jar target/SnakeAi.jar`

**Step 2: Verify headless mode**

- App starts in headless mode (no snake rendering, generations print to console rapidly)
- CPU usage should be significantly higher than before (multiple cores)
- Console shows generation/score/fitness output

**Step 3: Verify visual mode toggle**

- Press `V` — snakes should appear and animate on screen
- Press `V` again — returns to headless, generations speed up

**Step 4: Verify all hotkeys work**

- `P` pauses, `R` resumes
- `S` toggles show only best snake
- `D` toggles delay
- `+`/`-` adjusts mutation rate (visible in scores panel)
- `W` opens save dialog
- `L` opens load dialog
- `B` toggles best-only breeding

**Step 5: Verify generation progression**

- Let it run for 50+ generations
- Scores should increase over time (same learning behavior as before)

**Step 6: Commit CLAUDE.md update**

Update CLAUDE.md to document the new `V` key for headless/visual toggle and the SimulationEngine architecture. Commit.

```bash
git add CLAUDE.md
git commit -m "Update CLAUDE.md with SimulationEngine architecture and V key docs"
```
