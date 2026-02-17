package pl.morph.ai.snake.element;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreTest {

    @Test
    void startsAtZero() {
        Score score = new Score();
        assertEquals(0, score.getScore());
    }

    @Test
    void addScoreIncrements() {
        Score score = new Score();
        score.addScore();
        assertEquals(1, score.getScore());
        score.addScore();
        assertEquals(2, score.getScore());
    }

    @Test
    void setScoreSetsValue() {
        Score score = new Score();
        score.setScore(42);
        assertEquals(42, score.getScore());
    }
}
