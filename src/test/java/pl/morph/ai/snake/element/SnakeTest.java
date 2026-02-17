package pl.morph.ai.snake.element;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SnakeTest {

    private static final int BOARD_W = 200;
    private static final int BOARD_H = 200;
    private static final int DOT_SIZE = 10;
    private static final int DELAY = 50;

    private Snake snake;

    @BeforeEach
    void setUp() {
        snake = new Snake(BOARD_W, BOARD_H, DELAY, false, null, DOT_SIZE, Collections.<Wall>emptyList());
        snake.setStartingPosition(snake.new XY(5, 5));
        snake.spawnApple();
    }

    @Test
    void constructorInitializesCorrectly() {
        Snake s = new Snake(BOARD_W, BOARD_H, DELAY, false, null, DOT_SIZE, Collections.<Wall>emptyList());
        assertEquals(0, s.getScore());
        assertEquals(1, s.getLength());
        assertTrue(s.inGame);
    }

    @Test
    void moveUpdatesHeadPosition() {
        snake.direction = Direction.RIGHT;
        int xBefore = snake.getX()[0];
        snake.move();
        assertEquals(xBefore + DOT_SIZE, snake.getX()[0]);
    }

    @Test
    void moveShiftsBodySegments() {
        // Grow the snake by adding length manually
        snake.setLength(3);
        snake.getX()[0] = 50;
        snake.getY()[0] = 50;
        snake.getX()[1] = 40;
        snake.getY()[1] = 50;
        snake.getX()[2] = 30;
        snake.getY()[2] = 50;
        snake.direction = Direction.RIGHT;

        snake.move();

        // Old head position should now be body[1]
        assertEquals(50, snake.getX()[1]);
        assertEquals(50, snake.getY()[1]);
        // Old body[1] should now be body[2]
        assertEquals(40, snake.getX()[2]);
        assertEquals(50, snake.getY()[2]);
    }

    @Test
    void bodyCollideDetectsSelfCollision() {
        snake.setLength(4);
        snake.getX()[0] = 50;
        snake.getY()[0] = 50;
        snake.getX()[1] = 40;
        snake.getY()[1] = 50;
        snake.getX()[2] = 50;
        snake.getY()[2] = 50; // same as head
        snake.getX()[3] = 60;
        snake.getY()[3] = 50;

        assertTrue(snake.bodyCollide(50, 50));
    }

    @Test
    void bodyCollideReturnsFalseForNoCollision() {
        snake.setLength(2);
        snake.getX()[0] = 50;
        snake.getY()[0] = 50;
        snake.getX()[1] = 40;
        snake.getY()[1] = 50;

        assertFalse(snake.bodyCollide(60, 60));
    }

    @Test
    void wallCollideDetectsBoundaryCollision() {
        assertTrue(snake.wallCollide(BOARD_W, 50));  // right edge
        assertTrue(snake.wallCollide(-1, 50));        // left of board
        assertTrue(snake.wallCollide(50, BOARD_H));   // bottom edge
        assertTrue(snake.wallCollide(50, -1));         // above board
    }

    @Test
    void wallCollideReturnsFalseInsideBoard() {
        assertFalse(snake.wallCollide(50, 50));
        assertFalse(snake.wallCollide(0, 0));
    }

    @Test
    void foodCollideDetectsAppleCollision() {
        Apple apple = snake.getAppleToEat();
        assertTrue(snake.foodCollide(apple.getApple_x(), apple.getApple_y()));
    }

    @Test
    void foodCollideReturnsFalseForNoApple() {
        Apple apple = snake.getAppleToEat();
        assertFalse(snake.foodCollide(apple.getApple_x() + DOT_SIZE, apple.getApple_y()));
    }

    @Test
    void calculateFitnessReturnsHigherFitnessForHigherScore() {
        // Low score snake
        Snake s1 = new Snake(BOARD_W, BOARD_H, DELAY, false, null, DOT_SIZE, Collections.<Wall>emptyList());
        s1.setStartingPosition(s1.new XY(5, 5));
        s1.spawnApple();
        // Simulate some lifetime by moving
        for (int i = 0; i < 10; i++) {
            s1.direction = Direction.RIGHT;
            s1.move();
        }
        s1.calculateFitness();

        // High score snake - give it a higher score
        Snake s2 = new Snake(BOARD_W, BOARD_H, DELAY, false, null, DOT_SIZE, Collections.<Wall>emptyList());
        s2.setStartingPosition(s2.new XY(5, 5));
        s2.spawnApple();
        for (int i = 0; i < 10; i++) {
            s2.direction = Direction.RIGHT;
            s2.move();
        }
        // Manually set score higher via checkApple by placing apple on head
        Apple a = new Apple(s2.getX()[0], s2.getY()[0]);
        s2.setAppleToEat(a);
        s2.checkApple();
        s2.calculateFitness();

        assertTrue(s2.getFitness() > s1.getFitness());
    }

    @Test
    void cloneThisProducesIndependentSnakeWithSameBrain() {
        Snake clone = snake.cloneThis();

        assertNotSame(snake, clone);
        assertNotSame(snake.getBrain(), clone.getBrain());
        assertEquals(1, clone.getLength());
        assertTrue(clone.inGame);
    }

    @Test
    void cloneForReplayPreservesFoodListAndStartingPosition() {
        snake.setStartingPosition(snake.new XY(3, 4));
        snake.spawnApple();

        Snake replay = snake.cloneForReplay();

        assertNotSame(snake, replay);
        assertEquals(snake.getFoodList(), replay.getFoodList());
        assertEquals(snake.getStartingPosition().getX(), replay.getStartingPosition().getX());
        assertEquals(snake.getStartingPosition().getY(), replay.getStartingPosition().getY());
        assertEquals(snake.startingDirection, replay.direction);
    }

    @Test
    void lookFillsVisionArrayWith26Values() {
        snake.look();
        assertEquals(26, snake.vision.length);
    }

    @Test
    void lookInDirectionReturnsDistinctDistancesForDifferentPositions() {
        // Place snake at (10,5) facing RIGHT, wall at board edge (x=200)
        snake.setLength(1);
        snake.getX()[0] = 100;
        snake.getY()[0] = 50;
        snake.direction = Direction.RIGHT;

        // Place body segment at distance 3 (x=130) to the right
        snake.setLength(3);
        snake.getX()[1] = 130;
        snake.getY()[1] = 50;
        snake.getX()[2] = 140;
        snake.getY()[2] = 50;

        // Look right: wall distance = 1/(cells to wall), body distance = 1/3
        int[] rightDir = {DOT_SIZE, 0};
        double[] result = snake.lookInDirection(rightDir);

        // Body at distance 3 -> 1/3 = 0.3333...
        double bodyDist = result[1];
        assertTrue(bodyDist > 0.33 && bodyDist < 0.34,
            "Body distance at 3 cells should be ~0.333, got " + bodyDist);

        // Wall distance should be precise, not rounded
        double wallDist = result[2];
        // Wall is at x=200, head at x=100, dotSize=10, so (200-100)/10 = 10 cells away
        // But body blocks at 3, wall is at distance 10: 1/10 = 0.1
        assertTrue(wallDist > 0.09 && wallDist < 0.11,
            "Wall distance should be ~0.1, got " + wallDist);
    }

    @Test
    void checkAppleIncrementsScoreAndLength() {
        int scoreBefore = snake.getScore();
        int lengthBefore = snake.getLength();

        // Place apple exactly on snake head
        Apple apple = new Apple(snake.getX()[0], snake.getY()[0]);
        snake.setAppleToEat(apple);
        snake.checkApple();

        assertEquals(scoreBefore + 1, snake.getScore());
        assertEquals(lengthBefore + 1, snake.getLength());
    }
}
