package pl.morph.ai.snake.page;

import pl.morph.ai.snake.element.*;
import pl.morph.ai.snake.engine.SimulationEngine;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

public class Board extends JPanel implements ActionListener {

    public static final int B_WIDTH = SimulationEngine.B_WIDTH;
    public static final int B_HEIGHT = SimulationEngine.B_HEIGHT;

    private int DOT_SIZE = 40;

    private Snake snake;
    private List<Wall> humanWalls;
    private boolean humanPlaying;

    private Scores scores;
    private SimulationEngine engine;

    private Timer timer;

    // AI mode constructor
    public Board(Scores scores, SimulationEngine engine) {
        this.humanPlaying = false;
        this.scores = scores;
        this.engine = engine;

        setBackground(Color.black);
        setFocusable(true);
        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));
        addKeyListener(new TAdapter());
    }

    // Human mode constructor
    public Board(Scores scores) {
        this.humanPlaying = true;
        this.scores = scores;
        this.engine = null;

        setBackground(Color.black);
        setFocusable(true);
        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));
        addKeyListener(new TAdapter());

        createHumanSnake();

        timer = new Timer(60, this);
    }

    private void createHumanSnake() {
        if (humanWalls == null) {
            humanWalls = WallManager.prepareWalls(DOT_SIZE, B_WIDTH, B_HEIGHT);
        }
        this.snake = new Snake(B_WIDTH, B_HEIGHT, 60, true, null, DOT_SIZE, humanWalls);
        this.snake.setShowIt(true);
        placeSnakeRandomly(this.snake);
        snake.spawnApple();
    }

    private void placeSnakeRandomly(Snake snake) {
        List<Snake.XY> boardList = snake.populate(B_WIDTH / DOT_SIZE, B_HEIGHT / DOT_SIZE);
        int index = (int) (Math.random() * boardList.size());
        Snake.XY xy = boardList.get(index);
        snake.setStartingPosition(xy);
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
            if (humanWalls != null && !humanWalls.isEmpty()) {
                for (Wall wall : humanWalls) {
                    wall.doDrawing(g);
                }
            }
        } else {
            // AI mode: draw snakes from engine
            List<Snake> snakes = engine.getSnakes();
            if (snakes != null) {
                for (Snake snake : snakes) {
                    if (snake.inGame || snake.isBestSnake()) {
                        snake.doDrawing(g);
                    }
                }
            }

            List<Wall> walls = engine.getWalls();
            if (walls != null && !walls.isEmpty()) {
                for (Wall wall : walls) {
                    wall.doDrawing(g);
                }
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
        // Human mode only
        if (humanPlaying) {
            if (snake.inGame) {
                snake.checkApple(scores);
                snake.move();
            }
            repaint();
        }
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
                    scores.resetScores();
                    createHumanSnake();
                    if (timer != null) {
                        timer.stop();
                    }
                    timer = new Timer(60, Board.this);
                    timer.start();
                    repaint();
                }
            } else {
                // AI mode: delegate to engine
                if (key == KeyEvent.VK_P) {
                    engine.setRunning(false);
                }

                if (key == KeyEvent.VK_R) {
                    engine.setRunning(true);
                }

                if (key == KeyEvent.VK_S) {
                    engine.toggleShowOnlyFirstSnake();
                }

                if (key == KeyEvent.VK_D) {
                    engine.toggleDelay();
                }

                if (key == KeyEvent.VK_V) {
                    engine.setVisualMode(!engine.isVisualMode());
                }

                if (key == 107 || key == 61) {
                    engine.increaseMutationRate();
                }

                if (key == 109 || key == 45) {
                    engine.decreaseMutationRate();
                }

                if (key == 106) {
                    engine.increaseSaveSnakeRatio();
                }

                if (key == 111) {
                    engine.decreaseSaveSnakeRatio();
                }

                if (key == KeyEvent.VK_W) {
                    engine.saveToFile(Board.this);
                }

                if (key == KeyEvent.VK_A) {
                    engine.enableAutoSave(Board.this);
                }

                if (key == KeyEvent.VK_B) {
                    engine.toggleBestOnly();
                }

                if (key == KeyEvent.VK_L) {
                    engine.readFromFile(Board.this);
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
