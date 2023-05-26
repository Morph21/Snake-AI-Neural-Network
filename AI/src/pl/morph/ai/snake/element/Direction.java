package pl.morph.ai.snake.element;

public enum Direction {
    LEFT {
        @Override
        public int[] look(int dotSize, Direction lookDirection) {
            switch (lookDirection) {
                case UP :
                    return Direction.lookLeft(dotSize);
                case DOWN:
                    return Direction.lookRight(dotSize);
                case LEFT:
                    return Direction.lookDown(dotSize);
                case RIGHT:
                    return Direction.lookUp(dotSize);
                default:
                    return Direction.lookLeft(dotSize);
            }
        }
    },
    RIGHT {
        @Override
        public int[] look(int dotSize, Direction lookDirection) {
            switch (lookDirection) {
                case UP :
                    return Direction.lookRight(dotSize);
                case DOWN:
                    return Direction.lookLeft(dotSize);
                case LEFT:
                    return Direction.lookUp(dotSize);
                case RIGHT:
                    return Direction.lookDown(dotSize);
                default:
                    return Direction.lookRight(dotSize);
            }
        }
    },
    UP {
        @Override
        public int[] look(int dotSize, Direction lookDirection) {
            switch (lookDirection) {
                case UP :
                    return Direction.lookUp(dotSize);
                case DOWN:
                    return Direction.lookDown(dotSize);
                case LEFT:
                    return Direction.lookLeft(dotSize);
                case RIGHT:
                    return Direction.lookRight(dotSize);
                default:
                    return Direction.lookUp(dotSize);
            }
        }
    },
    DOWN {
        @Override
        public int[] look(int dotSize, Direction lookDirection) {
            switch (lookDirection) {
                case UP :
                    return Direction.lookDown(dotSize);
                case DOWN:
                    return Direction.lookUp(dotSize);
                case LEFT:
                    return Direction.lookRight(dotSize);
                case RIGHT:
                    return Direction.lookLeft(dotSize);
                default:
                    return Direction.lookDown(dotSize);
            }
        }
    };

    private static int[] lookLeft(int dotSize) {
        int xy[] = new int[2];
        xy[0] = -dotSize;
        xy[1] = 0;
        return xy;
    }

    private static int[] lookRight(int dotSize) {
        int xy[] = new int[2];
        xy[0] = dotSize;
        xy[1] = 0;
        return xy;
    }

    private static int[] lookUp(int dotSize) {
        int xy[] = new int[2];
        xy[0] = 0;
        xy[1] = -dotSize;
        return xy;
    }

    private static int[] lookDown(int dotSize) {
        int xy[] = new int[2];
        xy[0] = 0;
        xy[1] = dotSize;
        return xy;
    }

    private Direction opposite;

    static {
        UP.opposite = DOWN;
        DOWN.opposite = UP;
        LEFT.opposite = RIGHT;
        RIGHT.opposite = LEFT;
    }

    public abstract int[] look(int dotSize, Direction lookDirection);

    public boolean canMoveThere(Direction whereToGo) {
        if (whereToGo.equals(opposite)) {
            return false;
        } else {
            return true;
        }
    }
}
