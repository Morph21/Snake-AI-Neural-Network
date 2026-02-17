package pl.morph.ai.snake.element;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {

    @Test
    void canMoveThereBlocksOppositeDirection() {
        assertFalse(Direction.UP.canMoveThere(Direction.DOWN));
        assertFalse(Direction.DOWN.canMoveThere(Direction.UP));
        assertFalse(Direction.LEFT.canMoveThere(Direction.RIGHT));
        assertFalse(Direction.RIGHT.canMoveThere(Direction.LEFT));
    }

    @Test
    void canMoveThereAllowsNonOppositeDirections() {
        assertTrue(Direction.UP.canMoveThere(Direction.LEFT));
        assertTrue(Direction.UP.canMoveThere(Direction.RIGHT));
        assertTrue(Direction.UP.canMoveThere(Direction.UP));
        assertTrue(Direction.LEFT.canMoveThere(Direction.UP));
        assertTrue(Direction.LEFT.canMoveThere(Direction.DOWN));
        assertTrue(Direction.LEFT.canMoveThere(Direction.LEFT));
    }

    @Test
    void valueReturnsDistinctValuesForCardinalDirections() {
        Set<Double> values = new HashSet<Double>();
        values.add(Direction.UP.value());
        values.add(Direction.DOWN.value());
        values.add(Direction.LEFT.value());
        values.add(Direction.RIGHT.value());
        assertEquals(4, values.size(), "Each cardinal direction should have a distinct value");
    }

    @Test
    void lookReturnsCorrectRelativeDirectionVectors() {
        int dotSize = 10;

        // UP looking UP should return up vector (0, -dotSize)
        int[] upUp = Direction.UP.look(dotSize, Direction.UP);
        assertEquals(0, upUp[0]);
        assertEquals(-dotSize, upUp[1]);

        // UP looking RIGHT should return right vector (dotSize, 0)
        int[] upRight = Direction.UP.look(dotSize, Direction.RIGHT);
        assertEquals(dotSize, upRight[0]);
        assertEquals(0, upRight[1]);

        // UP looking LEFT should return left vector (-dotSize, 0)
        int[] upLeft = Direction.UP.look(dotSize, Direction.LEFT);
        assertEquals(-dotSize, upLeft[0]);
        assertEquals(0, upLeft[1]);
    }

    @Test
    void randomReturnsOnlyCardinalDirections() {
        List<Direction> cardinals = Arrays.asList(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT);
        for (int i = 0; i < 100; i++) {
            Direction d = Direction.random();
            assertTrue(cardinals.contains(d), "random() should only return cardinal directions, got: " + d);
        }
    }
}
