package pl.morph.ai.snake.page;

import pl.morph.ai.snake.element.*;
import pl.morph.ai.snake.engine.Matrix;
import pl.morph.ai.snake.engine.NeuralNetwork;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import java.util.List;

public class Board extends JPanel implements ActionListener {

    public final static int B_WIDTH = 800;
    public final static int B_HEIGHT = 800;

    //Speed of pl.morph.ai.snake
    private int DELAY = 0;
    //Size of pl.morph.ai.snake
    private int DOT_SIZE = 10;

    private String SAVE_PATH = "C:\\repo\\github\\AI";

    private Snake snake;
    private Snake bestSnake;
    private int bestSnakeScore;
    private List<Snake> snakes;
    private int highScore = 0;
    private double bestFitness;
    private float fitnessSum;
    private int samebest = 0;
    public static double MUTATION_RATE = 0.03;

    public static double CROSSOVER_RATE = 0.9;
    public static double SAVE_SNAKE_RATIO = 0.5;
    public static double avgFitness = 0;

    private boolean humanPlaying;
    private boolean showOnlyFirstSnake = false;

    private boolean resetByPressingSpace;
    private int AISnakes;
    public static boolean autoSave = false;
    public static boolean bestOnly = false;
    private boolean saveWaiting = false;
    private String fileName = null;

    private List<Wall> walls;

    private Scores scores;

    public static Timer timer;

    public Board(Scores scores, boolean humanPlaying, Integer AINumber) {
        this.humanPlaying = humanPlaying;
        this.scores = scores;
        this.walls = WallManager.prepareWalls(DOT_SIZE);
        if (humanPlaying) {
            initBoard(null);
        } else {
            initBoard(AINumber);
            AISnakes = AINumber;
        }
    }

    private void initBoard(Integer AINumber) {

        addKeyListener(new TAdapter());
        initGame(AINumber);
    }

    private void initGame(Integer AINumber) {
        resetByPressingSpace = false;

        setBackground(Color.black);
        setFocusable(true);
        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));

        if (humanPlaying) {
            DELAY = 60;
            createHumanSnake();
        } else {
            DELAY = 0;
            createAISnakes(AINumber);
        }

        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(DELAY, this);
//        timer.start();
    }

    private void createHumanSnake() {
        this.snake = new Snake(B_WIDTH, B_HEIGHT, DELAY, humanPlaying, null, DOT_SIZE, walls);
        this.snake.setShowIt(true);
        placeSnakeOnMiddle(this.snake);

        snake.spawnApple();
    }

    private void createAISnakes(int AINumber) {
        snakes = new ArrayList<>();

        for (int i = 0; i < AINumber; i++) {

            this.snake = new Snake(B_WIDTH, B_HEIGHT, DELAY, humanPlaying, null, DOT_SIZE, walls);
            placeSnakeOnMiddle();
            if (showOnlyFirstSnake) {
                if (snakes.size() == 0) {
                    this.snake.setShowIt(true);
                }
            } else {
                this.snake.setShowIt(true);
            }
            snake.spawnApple();
            snakes.add(snake);
        }
    }

    public void placeSnakeOnMiddle() {
        for (int z = 0; z < snake.getLength(); z++) {
            snake.getX()[z] = B_WIDTH / 2 - z * DOT_SIZE;
            snake.getY()[z] = B_HEIGHT / 2;
        }
    }

    public void placeSnakeOnMiddle(Snake snake) {
        for (int z = 0; z < snake.getLength(); z++) {
            snake.getX()[z] = B_WIDTH / 2 - z * DOT_SIZE;
            snake.getY()[z] = B_HEIGHT / 2;
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
            for (Snake snake : snakes) {
                if (snake.inGame || snake.isBestSnake()) {
                    snake.doDrawing(g);
                }
            }
        }

        if (walls != null && !walls.isEmpty()) {
            for (Wall wall : walls) {
                wall.doDrawing(g);
            }
        }
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
        if (humanPlaying) {
            if (snake.inGame) {
                snake.checkApple(scores);
//                pl.morph.ai.snake.checkCollision();
                snake.move();
            }
        } else {
            scores.setDeadSnakes(0);
            snakes.parallelStream().forEach(snake -> {

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

                    if (snake.getFitness() > scores.getHighestFitness()) {
                        scores.setHighestFitness(snake.getFitness());
                    }
                } else {
                    scores.incrementDeadSnakes();
                }
            });
//            for (Snake pl.morph.ai.snake : snakes) {
//            }

            scores.repaint();
            if (scores.getDeadSnakes() >= AISnakes || scores.getDeadSnakes() >= snakes.size()) {
                timer.stop();
                scores.resetScores();
                calculateFitness();
                naturalSelection();
                scores.setHighestFitness(bestFitness);
                System.out.println(scores.getGeneration()-1 + "\t" + bestSnakeScore + ". " + bestFitness);
                if (autoSave && (saveWaiting)) {
                    autoSave();
                }
                timer.restart();

            }
        }

        repaint();
    }

    void calculateFitness() {  //calculate the fitnesses for each pl.morph.ai.snake
        snakes.parallelStream().forEach(snake -> {
            snake.calculateFitness();
        });
//        for (Snake pl.morph.ai.snake : snakes) {
//        }
    }

    void naturalSelection() {
        List<Snake> newSnakes = new ArrayList<>();

        snakes.sort(Comparator.comparingDouble(Snake::getFitness).reversed());
        setBestSnake();
        calculateFitnessSum();

        Snake best = bestSnake.cloneForReplay();
        best.setBestSnake(true);
        best.setScores(scores);
        best.setShowIt(true);
        best.spawnApple();
        placeSnakeOnMiddle(best);
        int mutations = 0;

        newSnakes.add(best);  //add the best 9snake of the prior generation into the new generation
        found = 0;

        while (newSnakes.size() != AISnakes) {
            for (int i = 0; i < (snakes.size() * SAVE_SNAKE_RATIO); i++) {
                Snake child = selectParent();
                Snake parent = selectParent();

                Snake snakey = parent.cloneThis();
                if (i == 0) {
                    snakey = parent.cloneThis(); // dont crossover first one
                } else {
                    double rand = Matrix.random(0,1);
                    if (rand < CROSSOVER_RATE) {
                        snakey = child.crossover(parent);
                    }
                }


                snakey.mutate();

                NeuralNetwork brain = snakey.getBrain();
                snakey = new Snake(B_WIDTH, B_HEIGHT, DELAY, humanPlaying, null, DOT_SIZE, walls);
                snakey.setBrain(brain);
                snakey.setBestSnake(false);
                snakey.setScores(null);
                placeSnakeOnMiddle(snakey);
                if (showOnlyFirstSnake) {
                    if (newSnakes.size() == 0) {
                        snakey.setShowIt(true);
                    }
                } else {
                    if (i < 11) {
                        snakey.setShowIt(true);
                    }
                }
                snakey.spawnApple();
                newSnakes.add(i, snakey);
                if (newSnakes.size() == AISnakes) {
                    break;
                }
            }
        }
        snakes = null;
        snakes = newSnakes;
        scores.increseGeneration();
    }

    void calculateFitnessSum() {  //calculate the sum of all the snakes fitnesses
        fitnessSum = 0;
        for (int i = 0; i < snakes.size() * SAVE_SNAKE_RATIO; i++) {
            snakes.get(i).calculateFitness();
            fitnessSum += snakes.get(i).getFitness();
        }

        avgFitness = fitnessSum / snakes.stream().count();
    }

    int found = 0;

//    Snake selectParent(boolean child) {  //selects a random number in range of the fitnesssum and if a pl.morph.ai.snake falls in that range then select it
//        double rand = Matrix.random(0, fitnessSum);
//        double summation = 0;
//        if (bestOnly && !child) {
//            return snakes.get(0);
//        }
//        for (int i = 0; i < snakes.size() * SAVE_SNAKE_RATIO; i++) {
//            summation += snakes.get(i).getFitness();
//            if (summation > rand) {
//                found++;
//                return snakes.get(i);
//            }
//        }
//        return snakes.get(0);
//    }

    Snake selectParent() {  //selects a random number in range of the fitnesssum and if a pl.morph.ai.snake falls in that range then select it
        double rand = Matrix.random(0, fitnessSum);
        double summation = 0;
        if (bestOnly) {
            return snakes.get(0);
        }
        if (scores.getGeneration() % 100 == 0) {
            return snakes.get(0);
        }
        for (int i = 0; i < snakes.size() * SAVE_SNAKE_RATIO; i++) {
            summation += snakes.get(i).getFitness();
            if (summation > rand) {
                found++;
                return snakes.get(i);
            }
        }
        return snakes.get(0);
    }

    void setBestSnake() {  //set the best pl.morph.ai.snake of the generation
        double max = 0;
        int maxIndex = 0;
        for (int i = 0; i < snakes.size(); i++) {
            Snake snake = snakes.get(i);
            if (snake.getFitness() > max) {
                max = snake.getFitness();
                maxIndex = i;
            }
        }
        if (max > bestFitness) {
            bestFitness = max;
            bestSnake = snakes.get(maxIndex).cloneForReplay();
            bestSnakeScore = snakes.get(maxIndex).getScore();
        } else {
            bestSnake = bestSnake.cloneForReplay();

        }
    }

    private class TAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {

            int key = e.getKeyCode();
            if (key == KeyEvent.VK_P) {
                timer.stop();
            }

            if (key == KeyEvent.VK_R) {
                timer.start();
            }

            if (humanPlaying) {
                if (key == KeyEvent.VK_LEFT) {
                    goLeft();
                }

                if (key == KeyEvent.VK_RIGHT) {
                    goRight();
                }

                if (key == KeyEvent.VK_UP) {
                    goUp();
                }

                if (key == KeyEvent.VK_DOWN) {
                    goDown();
                }
                if (key == KeyEvent.VK_SPACE) {
                    resetByPressingSpace = false;
                    scores.resetScores();
                    initGame(null);
                    repaint();
                }
            } else {
                if (key == KeyEvent.VK_S) {
                    showOnlyFirstSnake = !showOnlyFirstSnake;
                    int i = 0;
                    for (Snake snake : snakes) {
                        if (showOnlyFirstSnake) {
                            if (snake.isBestSnake()) {
                                snake.setShowIt(true);
                            } else {
                                snake.setShowIt(false);
                            }
                            i++;
                        } else {
                            if (i < 11) {


                            snake.setShowIt(true);
                            i++;
                            }
                        }
                    }
                }

                if (key == KeyEvent.VK_D) {
                    if (DELAY == 0) {
                        DELAY = 20;
                    } else {
                        DELAY = 0;
                    }
                    timer.setDelay(DELAY);
                }

                if (key == 107 || key == 61) {
                    MUTATION_RATE *= 2;
                    if (MUTATION_RATE > 1) {
                        MUTATION_RATE = 1;
                    }
                    scores.repaint();
                }

                if (key == 109 || key == 45) {
                    MUTATION_RATE /= 2;
                    scores.repaint();
                }

                if (key == 106) {
                    SAVE_SNAKE_RATIO += 0.1;
                    if (SAVE_SNAKE_RATIO > 1) {
                        SAVE_SNAKE_RATIO = 1;
                    }
                    scores.repaint();
                }

                if (key == 111) {
                    SAVE_SNAKE_RATIO -= 0.1;
                    if (SAVE_SNAKE_RATIO <= 0) {
                        SAVE_SNAKE_RATIO = 0.01;
                    }
                    scores.repaint();
                }

                if (key == KeyEvent.VK_W) {
                    saveToFile();
                }

                if (key == KeyEvent.VK_A) {
                    autoSave = true;
                    saveToFile();
                    scores.repaint();
                }

                if (key == KeyEvent.VK_B) {
                    bestOnly = !bestOnly;
                    scores.repaint();
                }

                if (key == KeyEvent.VK_L) {
                    readFromFile();
                }


            }

        }
    }

    private void goLeft() {
        if (snake.direction.canMoveThere(Direction.LEFT)) {
            snake.direction = Direction.LEFT;
        }
    }

    private void goRight() {
        if (snake.direction.canMoveThere(Direction.RIGHT)) {
            snake.direction = Direction.RIGHT;
        }
    }

    private void goUp() {
        if (snake.direction.canMoveThere(Direction.UP)) {
            snake.direction = Direction.UP;
        }
    }

    private void goDown() {
        if (snake.direction.canMoveThere(Direction.DOWN)) {
            snake.direction = Direction.DOWN;
        }
    }

    private void autoSave() {
        try {
            if (fileName != null && !fileName.isEmpty()) {
                FileOutputStream fout = new FileOutputStream(fileName);
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeObject(snakes);
                saveWaiting = false;
                System.out.println("Auto save triggered");
            }
        } catch (Exception e) {
            System.out.println("AutoSave failed");
            e.printStackTrace();
        }
    }

    private void saveToFile() {
        try {
            final JFileChooser fc = new JFileChooser(SAVE_PATH);
            fc.setFileFilter(new FileNameExtensionFilter("*.ser", "ser"));
            int returnVal = fc.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                //This is where a real application would open the file.
                fileName = file.getName();
                FileOutputStream fout = new FileOutputStream(fileName);
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                snakes.sort(Comparator.comparingDouble(Snake::getFitness).reversed());
                if (snakes.size() > 1000) {
                    List<Snake> smallerList = new ArrayList<>();
                    int i = 0;
                    for (Snake snake : snakes) {
                        if (i >= 1000) {
                            break;
                        }
                        smallerList.add(snake);
                        i++;
                    }
                    oos.writeObject(smallerList);
                } else {
                    oos.writeObject(snakes);
                }
            } else {
                System.out.println(("Open command cancelled by user."));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readFromFile() {
        try {
            final JFileChooser fc = new JFileChooser(SAVE_PATH);
            fc.setFileFilter(new FileNameExtensionFilter("*.ser", "ser"));
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                //This is where a real application would open the file.
                FileInputStream streamIn = new FileInputStream(file.getName());
                ObjectInputStream objectinputstream = new ObjectInputStream(streamIn);
                this.snakes = null;
                List<Snake> readCase = (List<Snake>) objectinputstream.readObject();
                System.out.println("Snakes loaded");

                for (Snake snake : readCase) {
                    int dotSize = snake.getDotSize();
                    //if (dotSize != DOT_SIZE) {
                        snake.setDotSize(DOT_SIZE);
                        snake.setWalls(walls);

                        List<Apple> foodList = snake.getFoodList();
                        int diff;
                        boolean lesser;
                        if (dotSize > DOT_SIZE) {
                            diff = dotSize / DOT_SIZE;
                            lesser = false;
                        } else {
                            diff = DOT_SIZE / dotSize;
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

                    //}
                }

                snakes = readCase;
                repaint();
            } else {
                System.out.println(("Open command cancelled by user."));
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
}

