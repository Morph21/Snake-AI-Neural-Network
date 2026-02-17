package pl.morph.ai.snake.engine;

import pl.morph.ai.snake.element.*;
import pl.morph.ai.snake.page.Scores;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class SimulationEngine implements Runnable {

    public static final int B_WIDTH = 800;
    public static final int B_HEIGHT = 800;
    private static final int ELITISM_COUNT = 5;
    private static final int TOURNAMENT_SIZE = 5;

    private volatile List<Snake> snakes;
    private Snake bestSnake;
    private int bestSnakeScore;
    private int highScore = 0;
    private double bestFitness;
    private double fitnessSum;
    private int aiSnakeCount;
    private List<Wall> walls;
    private volatile boolean showOnlyFirstSnake = false;
    private volatile int delay = 0;
    private int dotSize = 40;
    private String savePath = System.getProperty("user.dir");
    private volatile boolean saveWaiting = false;
    private volatile String fileName = null;

    // GA parameters (volatile for cross-thread visibility)
    private volatile double mutationRate = 0.03;
    private volatile double crossoverRate = 0.9;
    private volatile double saveSnakeRatio = 0.5;
    private volatile double avgFitness = 0;
    private volatile boolean autoSave = false;
    private volatile boolean bestOnly = false;

    // Volatile flags
    private volatile boolean running;
    private volatile boolean visualMode = true;

    private ExecutorService executor;

    private JPanel boardPanel;
    private Scores scores;

    public SimulationEngine(Scores scores, int aiSnakeCount, JPanel boardPanel) {
        this.scores = scores;
        this.aiSnakeCount = aiSnakeCount;
        this.boardPanel = boardPanel;
        this.walls = WallManager.prepareWalls(dotSize, B_WIDTH, B_HEIGHT);
        createAISnakes();
    }

    private void createAISnakes() {
        List<Snake> newSnakes = new ArrayList<Snake>();

        for (int i = 0; i < aiSnakeCount; i++) {
            Snake snake = new Snake(B_WIDTH, B_HEIGHT, delay, false, null, dotSize, walls);
            snake.setMutationRate(mutationRate);
            placeSnakeRandomly(snake);
            if (showOnlyFirstSnake) {
                if (newSnakes.size() == 0) {
                    snake.setShowIt(true);
                }
            } else {
                snake.setShowIt(true);
            }
            snake.spawnApple();
            newSnakes.add(snake);
        }
        snakes = newSnakes;
    }

    public void placeSnakeRandomly(Snake snake) {
        List<Snake.XY> boardList = snake.populate(B_WIDTH / dotSize, B_HEIGHT / dotSize);
        int index = (int) (Math.random() * boardList.size());
        Snake.XY xy = boardList.get(index);
        snake.setStartingPosition(xy);
    }

    private void simulateTickParallel() throws InterruptedException {
        scores.setDeadSnakes(0);

        // Collect alive snakes for parallel processing
        final List<Snake> currentSnakes = snakes;
        List<Snake> aliveSnakes = new ArrayList<Snake>();
        for (Snake snake : currentSnakes) {
            if (snake.getScore() == ((B_HEIGHT / dotSize) * (B_WIDTH / dotSize)) - 1) {
                running = false;
            }
            if (snake.isBestSnake()) {
                scores.setScore((new Score()).setScore(snake.getScore()));
            }
            if (snake.inGame) {
                aliveSnakes.add(snake);
            } else {
                scores.incrementDeadSnakes();
            }
        }

        // Create parallel tasks for each alive snake
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
        for (final Snake snake : aliveSnakes) {
            tasks.add(new Callable<Void>() {
                public Void call() {
                    snake.look();
                    snake.think();
                    snake.move();
                    return null;
                }
            });
        }

        // Execute all tasks in parallel and wait for completion
        if (!tasks.isEmpty()) {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < futures.size(); i++) {
                try {
                    futures.get(i).get();
                } catch (ExecutionException e) {
                    System.err.println("Snake task failed: " + e.getCause());
                    e.getCause().printStackTrace();
                    aliveSnakes.get(i).inGame = false;
                }
            }
        }

        // Count snakes that died during parallel tick
        for (Snake snake : aliveSnakes) {
            if (!snake.inGame) {
                scores.incrementDeadSnakes();
            }
        }

        // Single-threaded: update high score
        for (Snake snake : aliveSnakes) {
            if (snake.inGame && snake.getScore() > highScore) {
                highScore = snake.getScore();
                scores.setHighestScore(highScore);
                saveWaiting = true;
            }
        }

        // Single-threaded: check generation end
        if (scores.getDeadSnakes() >= aiSnakeCount || scores.getDeadSnakes() >= currentSnakes.size()) {
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

    private void calculateFitness() {
        for (Snake snake : snakes) {
            snake.calculateFitness();
        }
    }

    private void naturalSelection() {
        List<Snake> newSnakes = new ArrayList<Snake>();

        Collections.sort(snakes, new Comparator<Snake>() {
            public int compare(Snake a, Snake b) {
                return Double.compare(b.getFitness(), a.getFitness());
            }
        });
        setBestSnake();
        calculateFitnessSum();

        // Add best snake clone for replay (preserves foodList for replay)
        Snake best = bestSnake.cloneForReplay();
        best.setBestSnake(true);
        best.setScores(scores);
        best.setShowIt(true);
        best.spawnApple();
        newSnakes.add(best);

        // Elitism: carry top snakes unmodified (no mutation) into next generation
        for (int i = 0; i < Math.min(ELITISM_COUNT, snakes.size()) && newSnakes.size() < aiSnakeCount; i++) {
            Snake elite = snakes.get(i);
            NeuralNetwork brain = elite.getBrain().clone();
            Snake eliteSnake = new Snake(B_WIDTH, B_HEIGHT, delay, false, null, dotSize, walls);
            eliteSnake.setMutationRate(mutationRate);
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

        // Fill rest with crossover + mutation
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
            child.setMutationRate(mutationRate);
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

    private void calculateFitnessSum() {
        fitnessSum = 0;
        for (Snake snake : snakes) {
            fitnessSum += snake.getFitness();
        }
        avgFitness = fitnessSum / snakes.size();
    }

    private Snake tournamentSelect() {
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

    private void setBestSnake() {
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

        // Prefer higher score; use fitness as tiebreaker
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

    public void saveToFile(JPanel parent) {
        try {
            final JFileChooser fc = new JFileChooser(savePath);
            fc.setFileFilter(new FileNameExtensionFilter("*.ser", "ser"));
            int returnVal = fc.showSaveDialog(parent);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                fileName = file.getAbsolutePath();
                try (FileOutputStream fout = new FileOutputStream(fileName);
                     ObjectOutputStream oos = new ObjectOutputStream(fout)) {
                    List<Snake> toSave = new ArrayList<Snake>(snakes);
                    Collections.sort(toSave, new Comparator<Snake>() {
                        public int compare(Snake a, Snake b) {
                            return Double.compare(b.getFitness(), a.getFitness());
                        }
                    });
                    if (toSave.size() > 1000) {
                        oos.writeObject(new ArrayList<Snake>(toSave.subList(0, 1000)));
                    } else {
                        oos.writeObject(toSave);
                    }
                }
            } else {
                System.out.println("Open command cancelled by user.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    int snakeDotSize = snake.getDotSize();
                    snake.setDotSize(dotSize);
                    snake.setWalls(walls);

                    List<Apple> foodList = snake.getFoodList();
                    int diff;
                    boolean lesser;
                    if (snakeDotSize > dotSize) {
                        diff = snakeDotSize / dotSize;
                        lesser = false;
                    } else {
                        diff = dotSize / snakeDotSize;
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
                if (boardPanel != null) {
                    boardPanel.repaint();
                }
            } else {
                System.out.println("Open command cancelled by user.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shiftApple(Apple apple, boolean lesser, int diff) {
        int apple_x = apple.getApple_x();
        int apple_y = apple.getApple_y();
        if (apple_x != 0) {
            if (lesser)
                apple_x *= diff;
            else
                apple_x /= diff;
        }

        if (apple_y != 0) {
            if (lesser)
                apple_y *= diff;
            else
                apple_y /= diff;
        }
        apple.setApple_x(apple_x);
        apple.setApple_y(apple_y);
    }

    public void run() {
        int cores = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(cores);
        try {
            while (true) {
                if (!running) {
                    try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                    continue;
                }
                simulateTickParallel();
                if (visualMode && boardPanel != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            boardPanel.repaint();
                            scores.repaint();
                        }
                    });
                    try { Thread.sleep(delay > 0 ? delay : 20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
    }

    // Accessor methods
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
        List<Snake> currentSnakes = snakes;
        if (currentSnakes != null) {
            for (Snake snake : currentSnakes) {
                if (showOnlyFirstSnake) {
                    if (snake.isBestSnake()) {
                        snake.setShowIt(true);
                    } else {
                        snake.setShowIt(false);
                    }
                } else {
                    snake.setShowIt(true);
                }
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
        if (mutationRate > 1) {
            mutationRate = 1;
        }
    }

    public void decreaseMutationRate() {
        mutationRate /= 2;
    }

    public void increaseSaveSnakeRatio() {
        saveSnakeRatio += 0.1;
        if (saveSnakeRatio > 1) {
            saveSnakeRatio = 1;
        }
    }

    public void decreaseSaveSnakeRatio() {
        saveSnakeRatio -= 0.1;
        if (saveSnakeRatio < 0.1) {
            saveSnakeRatio = 0.1;
        }
    }

    public void toggleBestOnly() {
        bestOnly = !bestOnly;
    }

    public void enableAutoSave(JPanel parent) {
        autoSave = true;
        saveToFile(parent);
    }

    public void setBoardPanel(JPanel boardPanel) {
        this.boardPanel = boardPanel;
    }

    public double getMutationRate() {
        return mutationRate;
    }

    public double getCrossoverRate() {
        return crossoverRate;
    }

    public double getSaveSnakeRatio() {
        return saveSnakeRatio;
    }

    public double getAvgFitness() {
        return avgFitness;
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    public boolean isBestOnly() {
        return bestOnly;
    }
}
