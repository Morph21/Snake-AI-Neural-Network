package pl.morph.ai.snake.element;

import java.awt.*;
import java.io.Serializable;

public class Wall implements Serializable {
    private static final long serialVersionUID = 1;

    private int x;
    private int y;
    private int width;
    private int height;

    public Wall(int x, int y, int dotSize) {
        this.x = x;
        this.y = y;
        this.width = dotSize;
        this.height = dotSize;
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

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void doDrawing(Graphics g) {
        g.setColor(Color.gray);
        g.fillRect(x, y, width - 1, height - 1);
        g.setColor(Color.black);
        g.drawRect(x, y, width, height);
    }
}
