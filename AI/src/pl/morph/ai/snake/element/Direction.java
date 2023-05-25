package pl.morph.ai.snake.element;

public enum Direction {
    LEFT,
    RIGHT,
    UP,
    DOWN;

    private Direction opposite;

    static {
        UP.opposite = DOWN;
        DOWN.opposite = UP;
        LEFT.opposite = RIGHT;
        RIGHT.opposite = LEFT;
    }

    public boolean canMoveThere(Direction whereToGo) {
        if (whereToGo.equals(opposite)) {
            return false;
        } else {
            return true;
        }
    }
}
