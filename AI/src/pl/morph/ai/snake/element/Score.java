package pl.morph.ai.snake.element;

public class Score {
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
