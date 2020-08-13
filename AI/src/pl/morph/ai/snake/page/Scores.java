package pl.morph.ai.snake.page;

import pl.morph.ai.snake.element.Score;

import javax.swing.*;
import java.awt.*;

public class Scores extends JPanel {
    private final int SCORES_WIDTH = 300;
    private final int SCORES_HEIGHT = 100;

    private Score score;
    private Score highScore;
    private double highestFitness = 0;
    private int generation = 1;

    public Scores() {
        init();
    }

    private void init() {
        setBackground(Color.gray);
        setPreferredSize(new Dimension(SCORES_WIDTH, SCORES_HEIGHT));
        setSize(new Dimension(SCORES_WIDTH, SCORES_HEIGHT));
        setMaximumSize(new Dimension(SCORES_WIDTH, SCORES_HEIGHT));
        setMinimumSize(new Dimension(SCORES_WIDTH, SCORES_HEIGHT));
        score = new Score();
        highScore = new Score();
    }

    public void resetScores() {
        if (score.getScore() > highScore.getScore()) {
            highScore.setScore(score.getScore());
        }
        score.setScore(0);
        repaint();
    }

    public void increseGeneration() {
        generation++;
        repaint();
    }

    public void addPoint() {
        score.addScore();
        repaint();
    }

    public void setHighestScore(int highestScore) {
        score.setScore(highestScore);
        repaint();
    }

    private void scoreBoard(Graphics g) {

        String msg = "Score: ";
        msg += score.getScore();
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.black);
        g.setFont(small);
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, SCORES_HEIGHT / 2);
    }

    private void highScore(Graphics g) {

        String msg = "High score: ";
        msg += highScore.getScore();
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.black);
        g.setFont(small);
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 20);
    }

    private void generation(Graphics g) {

        String msg = "Generation: ";
        msg += generation;
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.black);
        g.setFont(small);
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) - 20);
    }

    private void fitness(Graphics g) {

        String msg = "Fitness: ";
        msg += highestFitness;
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.black);
        g.setFont(small);
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 40);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        generation(g);
        scoreBoard(g);
        highScore(g);
        fitness(g);
    }

    public double getHighestFitness() {
        return highestFitness;
    }

    public Scores setHighestFitness(double highestFitness) {
        this.highestFitness = highestFitness;
        return this;
    }
}
