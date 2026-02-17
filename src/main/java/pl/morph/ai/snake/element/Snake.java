package pl.morph.ai.snake.element;

import pl.morph.ai.snake.engine.NeuralNetwork;
import pl.morph.ai.snake.page.Scores;

import java.awt.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static java.lang.Math.floor;
import static java.lang.Math.pow;
import static pl.morph.ai.snake.engine.Matrix.random;

public class Snake implements Serializable {
    private static final long serialVersionUID = -7594040040072369358L;
    private final int boardWidth;
    private final int boardHeight;
    //Maximum length of pl.morph.ai.snake
    private final int max_length;
    //Max random x coord where apple can be spawned
    private final int rand_pos_x;
    //Max random y coord where apple can be spawned
    private final int rand_pos_y;
    //Size of dot
    private int dotSize;
    private Apple appleToEat;
    private int length = 1;
    private int x[];
    private int y[];
    private Score snakeScore;
    private long lifetime = 0;
    private long timeLeft = 100;
    private long maxLife;
    private long lifeForApple;
    private double fitness = 0;
    public boolean inGame = true;
    public Direction startingDirection = Direction.random();
    public Direction direction = startingDirection;
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
    private int hidden_nodes = 24;
    private double mutationRate;

    final int input_count = 26;
    final int output_count = 3;

    final int look_count = 3;
    double vision[] = new double[input_count];
    double decision[] = new double[output_count];

    private XY startingPosition;

    private List<Wall> walls;

    public Snake(int boardWidth,
                 int boardHeight,
                 int delay,
                 boolean humanPlaying,
                 List<Apple> foodList,
                 int dotSize,
                 List<Wall> walls) {
        this.dotSize = dotSize;
        this.delay = delay;
        this.humanPlaying = humanPlaying;
        this.mutationRate = 0.03;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        max_length = (boardWidth * boardHeight) / (dotSize * dotSize);
        this.x = new int[max_length];
        this.y = new int[max_length];
        this.rand_pos_x = boardWidth / dotSize;
        this.rand_pos_y = boardHeight / dotSize;
        this.maxLife = ((boardWidth / dotSize) * 15);
        timeLeft = (boardWidth / dotSize) * 15;
        this.lifeForApple = maxLife;

        this.walls = walls;
        if (foodList != null) {
            this.foodList = foodList;
        }
        if (!humanPlaying) {
            brain = new NeuralNetwork(input_count, hidden_nodes, output_count, hidden_layers);
        }

        snakeScore = new Score();
        snakeScore.setScore(0);
    }

    public Snake cloneThis() {  //clone the pl.morph.ai.snake
        Snake clone = new Snake(boardWidth, boardHeight, delay, humanPlaying, null, dotSize, walls);
        clone.brain = brain.clone();
        clone.startingDirection = Direction.random();
        clone.direction = clone.startingDirection;
        return clone;
    }

    public Snake cloneForReplay() {  //clone a version of the pl.morph.ai.snake that will be used for a replay
        Snake clone = new Snake(boardWidth, boardHeight, delay, humanPlaying, foodList, dotSize, walls);
        clone.brain = brain.clone();
        clone.direction = this.startingDirection;
        clone.startingDirection = this.startingDirection;
        clone.setStartingPosition(this.startingPosition);
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
        Set<Long> occupied = new HashSet<Long>();

        for (int i = 0; i < length; i++) {
            occupied.add(posKey(x[i] / dotSize, y[i] / dotSize));
        }

        if (walls != null && !walls.isEmpty()) {
            for (Wall wall : walls) {
                occupied.add(posKey(wall.getX() / dotSize, wall.getY() / dotSize));
            }
        }

        int totalCells = rand_pos_x * rand_pos_y;
        int freeCells = totalCells - occupied.size();

        if (freeCells > 0) {
            int target = (int) (Math.random() * freeCells);
            int count = 0;
            for (int bx = 0; bx < rand_pos_x; bx++) {
                for (int by = 0; by < rand_pos_y; by++) {
                    if (!occupied.contains(posKey(bx, by))) {
                        if (count == target) {
                            appleToEat = new Apple(bx * dotSize, by * dotSize);
                            foodList.add(appleToEat);
                            return;
                        }
                        count++;
                    }
                }
            }
        }

        appleToEat = randomizeApple();
        foodList.add(appleToEat);
    }

    private static long posKey(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public List<XY> populate(int maxX, int maxY) {
        List<XY> boardTable = new ArrayList<>();
        for (int x = 0; x < maxX; x++) {
            for (int y = 0; y < maxY; y++) {
                boardTable.add(new XY(x, y));
            }
        }
        return boardTable;
    }

    public class XY implements Serializable {
        private int x;
        private int y;

        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XY xy = (XY) o;
            return x == xy.x && y == xy.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
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
            if (showIt && appleToEat != null) {
                g.setColor(Color.red);
                g.fillRect(appleToEat.getApple_x(), appleToEat.getApple_y(), dotSize - 1, dotSize - 1);
                g.setColor(Color.black);
                g.drawRect(appleToEat.getApple_x(), appleToEat.getApple_y(), dotSize, dotSize);
            }

            for (int z = length - 1; z >= 0; z--) {
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
     * Checks if pl.morph.ai.snake is going to eat apple
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
     * Checks if pl.morph.ai.snake is going to eat apple
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
        if (timeLeft < maxLife) {
            timeLeft += lifeForApple;
        }
    }

    public void calculateFitness() {  //calculate the fitness of the pl.morph.ai.snake
        int score = snakeScore.getScore();
        if (score < 10) {
            fitness = floor(lifetime * lifetime) * pow(2, score);
        } else {
            fitness = floor(lifetime * lifetime) * pow(2, 10) * (score - 9);
        }
        // bonus that scales with score to reward eating over just surviving
        fitness += pow(score, 3) * 1000;
        setHighestFitness();
    }

    private void setHighestFitness() {
        if (fitness > brain.getHighestFitness()) {
            brain.setHighestFitness(fitness);
        }
    }

    public Snake crossover(Snake parent) {  //crossover the pl.morph.ai.snake with another pl.morph.ai.snake
        Snake child = new Snake(boardWidth, boardHeight, delay, humanPlaying, null, dotSize, walls);
        child.startingDirection = Direction.random();
        child.direction = child.startingDirection;
        child.brain = brain.crossover(parent.brain);
        return child;
    }

    public void mutate() {  //mutate the snakes brain
        brain.mutate(mutationRate);
    }

    private boolean shouldMutate() {
        // Not every pl.morph.ai.snake should mutate
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


    public void move() {  //move the pl.morph.ai.snake
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
        vision[0] = direction.value();
        vision[1] = tailDirection().value();
        double[] temp = lookInDirection(direction.look(dotSize, Direction.LEFT)); // LEFT
        vision[2] = temp[0];
        vision[3] = temp[1];
        vision[4] = temp[2];
        temp = lookInDirection(direction.look(dotSize, Direction.UP)); // TOP
        vision[5] = temp[0];
        vision[6] = temp[1];
        vision[7] = temp[2];
        temp = lookInDirection(direction.look(dotSize, Direction.RIGHT)); // RIGHT
        vision[8] = temp[0];
        vision[9] = temp[1];
        vision[10] = temp[2];

        temp = lookInDirection(direction.look(dotSize, Direction.DOWN)); // DOWN
        vision[11] = temp[0];
        vision[12] = temp[1];
        vision[13] = temp[2];

        temp = lookInDirection(direction.look(dotSize, Direction.TOP_LEFT));  // LEFT TOP
        vision[14] = temp[0];
        vision[15] = temp[1];
        vision[16] = temp[2];

        temp = lookInDirection(direction.look(dotSize, Direction.TOP_RIGHT));  // RIGHT TOP
        vision[17] = temp[0];
        vision[18] = temp[1];
        vision[19] = temp[2];
        temp = lookInDirection(direction.look(dotSize, Direction.DOWN_RIGHT)); // RIGHT DOWN
        vision[20] = temp[0];
        vision[21] = temp[1];
        vision[22] = temp[2];
        temp = lookInDirection(direction.look(dotSize, Direction.DOWN_LEFT)); // LEFT DOWN
        vision[23] = temp[0];
        vision[24] = temp[1];
        vision[25] = temp[2];
    }

    double[] lookInDirection(int[] XY) {  //look in a direction and check for food, body and wall
        double look[] = new double[look_count];

        int X = XY[0];
        int Y = XY[1];

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
                look[0] = 1;
            }
            if (!bodyFound && bodyCollide(head_x, head_y)) {
                bodyFound = true;
                look[1] = new BigDecimal(1 / distance).setScale(1, RoundingMode.HALF_UP).doubleValue();

            }

            head_x += X;
            head_y += Y;
            distance += 1;
        }


        look[2] = new BigDecimal(1 / distance).setScale(1, RoundingMode.HALF_UP).doubleValue();


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
        if (appleToEat != null && X == appleToEat.getApple_x() && Y == appleToEat.getApple_y()) {
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
        if (value == 0) { // LEFT
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
        } else if (value == 1) { // RIGHT
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
        } else if (value == 2) { // MOVE FORWARD
        }
    }

    private Direction tailDirection() {
        int xLast = x[length - 1];
        int yLast = y[length - 1];

        if (length < 2) {
            return direction;
        }

        int xBeforeLast = x[length - 2];
        int yBeforeLast = y[length - 2];

        if (xLast == xBeforeLast) {
            if (yBeforeLast > yLast) {
                return Direction.DOWN;
            } else {
                return Direction.UP;
            }
        }
        if (yLast == yBeforeLast) {
            if (xBeforeLast > xLast) {
                return Direction.RIGHT;
            } else {
                return Direction.LEFT;
            }
        }
        return Direction.random();
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

    public void setStartingPosition(XY startingPosition) {
        this.startingPosition = startingPosition;
        x[0] = startingPosition.getX() * dotSize;
        y[0] = startingPosition.getY() * dotSize;
    }

    public XY getStartingPosition() {
        return this.startingPosition;
    }
}
