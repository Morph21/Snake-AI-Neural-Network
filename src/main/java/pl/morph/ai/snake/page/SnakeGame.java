package pl.morph.ai.snake.page;

import pl.morph.ai.snake.engine.SimulationEngine;

import javax.swing.*;
import java.awt.*;

public class SnakeGame extends JFrame {

    public SnakeGame() {

        initUI();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                JFrame ex = new SnakeGame();
                ex.setVisible(true);
            }
        });
    }

    //Set to false if you want AI to play the game
    private final boolean humanPlaying = false;
    private final int AISnakesCount = 200;

    private void initUI() {
        if (humanPlaying) {
            initHumanUI();
        } else {
            initAIUI();
        }

        setResizable(false);
        pack();


        setTitle("Snake");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initHumanUI() {
        GridLayout gridLayout = new GridLayout(1, 2);
        JPanel ctrl = new JPanel(gridLayout);
        Scores scores = new Scores();
        ctrl.add(scores);
        ctrl.add(new Board(scores));
        Container cnt = getContentPane();

        cnt.add(ctrl, BorderLayout.CENTER);
    }

    private void initAIUI() {
        GridLayout gridLayout = new GridLayout(1, 2);
        JPanel mainLayout = new JPanel(gridLayout);
        Scores scores = new Scores();
        mainLayout.add(scores);

        FlowLayout boardLayout = new FlowLayout();
        JPanel boardContainer = new JPanel(boardLayout);

        SimulationEngine engine = new SimulationEngine(scores, AISnakesCount, null);
        scores.setEngine(engine);

        Board board = new Board(scores, engine);
        engine.setBoardPanel(board);

        boardContainer.add(board);
        mainLayout.add(boardContainer);
        Container cnt = getContentPane();

        cnt.add(mainLayout, BorderLayout.CENTER);

        // Start simulation on a daemon thread
        engine.setRunning(true);
        Thread simThread = new Thread(engine, "SimulationEngine");
        simThread.setDaemon(true);
        simThread.start();
    }

}
