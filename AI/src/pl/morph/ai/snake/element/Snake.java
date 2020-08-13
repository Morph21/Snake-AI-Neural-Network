package pl.morph.ai.snake.element;

import pl.morph.ai.snake.engine.NeuralNetwork;
import pl.morph.ai.snake.page.Scores;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.floor;
import static java.lang.Math.pow;

public class Snake implements ActionListener {
    private final int boardWidth;
    private final int boardHeight;
    //Maximum length of snake
    private final int max_length;
    //Max random x coord where apple can be spawned
    private final int rand_pos_x;
    //Max random y coord where apple can be spawned
    private final int rand_pos_y;
    //Size of dot
    private final int dotSize = 10;
    private Apple appleToEat;
    private int length = 3;
    private int x[];
    private int y[];
    private Score snakeScore;
    private long lifetime = 0;
    private long timeLeft = 200;
    private double fitness = 0;
    public boolean inGame = true;
    public Direction direction = randomDirection();
    private Timer timer;
    private NeuralNetwork brain;
    private List<double[]> inputs = new ArrayList<>();
    private List<double[]> outputs = new ArrayList<>();

    public Snake(int boardWidth, int boardHeight, int delay, boolean humanPlaying) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        max_length = (boardWidth * boardHeight) / 100;
        this.x = new int[max_length];
        this.y = new int[max_length];
        this.rand_pos_x = boardWidth / 10;
        this.rand_pos_y = boardHeight / 10;

        if (!humanPlaying) {
            brain = new NeuralNetwork(4, 12, 4);
        }

        snakeScore = new Score();
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(delay, this);
        timer.start();
    }

    public Apple getAppleToEat() {
        return appleToEat;
    }

    public Snake setAppleToEat(Apple appleToEat) {
        this.appleToEat = appleToEat;
        return this;
    }

    public int getLength() {
        return length;
    }

    public Snake setLength(int length) {
        this.length = length;
        return this;
    }

    public int[] getX() {
        return x;
    }

    public Snake setX(int[] x) {
        this.x = x;
        return this;
    }

    public int[] getY() {
        return y;
    }

    public Snake setY(int[] y) {
        this.y = y;
        return this;
    }

    public void eat(Apple newAppleToEat) {
        this.appleToEat = newAppleToEat;
        this.length++;
    }

    /**
     * Creates new apple;
     */
    public void spawnApple() {
        int r = (int) (Math.random() * rand_pos_x);
        int apple_x = ((r * dotSize));

        r = (int) (Math.random() * rand_pos_y);
        int apple_y = ((r * dotSize));
        appleToEat = new Apple(apple_x, apple_y);
    }

    public void doDrawing(Graphics g) {
        if (inGame) {
            g.setColor(Color.red);
            g.fillRect(appleToEat.getApple_x(), appleToEat.getApple_y(), 10, 10);

            for (int z = 0; z < length; z++) {
                if (z == 0) {
                    //head
                    g.setColor(Color.green);
                    g.fillRect(x[z], y[z], 10, 10);
                } else {
                    //tail
                    g.setColor(Color.white);
                    g.fillRect(x[z], y[z], 10, 10);
                }
            }
            Toolkit.getDefaultToolkit().sync();
        } else {
            timer.stop();
            //TODO: What should it do when it's game over for him
        }
    }

    /**
     * Checks if snake is going to eat apple
     */
    public void checkApple() {

        // Snake head collided with apple
        if ((x[0] == appleToEat.getApple_x()) && (y[0] == appleToEat.getApple_y())) {
            snakeScore.addScore();
            length++;
            spawnApple();
        }
    }

    /**
     * Checks if snake is going to eat apple
     */
    public void checkApple(Scores scores) {

        // Snake head collided with apple
        if ((x[0] == appleToEat.getApple_x()) && (y[0] == appleToEat.getApple_y())) {
            if (timeLeft < 500) {
                timeLeft += 100;
            }
            scores.addPoint();
            length++;
            spawnApple();
        }
    }

    private void calculateFitness() {  //calculate the fitness of the snake
        if(snakeScore.getScore() < 10) {
            fitness = floor(lifetime * lifetime) * pow(2,snakeScore.getScore());
        } else {
            fitness = floor(lifetime * lifetime);
            fitness *= pow(2,10);
            fitness *= (snakeScore.getScore()-9);
        }
    }

    public void move() {
        lifetime++;
        timeLeft--;
        if (timeLeft <= 0) {
            inGame = false;
        }
        for (int z = length; z > 0; z--) {
            x[z] = x[(z - 1)];
            y[z] = y[(z - 1)];
        }

        switch (direction) {
            case LEFT:
                x[0] -= dotSize;
                break;
            case RIGHT:
                x[0] += dotSize;
                break;
            case UP:
                y[0] -= dotSize;
                break;
            case DOWN:
                y[0] += dotSize;
                break;

        }
    }

    public void think() throws Exception {
        double[] input = see();
        inputs.add(input);
        double[] predict = brain.predict(input);
        outputs.add(predict);
        double max = -1;
        int bestIndex = 0;
        for (int i = 0; i < predict.length; i++) {
            Double aDouble = predict[i];
            if (aDouble > max) {
                max = aDouble;
                bestIndex = i;
            }
        }

        int value = bestIndex + 1;
        if (value == 1) {
            direction = Direction.LEFT;
        } else if (value == 2) {
            direction = Direction.RIGHT;
        } else if (value == 3) {
            direction = Direction.UP;
        } else if (value == 4) {
            direction = Direction.DOWN;
        } else {
            throw new Exception("THINKING FAILED");
        }
    }

    public void checkCollision() {

        for (int z = length; z > 0; z--) {

            if ((z > 2) && (x[0] == x[z]) && (y[0] == y[z])) {
                inGame = false;
            }
        }

        if (y[0] >= boardHeight) {
            inGame = false;
        }

        if (y[0] < 0) {
            inGame = false;
        }

        if (x[0] >= boardWidth) {
            inGame = false;
        }

        if (x[0] < 0) {
            inGame = false;
        }

        if (!inGame) {
            timer.stop();
            calculateFitness();
        }
    }

    public int getScore() {
        return snakeScore.getScore();
    }

    private double[] see() {
        // LEFT, RIGHT, DOWN, UP
        // 0 - wall or tail
        // 0.5 empty
        // 1 - apple
        double[] see = new double[4];
        if (x[0] - dotSize < 0) {
            see[0] = 0;
        } else if (x[0] - dotSize == appleToEat.getApple_x() && y[0] == appleToEat.getApple_y()) {
            see[0] = 1;
        } else {
            see[0] = 0.5;
        }

        if (x[0] + dotSize > boardWidth) {
            see[1] = 0;
        } else if (x[0] + dotSize == appleToEat.getApple_x() && y[0] == appleToEat.getApple_y()) {
            see[1] = 1;
        } else {
            see[1] = 0.5;
        }

        if (y[0] + dotSize > boardHeight) {
            see[2] = 0;
        } else if (y[0] + dotSize == appleToEat.getApple_y() && x[0] == appleToEat.getApple_x()) {
            see[2] = 1;
        } else {
            see[2] = 0.5;
        }

        if (y[0] - dotSize > 0) {
            see[3] = 0;
        } else if (y[0] - dotSize == appleToEat.getApple_y() && x[0] == appleToEat.getApple_x()) {
            see[3] = 1;
        } else {
            see[3] = 0.5;
        }

        return see;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    public double getFitness() {
        return fitness;
    }

    public Snake setFitness(double fitness) {
        this.fitness = fitness;
        return this;
    }

    public NeuralNetwork getBrain() {
        return brain;
    }

    public Snake setBrain(NeuralNetwork brain) {
        this.brain = brain;
        return this;
    }

    public List<double[]> getInputs() {
        return inputs;
    }

    public Snake setInputs(List<double[]> inputs) {
        this.inputs = inputs;
        return this;
    }

    public List<double[]> getOutputs() {
        return outputs;
    }

    public Snake setOutputs(List<double[]> outputs) {
        this.outputs = outputs;
        return this;
    }

    private Direction randomDirection() {
        Random generator = new Random();
        int value = generator.nextInt( 4 ) + 1;
        if (value == 1) {
            direction = Direction.LEFT;
        } else if (value == 2) {
            direction = Direction.RIGHT;
        } else if (value == 3) {
            direction = Direction.UP;
        } else if (value == 4) {
            direction = Direction.DOWN;
        }

        return direction;
    }
}
