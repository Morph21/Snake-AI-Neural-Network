package pl.morph.ai.snake.page;

import pl.morph.ai.snake.element.Score;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Scores extends JPanel {
    private final int SCORES_WIDTH = 800;
    private final int SCORES_HEIGHT = 100;

    private Score score;
    private Score highScore;
    private double highestFitness = 0;
    private int generation = 1;
    private int deadSnakes = 0;

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
        highScore.setScore(highestScore);
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

    private void deadSnakes(Graphics g) {

        String msg = "Dead snakes: ";
        msg += deadSnakes;
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.black);
        g.setFont(small);
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 60);
    }

    private void mutation(Graphics g) {

        String msg = "Mutation rate: ";
        msg += Board.MUTATION_RATE;
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.black);
        g.setFont(small);
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 80);
    }

    private void info(Graphics g) {

        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.black);
        g.setFont(small);
        String msg = "Press d to change delay";
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 300);
        msg = "Press s to change showing snakes\nPress + to increase mutation\n Press - to decrease mutation ";
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 320);
        msg = "Press + to increase mutation\n Press - to decrease mutation ";
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 340);
        msg = "Press - to decrease mutation ";
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 360);
        msg = "Press p to pause ";
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 380);
        msg = "Press r to resume ";
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, (SCORES_HEIGHT / 2) + 400);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        generation(g);
        scoreBoard(g);
        highScore(g);
        fitness(g);
        deadSnakes(g);
        mutation(g);
        info(g);
    }

    public double getHighestFitness() {
        return highestFitness;
    }

    public Scores setHighestFitness(double highestFitness) {
        this.highestFitness = highestFitness;
        return this;
    }

    public void setDeadSnakes(int deadSnakes) {
        this.deadSnakes = deadSnakes;
        repaint();
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
        repaint();
    }
}
