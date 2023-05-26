package pl.morph.ai.snake.page;

import pl.morph.ai.snake.element.Score;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

public class Scores extends JPanel implements Serializable {
    private static final long serialVersionUID = -6020702076594229938L;
    private final int SCORES_WIDTH = 800;
    private final int SCORES_HEIGHT = 800;

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
//        setPreferredSize(new Dimension(SCORES_WIDTH, SCORES_HEIGHT));
//        setSize(new Dimension(SCORES_WIDTH, SCORES_HEIGHT));
//        setMaximumSize(new Dimension(SCORES_WIDTH, SCORES_HEIGHT));
//        setMinimumSize(new Dimension(SCORES_WIDTH, SCORES_HEIGHT));

        score = new Score();
        highScore = new Score();
    }

    public boolean resetScores() {
        boolean scoreChanged = false;
        if (score.getScore() > highScore.getScore()) {
            highScore.setScore(score.getScore());
            spawnNotification();
            scoreChanged = true;
        }
        score.setScore(0);
        repaint();
        return scoreChanged;
    }

    private void spawnNotification() {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        spawnNotification();
        repaint();
    }

    private void generationInfo(Graphics g) {
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);
        g.setColor(Color.black);
        g.setFont(small);
        String msg;

        msg = "Score: " + score.getScore();
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 20);
        msg = "High score: " + highScore.getScore();
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 40);
        msg = "Generation: " + generation;
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 60);
        msg = "Fitness: " + highestFitness;
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 80);
        msg = "Average fitness: " + Board.avgFitness;
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 100);
        msg = "Dead snakes: " + deadSnakes;
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 120);
        msg = "Mutation rate: " + Board.MUTATION_RATE;
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 140);
        msg = "SaveSnake ratio: " + Board.SAVE_SNAKE_RATIO;
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 160);
        msg = "AutoSave: " + Board.autoSave;
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 180);
        msg = "Best only for crossing: " + Board.bestOnly;
        g.drawString(msg, (SCORES_WIDTH - metr.stringWidth(msg)) / 2, 200);
    }

    private void info(Graphics g) {

        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.black);
        g.setFont(small);
        String msg = "Press 'd' to change delay";
        g.drawString(msg, 20, 300);
        msg = "Press 's' to change showing snakes";
        g.drawString(msg, 20, 320);
        msg = "Press '+' to increase mutation\n Press - to decrease mutation ";
        g.drawString(msg, 20, 340);
        msg = "Press '-' to decrease mutation ";
        g.drawString(msg, 20, 360);
        msg = "Press 'p' to pause ";
        g.drawString(msg, 20, 380);
        msg = "Press 'r' to resume ";
        g.drawString(msg, 20, 400);
        msg = "Press 'w' to save all snakes to file (do it after pause) ";
        g.drawString(msg, 20, 420);
        msg = "Press 'l' to load all snakes from file (do it on paused game) ";
        g.drawString(msg, 20, 440);
        msg = "Press 'a' tu turn on autosave ";
        g.drawString(msg, 20, 460);
        msg = "Press 'b' tu turn on best only for crossing ";
        g.drawString(msg, 20, 480);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        generationInfo(g);
        info(g);
    }

    public double getHighestFitness() {
        return highestFitness;
    }

    public Scores setHighestFitness(double highestFitness) {
        this.highestFitness = highestFitness;
        return this;
    }

    public synchronized void setDeadSnakes(int deadSnakes) {
        this.deadSnakes = deadSnakes;
        repaint();
    }

    public int getDeadSnakes() {
        return this.deadSnakes;
    }

    public synchronized void incrementDeadSnakes() {
        this.deadSnakes++;
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
        repaint();
    }

    public void repaintIt() {
        repaint();
    }

    public int getGeneration() {
        return generation;
    }
}
