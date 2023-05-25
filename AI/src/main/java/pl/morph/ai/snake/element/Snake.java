package pl.morph.ai.snake.element;

import pl.morph.ai.snake.engine.NeuralNetwork;
import pl.morph.ai.snake.page.Board;
import pl.morph.ai.snake.page.Scores;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.floor;
import static java.lang.Math.pow;
import static pl.morph.ai.snake.engine.Matrix.random;

public class Snake implements Serializable {
    private static final long serialVersionUID = -4639319120226368237L;
    private final int boardWidth;
    private final int boardHeight;
    //Maximum length of snake
    private final int max_length;
    //Max random x coord where apple can be spawned
    private final int rand_pos_x;
    //Max random y coord where apple can be spawned
    private final int rand_pos_y;
    //Size of dot
    private int dotSize;
    private Apple appleToEat;
    private int length = 4;
    private int x[];
    private int y[];
    private Score snakeScore;
    private long lifetime = 0;
    private long timeLeft = 100;
    private long maxLife;
    private long lifeForApple;
    private double fitness = 0;
    public boolean inGame = true;
    public Direction direction = Direction.RIGHT;
    private NeuralNetwork brain;
    private boolean showIt = false;
    private boolean humanPlaying;
    private int delay;
    private List<Apple> foodList = new ArrayList<>();
    private int foodIterate = 0;
    private boolean bestSnake = false;
    private Scores scores;
    private boolean wallCollide = false;
    private boolean bodyCollide = false;

    private int hidden_layers = 2;
    private int hidden_nodes = 16;
    private double mutationRate;

    final int input_count = 24;
    final int output_count = 3;
    double vision[] = new double[input_count];
    double decision[] = new double[output_count];

    private List<Wall> walls;

    public Snake(int boardWidth, int boardHeight, int delay, boolean humanPlaying, List<Apple> foodList, int dotSize, List<Wall> walls) {
        this.dotSize = dotSize;
        this.delay = delay;
        this.humanPlaying = humanPlaying;
        this.mutationRate = Board.MUTATION_RATE;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        max_length = (boardWidth * boardHeight) / (dotSize * dotSize);
        this.x = new int[max_length];
        this.y = new int[max_length];
        this.rand_pos_x = boardWidth / dotSize;
        this.rand_pos_y = boardHeight / dotSize;
        this.maxLife = ((boardWidth / dotSize) * 10);
        timeLeft = (boardWidth / dotSize) * 10;
        this.lifeForApple = maxLife / 4;

        this.walls = walls;
        if (foodList != null) {
            this.foodList = foodList;
        }
        if (!humanPlaying) {
            brain = new NeuralNetwork(input_count, hidden_nodes, output_count, hidden_layers);
        }

        snakeScore = new Score();
        snakeScore.setScore(1);
    }

    public Snake cloneThis() {  //clone the snake
        Snake clone = new Snake(boardWidth, boardHeight, delay, humanPlaying, null, dotSize, walls);
        clone.brain = brain.clone();
        return clone;
    }

    public Snake cloneForReplay() {  //clone a version of the snake that will be used for a replay
        Snake clone = new Snake(boardWidth, boardHeight, delay, humanPlaying, foodList, dotSize, walls);
        clone.brain = brain.clone();
        return clone;
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
        if (!bestSnake) {

            randomApple();
        } else {

            if (foodList.size() > foodIterate) {
                appleToEat = foodList.get(foodIterate);
                foodIterate++;
            } else {
                randomApple();
            }
        }
    }

    private void randomApple() {
        appleToEat = randomizeApple();
        for (int i = 0; i < length; i++) {
            while ((appleToEat.getApple_x() == x[i] && appleToEat.getApple_y() == y[i]) || wallCollide(appleToEat.getApple_x(), appleToEat.getApple_y())) {
                appleToEat = randomizeApple();
            }
        }
        foodList.add(appleToEat);
    }

    private Apple randomizeApple() {
        int r = (int) (Math.random() * rand_pos_x);
        int apple_x = ((r * dotSize));

        r = (int) (Math.random() * rand_pos_y);
        int apple_y = ((r * dotSize));
        return new Apple(apple_x, apple_y);
    }

    public void doDrawing(Graphics g) {
        if (inGame) {
            if (showIt) {
                g.setColor(Color.red);
                g.fillRect(appleToEat.getApple_x(), appleToEat.getApple_y(), dotSize - 1, dotSize - 1);
                g.setColor(Color.black);
                g.drawRect(appleToEat.getApple_x(), appleToEat.getApple_y(), dotSize, dotSize);
            }

            for (int z = length; z >= 0; z--) {
                if (z == 0) {
                    //head
                    if (this.showIt) {
                        g.setColor(brain.tailColor());
                        g.fillRect(x[z], y[z], dotSize - 1, dotSize - 1);
                        g.setColor(Color.yellow);
                        g.drawRect(x[z], y[z], dotSize, dotSize);
                    }
                } else {
                    //tail
                    if (this.showIt) {
//                        brain.
                        g.setColor(brain.tailColor());
                        g.fillRect(x[z], y[z], dotSize - 1, dotSize - 1);
                        g.setColor(Color.white);
                        g.drawRect(x[z], y[z], dotSize, dotSize);
                    }

                }
            }
//            Toolkit.getDefaultToolkit().sync();
        } else {
            if (bestSnake) {
                for (int z = 0; z < length; z++) {
                    if (z == 0) {
                        //head
                        if (this.showIt) {
                            g.setColor(Color.CYAN);
                            g.fillRect(x[z], y[z], dotSize - 1, dotSize - 1);
                            g.setColor(Color.black);
                            g.drawRect(x[z], y[z], dotSize, dotSize);
                        }
                    } else {
                        //tail
                        if (this.showIt) {
                            g.setColor(Color.gray);
                            g.fillRect(x[z], y[z], dotSize - 1, dotSize - 1);
                            g.setColor(Color.black);
                            g.drawRect(x[z], y[z], dotSize, dotSize);
                        }

                    }
                }
            }
            //TODO: What should it do when it's game over for him
        }
    }

    /**
     * Checks if snake is going to eat apple
     */
    public void checkApple() {

        // Snake head collided with apple
        if ((x[0] == appleToEat.getApple_x()) && (y[0] == appleToEat.getApple_y())) {
            increaseLifeSpan();
            snakeScore.addScore();
            if (bestSnake && scores != null) {
                scores.addPoint();
            }

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
            increaseLifeSpan();
            scores.addPoint();
            length++;
            spawnApple();
        }
    }

    public void increaseLifeSpan() {
        timeLeft += lifeForApple;
        if (timeLeft < maxLife) {

        }
    }

//    public void calculateFitness() {  //calculate the fitness of the snake
//        fitness = floor(lifetime * lifetime) * pow(5, snakeScore.getScore());
//
////        if (snakeScore.getScore() < 10) {
////            fitness = floor(lifetime * lifetime) * pow(2, snakeScore.getScore());
////        } else {
////            fitness = floor(lifetime * lifetime);
////            fitness *= pow(2, 10);
////            fitness *= (snakeScore.getScore() - 9);
////        }
//        setHighestFitness();
//    }

//    public void calculateFitness() {  //calculate the fitness of the snake
//        fitness = 200 * snakeScore.getScore() + lifetime;
//        setHighestFitness();
//    }

    public void calculateFitness() {  //calculate the fitness of the snake
        fitness = lifetime + (pow(2, snakeScore.getScore()) + pow(snakeScore.getScore(), 2.1) * 500) - (pow(snakeScore.getScore(), 1.2) * pow((0.25 * lifetime), 1.3));
        setHighestFitness();
    }

    private void setHighestFitness() {
        if (fitness > brain.getHighestFitness()) {
            brain.setHighestFitness(fitness);
        }
    }

    public Snake crossover(Snake parent) {  //crossover the snake with another snake
        Snake child = new Snake(boardWidth, boardHeight, delay, humanPlaying, null, dotSize, walls);
        child.brain = brain.crossover(parent.brain);
        return child;
    }

    public boolean mutate() {  //mutate the snakes brain
        if (shouldMutate()) {
            brain.mutate(mutationRate);
            return true;
        }
        return false;
    }

    private boolean shouldMutate() {
        // Not every snake should mutate
        double rand = random(0, 1);
        if (rand < 0.9) {
            return true;
        }
        return false;
    }

    public void think() {
        decision = brain.output(vision);
        int maxIndex = 0;
        double max = 0;
        for (int i = 0; i < decision.length; i++) {
            if (decision[i] > max) {
                max = decision[i];
                maxIndex = i;
            }
        }
        checkDirection(maxIndex);
    }


    public void move() {  //move the snake
        if (inGame) {
            if (!humanPlaying) {
                lifetime++;
                timeLeft--;
            }
            if (foodCollide(x[0], y[0])) {
                checkApple();
            }
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

            if (wallCollide(x[0], y[0])) {
                wallCollide = true;
                inGame = false;
            } else if (bodyCollide(x[0], y[0])) {
                bodyCollide = true;
                inGame = false;
            } else if (timeLeft <= 0 && !humanPlaying) {
                inGame = false;
            }

        }
    }

    public int getScore() {
        return snakeScore.getScore();
    }

    public void look() {  //look in all 8 directions and check for food, body and wall
        vision = new double[input_count];
        double[] temp = lookInDirection(-dotSize, 0); // LEFT
        vision[0] = temp[0];
        vision[1] = temp[1];
        vision[2] = temp[2];
        temp = lookInDirection(-dotSize, -dotSize); // LEFT TOP
        vision[3] = temp[0];
        vision[4] = temp[1];
        vision[5] = temp[2];
        temp = lookInDirection(0, -dotSize); // TOP
        vision[6] = temp[0];
        vision[7] = temp[1];
        vision[8] = temp[2];
        temp = lookInDirection(dotSize, -dotSize); // RIGHT TOP
        vision[9] = temp[0];
        vision[10] = temp[1];
        vision[11] = temp[2];
        temp = lookInDirection(dotSize, 0); // RIGHT
        vision[12] = temp[0];
        vision[13] = temp[1];
        vision[14] = temp[2];
        temp = lookInDirection(dotSize, dotSize); // RIGHT DOWN
        vision[15] = temp[0];
        vision[16] = temp[1];
        vision[17] = temp[2];
        temp = lookInDirection(0, dotSize); // DOWN
        vision[18] = temp[0];
        vision[19] = temp[1];
        vision[20] = temp[2];
        temp = lookInDirection(-dotSize, dotSize);//LEFT DONW
        vision[21] = temp[0];
        vision[22] = temp[1];
        vision[23] = temp[2];

//        switch (direction) {
//            case RIGHT:
//                vision[24] = 1;
//                vision[25] = 0;
//                vision[26] = 0;
//                vision[27] = 0;
//                break;
//            case UP:
//                vision[24] = 0;
//                vision[25] = 1;
//                vision[26] = 0;
//                vision[27] = 0;
//                break;
//            case DOWN:
//                vision[24] = 0;
//                vision[25] = 0;
//                vision[26] = 1;
//                vision[27] = 0;
//                break;
//            case LEFT:
//                vision[24] = 0;
//                vision[25] = 0;
//                vision[26] = 0;
//                vision[27] = 1;
//                break;
//        }
    }

    double[] lookInDirection(int X, int Y) {  //look in a direction and check for food, body and wall
        double look[] = new double[output_count];

        int head_x = x[0];
        int head_y = y[0];

        float distance = 0;
        boolean foodFound = false;
        boolean bodyFound = false;
        head_x += X;
        head_y += Y;
        distance += 1;
        while (!wallCollide(head_x, head_y)) {
            if (!foodFound && foodCollide(head_x, head_y)) {
                foodFound = true;
//                look[0] = 1 / distance;
                look[0] = 1;
            }
            if (!bodyFound && bodyCollide(head_x, head_y)) {
                bodyFound = true;
//                look[1] = 1 / distance;
                look[1] = 1;
            }

            head_x += X;
            head_y += Y;
            distance += 1;
        }

        look[2] = 1 / distance;
//        look[2] = 1;
        return look;
    }

    boolean bodyCollide(double X, double Y) {  //check if a position collides with the snakes body
        for (int i = 1; i < length; i++) {
            if (X == x[i] && Y == y[i]) {
                return true;
            }
        }
        return false;
    }

    boolean foodCollide(double X, double Y) {  //check if a position collides with the food
        if (X == appleToEat.getApple_x() && Y == appleToEat.getApple_y()) {
            return true;
        }
        return false;
    }

    boolean wallCollide(double X, double Y) {  //check if a position collides with the wall
        if (X >= boardWidth || X < 0 || Y >= boardHeight || Y < 0) {
            return true;
        }

        if (walls != null && !walls.isEmpty()) {
            for (Wall wall : walls) {
                if (X == wall.getX() && Y == wall.getY()) {
                    return true;
                }
            }
        }

        return false;
    }

    public double calculateDistanceBetweenPoints(
            double x1,
            double y1,
            double x2,
            double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

//    @Override
//    public void actionPerformed(ActionEvent e) {
//
//    }

    public double getFitness() {
        return fitness;
    }

    public double getHighestFitness() {
        return brain.getHighestFitness();
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

    public Direction randomDirection() {
        Random generator = new Random();
        int value = generator.nextInt(output_count) + 1;
        checkDirection(value);

        return direction;
    }

//    private void checkDirection(int value) {
//        if (value == 1) {
//            direction = Direction.LEFT;
//        } else if (value == 2) {
//            direction = Direction.RIGHT;
//        } else if (value == 3) {
//            direction = Direction.UP;
//        } else if (value == 4) {
//            direction = Direction.DOWN;
//        }
//    }

    private void checkDirection(int value) {
        if (value == 1) { // LEFT
            switch (direction) {
                case LEFT:
                    direction = Direction.DOWN;
                    break;
                case DOWN:
                    direction = Direction.RIGHT;
                    break;
                case UP:
                    direction = Direction.LEFT;
                    break;
                case RIGHT:
                    direction = Direction.UP;
                    break;
            }
        } else if (value == 2) { // RIGHT
            switch (direction) {
                case LEFT:
                    direction = Direction.UP;
                    break;
                case DOWN:
                    direction = Direction.LEFT;
                    break;
                case UP:
                    direction = Direction.RIGHT;
                    break;
                case RIGHT:
                    direction = Direction.DOWN;
                    break;
            }
        } else if (value == 3) { // MOVE FORWARD
        }
    }


    public void setShowIt(boolean showIt) {
        this.showIt = showIt;
    }

    public double getMutationRate() {
        return mutationRate;
    }

    public void setMutationRate(double mutationRate) {
        this.mutationRate = mutationRate;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public boolean isBestSnake() {
        return bestSnake;
    }

    public void setBestSnake(boolean bestSnake) {
        this.bestSnake = bestSnake;
    }

    public void setScores(Scores scores) {
        this.scores = scores;
    }

    public int getDotSize() {
        return dotSize;
    }

    public List<Apple> getFoodList() {
        return foodList;
    }

    public void setFoodList(List<Apple> foodList) {
        this.foodList = foodList;
    }

    public void setDotSize(int dotSize) {
        this.dotSize = dotSize;
    }

    public void setWalls(List<Wall> walls) {
        this.walls = walls;
    }
}
