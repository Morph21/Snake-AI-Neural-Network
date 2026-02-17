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
                case TOP_LEFT:
                    return Direction.lookDownLeft(dotSize);
                case TOP_RIGHT:
                    return Direction.lookTopLeft(dotSize);
                case DOWN_LEFT:
                    return Direction.lookDownRight(dotSize);
                case DOWN_RIGHT:
                    return Direction.lookTopRight(dotSize);
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
                case TOP_LEFT:
                    return Direction.lookTopRight(dotSize);
                case TOP_RIGHT:
                    return Direction.lookDownRight(dotSize);
                case DOWN_LEFT:
                    return Direction.lookTopLeft(dotSize);
                case DOWN_RIGHT:
                    return Direction.lookDownLeft(dotSize);
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
                case TOP_LEFT:
                    return Direction.lookTopLeft(dotSize);
                case TOP_RIGHT:
                    return Direction.lookTopRight(dotSize);
                case DOWN_LEFT:
                    return Direction.lookDownLeft(dotSize);
                case DOWN_RIGHT:
                    return Direction.lookDownRight(dotSize);
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
                case TOP_LEFT:
                    return Direction.lookDownRight(dotSize);
                case TOP_RIGHT:
                    return Direction.lookDownLeft(dotSize);
                case DOWN_LEFT:
                    return Direction.lookTopRight(dotSize);
                case DOWN_RIGHT:
                    return Direction.lookTopLeft(dotSize);
                default:
                    return Direction.lookDown(dotSize);
            }
        }
    },

    TOP_LEFT {
        @Override
        public int[] look(int dotSize, Direction lookDirection) {
            return new int[0];
        }
    },
    TOP_RIGHT {
        @Override
        public int[] look(int dotSize, Direction lookDirection) {
            return new int[0];
        }
    },
    DOWN_LEFT {
        @Override
        public int[] look(int dotSize, Direction lookDirection) {
            return new int[0];
        }
    },
    DOWN_RIGHT {
        @Override
        public int[] look(int dotSize, Direction lookDirection) {
            return new int[0];
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

    private static int[] lookTopLeft(int dotSize) {
        int xy[] = new int[2];
        xy[0] = -dotSize;
        xy[1] = -dotSize;
        return xy;
    }
    private static int[] lookTopRight(int dotSize) {
        int xy[] = new int[2];
        xy[0] = dotSize;
        xy[1] = -dotSize;
        return xy;
    }
    private static int[] lookDownRight(int dotSize) {
        int xy[] = new int[2];
        xy[0] = dotSize;
        xy[1] = dotSize;
        return xy;
    }
    private static int[] lookDownLeft(int dotSize) {
        int xy[] = new int[2];
        xy[0] = -dotSize;
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

    public static Direction random() {
        double random = Math.random();
        if (random < 0.25) {
            return Direction.RIGHT;
        }
        if (random < 0.5) {
            return Direction.LEFT;
        }
        if (random < 0.75) {
            return Direction.UP;
        }
        return Direction.DOWN;
    }

    public double value() {
        switch (this) {
            case UP:
                return 0;
            case DOWN:
                return 0.33;
            case LEFT:
                return 0.66;
            case RIGHT:
                return 1;
        }
        return 0.77;
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
