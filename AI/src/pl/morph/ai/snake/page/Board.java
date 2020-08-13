package pl.morph.ai.snake.page;

import pl.morph.ai.snake.element.Direction;
import pl.morph.ai.snake.element.Score;
import pl.morph.ai.snake.element.Snake;
import pl.morph.ai.snake.engine.NeuralNetwork;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Board extends JPanel implements ActionListener {

    private final int B_WIDTH = 300;
    private final int B_HEIGHT = 300;

    //Speed of snake
    private int DELAY = 1;

    private Snake snake;
    private List<Snake> snakes;
    private int highScore = 0;

    private boolean humanPlaying;

    private boolean resetByPressingSpace;
    private int AISnakes;

    private Scores scores;

    private Timer timer;

    public Board(Scores scores, boolean humanPlaying, Integer AINumber) {
        this.humanPlaying = humanPlaying;
        this.scores = scores;
        if (humanPlaying) {
            initBoard(null);
            DELAY = 80;
        } else {
            initBoard(AINumber);
            AISnakes = AINumber;
            DELAY = 0;
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
            createHumanSnake();
        } else {
            createAISnakes(AINumber);
        }

        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void initGameWithBrains(Integer AINumber, List<NeuralNetwork> brains) {
        resetByPressingSpace = false;

        setBackground(Color.black);
        setFocusable(true);

        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));

        createAISnakesWithBrains(AINumber, brains);

        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void createHumanSnake() {
        this.snake = new Snake(B_WIDTH, B_HEIGHT, DELAY, humanPlaying);

        for (int z = 0; z < snake.getLength(); z++) {
            snake.getX()[z] = 50 - z * 10;
            snake.getY()[z] = 50;
        }

        snake.spawnApple();
    }

    private void createAISnakes(int AINumber) {
        snakes = new ArrayList<>();

        for (int i = 0; i < AINumber; i++) {

            this.snake = new Snake(B_WIDTH, B_HEIGHT, DELAY, humanPlaying);

            placeSnakeOnMiddle();

            snake.spawnApple();
            snakes.add(snake);
        }
    }

    private void createAISnakesWithBrains(int AINumber, List<NeuralNetwork> brains) {
        snakes = new ArrayList<>();

        for (int i = 0; i < brains.size(); i++) {
            if (snakes.size() < AINumber) {
                NeuralNetwork neuralNetwork = brains.get(0);
                this.snake = new Snake(B_WIDTH, B_HEIGHT, DELAY, humanPlaying);
                placeSnakeOnMiddle();
                snake.setBrain(neuralNetwork);
                snake.spawnApple();
                snakes.add(snake);

            }
        }
        while (snakes.size() < AINumber) {
            this.snake = new Snake(B_WIDTH, B_HEIGHT, DELAY, humanPlaying);

            placeSnakeOnMiddle();

            snake.spawnApple();
            snakes.add(snake);
        }
    }

    public void placeSnakeOnMiddle() {
        for (int z = 0; z < snake.getLength(); z++) {
            snake.getX()[z] = B_WIDTH/2 - z * 10;
            snake.getY()[z] = B_HEIGHT/2;
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
                if (snake.inGame) {
                    snake.doDrawing(g);
                }
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
                snake.checkCollision();
                snake.move();
            }
        } else {
            int deadSnakes = 0;
            for (Snake snake : snakes) {
                if (snake.inGame) {
                    snake.checkApple();
                    snake.checkCollision();
                    try {
                        snake.think();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    if (snake.getScore() > highScore) {
                        highScore = snake.getScore();
                        scores.setHighestScore(highScore);
                    }

                    if (snake.getFitness() > scores.getHighestFitness()) {
                        scores.setHighestFitness(snake.getFitness());
                    }
                    snake.move();
                } else {
                    deadSnakes++;
                }
            }
            if (deadSnakes == AISnakes) {
                scores.resetScores();
                scores.increseGeneration();
                List<Snake> collected = snakes.stream()
                                              .sorted(Comparator.comparingDouble(Snake::getFitness).reversed())
                                              .collect(Collectors.toList());
                List<Snake> bestSnakes = new ArrayList<>();
                for (int i = 0; i < AISnakes * 0.90; i++) {
                    bestSnakes.add(collected.get(i));
                }

                List<NeuralNetwork> networks = new ArrayList<>();
                for (Snake snake : bestSnakes) {
                    List<double[]> inputs = snake.getInputs();
                    double[][] in = new double[inputs.size()][4];
                    for (var i = 0; i < inputs.size(); i++) {
                        in[0] = inputs.get(i);
                    }
                    List<double[]> outputs = snake.getOutputs();
                    double[][] out = new double[outputs.size()][4];
                    for (var i = 0; i < outputs.size(); i++) {
                        out[0] = outputs.get(i);
                    }
                    snake.getBrain().fit(in, out, 10);
                    networks.add(snake.getBrain());
                }

                initGameWithBrains(AISnakes, networks);
            }
        }

        repaint();
    }

    private class TAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {

            int key = e.getKeyCode();

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
}

