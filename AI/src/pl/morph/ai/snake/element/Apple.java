package pl.morph.ai.snake.element;

public class Apple {
    private int apple_x;
    private int apple_y;

    public Apple (int apple_x, int apple_y) {
        this.apple_x = apple_x;
        this.apple_y = apple_y;
    }

    public int getApple_x() {
        return apple_x;
    }

    public Apple setApple_x(int apple_x) {
        this.apple_x = apple_x;
        return this;
    }

    public int getApple_y() {
        return apple_y;
    }

    public Apple setApple_y(int apple_y) {
        this.apple_y = apple_y;
        return this;
    }

    public Apple clone() {
        Apple apple = new Apple(apple_x, apple_y);
        return apple;
    }
}
