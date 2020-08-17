package pl.morph.ai.snake.element;

import java.io.Serializable;

public class Score implements Serializable {
    private int score;

    public Score() {
        score = 0;
    }

    public int getScore() {
        return score;
    }

    public Score setScore(int score) {
        this.score = score;
        return this;
    }

    public void addScore() {
        score++;
    }
}
