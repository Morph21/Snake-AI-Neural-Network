package pl.morph.ai.snake.page;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    public Main() {

        initUI();
    }

    //Set to false if you want AI to play the game
    private final boolean humanPlaying = false;
    private final int AISnakesCount = 6000;

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
        ctrl.add(new Board(scores, humanPlaying, null));
        Container cnt = getContentPane();

        cnt.add(ctrl, BorderLayout.CENTER);
    }

    private void initAIUI() {
        GridLayout gridLayout = new GridLayout(1, 2);
        JPanel mainLayout = new JPanel(gridLayout);
        Scores scores = new Scores();
        mainLayout.add(scores);

        FlowLayout boardLayout = new FlowLayout();

        JPanel board = new JPanel(boardLayout);
        board.add(new Board(scores, humanPlaying, AISnakesCount));

        mainLayout.add(board);
        Container cnt = getContentPane();

        cnt.add(mainLayout, BorderLayout.CENTER);
    }

    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {
            JFrame ex = new Main();
            ex.setVisible(true);
        });
    }
}