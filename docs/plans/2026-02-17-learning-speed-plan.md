# Learning Speed Improvement Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Break the snake AI's score plateau at 100-200 apples by fixing vision precision, food distance, fitness scaling, and time budget.

**Architecture:** All changes are in `Snake.java` — three methods modified: `lookInDirection()`, `calculateFitness()`, `increaseLifeSpan()`. No new classes, no API changes, no serialization changes.

**Tech Stack:** Java 8, JUnit 5, Maven

---

### Task 1: Fix Vision Precision — Remove BigDecimal Rounding

**Files:**
- Modify: `src/main/java/pl/morph/ai/snake/element/Snake.java:522-558` (lookInDirection)
- Test: `src/test/java/pl/morph/ai/snake/element/SnakeTest.java`

**Step 1: Write failing test for vision precision**

Add to `SnakeTest.java`:

```java
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
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SnakeTest#lookInDirectionReturnsDistinctDistancesForDifferentPositions -pl .`
Expected: FAIL — body distance rounds to 0.3 instead of 0.333

**Step 3: Fix lookInDirection — remove BigDecimal rounding**

In `Snake.java`, replace the body of `lookInDirection()` (lines 522-558):

```java
double[] lookInDirection(int[] XY) {
    double look[] = new double[look_count];

    int X = XY[0];
    int Y = XY[1];

    int head_x = x[0];
    int head_y = y[0];

    float distance = 0;
    boolean foodFound = false;
    boolean bodyFound = false;
    head_x += X;
    head_y += Y;
    distance += 1;
    while (!wallCollide(head_x, head_y)) {
        if (!foodFound && foodCollide(head_x, head_y)) {
            foodFound = true;
            look[0] = 1.0 / distance;
        }
        if (!bodyFound && bodyCollide(head_x, head_y)) {
            bodyFound = true;
            look[1] = 1.0 / distance;
        }

        head_x += X;
        head_y += Y;
        distance += 1;
    }

    look[2] = 1.0 / distance;

    return look;
}
```

This removes all BigDecimal usage and changes food from binary to 1/distance.

**Step 4: Remove unused BigDecimal import**

In `Snake.java`, remove these imports if no longer used elsewhere:
```java
import java.math.BigDecimal;
import java.math.RoundingMode;
```

**Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=SnakeTest#lookInDirectionReturnsDistinctDistancesForDifferentPositions -pl .`
Expected: PASS

**Step 6: Run all SnakeTest tests**

Run: `mvn test -Dtest=SnakeTest -pl .`
Expected: All PASS

**Step 7: Commit**

```bash
git add src/main/java/pl/morph/ai/snake/element/Snake.java src/test/java/pl/morph/ai/snake/element/SnakeTest.java
git commit -m "Fix vision precision: remove BigDecimal rounding, add food distance"
```

---

### Task 2: Reshape Fitness Function

**Files:**
- Modify: `src/main/java/pl/morph/ai/snake/element/Snake.java:377-387` (calculateFitness)
- Test: `src/test/java/pl/morph/ai/snake/element/SnakeTest.java`

**Step 1: Write failing test for quadratic fitness scaling**

Add to `SnakeTest.java`:

```java
@Test
void calculateFitnessScalesQuadraticallyAboveScore10() {
    // Create two snakes with same lifetime but different high scores
    Snake s1 = new Snake(BOARD_W, BOARD_H, DELAY, false, null, DOT_SIZE, Collections.<Wall>emptyList());
    s1.setStartingPosition(s1.new XY(5, 5));
    s1.spawnApple();

    Snake s2 = new Snake(BOARD_W, BOARD_H, DELAY, false, null, DOT_SIZE, Collections.<Wall>emptyList());
    s2.setStartingPosition(s2.new XY(5, 5));
    s2.spawnApple();

    // Both snakes get same lifetime (20 moves)
    for (int i = 0; i < 20; i++) {
        s1.direction = Direction.RIGHT;
        s1.move();
        s2.direction = Direction.RIGHT;
        s2.move();
    }

    // Give s1 score=50 and s2 score=100 by feeding apples
    for (int i = 0; i < 50; i++) {
        Apple a = new Apple(s1.getX()[0], s1.getY()[0]);
        s1.setAppleToEat(a);
        s1.checkApple();
    }
    for (int i = 0; i < 100; i++) {
        Apple a = new Apple(s2.getX()[0], s2.getY()[0]);
        s2.setAppleToEat(a);
        s2.checkApple();
    }

    s1.calculateFitness();
    s2.calculateFitness();

    // With quadratic scaling, s2 (score=100) should be much more than 2x s1 (score=50)
    // score-9 ratio: (91)^2 / (41)^2 = 8281/1681 ≈ 4.9x for the main term
    double ratio = s2.getFitness() / s1.getFitness();
    assertTrue(ratio > 3.0,
        "Score 100 fitness should be >3x score 50 fitness (quadratic scaling), got ratio " + ratio);
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SnakeTest#calculateFitnessScalesQuadraticallyAboveScore10 -pl .`
Expected: FAIL — with linear scaling the ratio would be ~2x, not >3x

**Step 3: Update calculateFitness**

In `Snake.java`, replace `calculateFitness()` (lines 377-387):

```java
public void calculateFitness() {
    int score = snakeScore.getScore();
    if (score < 10) {
        fitness = floor(lifetime * lifetime) * pow(2, score);
    } else {
        fitness = floor(lifetime * lifetime) * pow(2, 10) * pow(score - 9, 2);
    }
    fitness += pow(score, 4) * 500;
    setHighestFitness();
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=SnakeTest#calculateFitnessScalesQuadraticallyAboveScore10 -pl .`
Expected: PASS

**Step 5: Run all SnakeTest tests**

Run: `mvn test -Dtest=SnakeTest -pl .`
Expected: All PASS (including the existing `calculateFitnessReturnsHigherFitnessForHigherScore`)

**Step 6: Commit**

```bash
git add src/main/java/pl/morph/ai/snake/element/Snake.java src/test/java/pl/morph/ai/snake/element/SnakeTest.java
git commit -m "Reshape fitness: quadratic scaling above score 10, score^4 bonus"
```

---

### Task 3: Scale Time Budget With Snake Length

**Files:**
- Modify: `src/main/java/pl/morph/ai/snake/element/Snake.java:371-375` (increaseLifeSpan)
- Test: `src/test/java/pl/morph/ai/snake/element/SnakeTest.java`

**Step 1: Write failing test for time budget scaling**

Add to `SnakeTest.java`:

```java
@Test
void increaseLifeSpanScalesWithLength() {
    // Board 200x200, dotSize 10 -> maxLife = (200/10)*15 = 300
    // lifeForApple = 300
    Snake s = new Snake(BOARD_W, BOARD_H, DELAY, false, null, DOT_SIZE, Collections.<Wall>emptyList());
    s.setStartingPosition(s.new XY(5, 5));
    s.spawnApple();

    // Set length to 100 -> effectiveMax = 300 + 100*2 = 500
    s.setLength(100);

    // Drain timeLeft to 0 by moving
    // Instead, we can call increaseLifeSpan multiple times
    // First call should increase timeLeft
    // Move to reduce timeLeft, then increase
    for (int i = 0; i < 350; i++) {
        s.direction = Direction.RIGHT;
        s.move();
        if (!s.inGame) break;
    }

    // Snake should still be alive because increaseLifeSpan with length=100
    // allows timeLeft up to 500 (300 + 100*2)
    // Actually, timeLeft starts at 300 and decreases by 1 each move
    // After 300 moves, timeLeft = 0 and snake dies
    // The fix needs to be tested differently

    // Better approach: directly test increaseLifeSpan behavior
    Snake s2 = new Snake(BOARD_W, BOARD_H, DELAY, false, null, DOT_SIZE, Collections.<Wall>emptyList());
    s2.setStartingPosition(s2.new XY(5, 5));
    s2.spawnApple();
    s2.setLength(100);

    // Eat an apple to trigger increaseLifeSpan when timeLeft is near max
    // Set apple on head
    Apple a = new Apple(s2.getX()[0], s2.getY()[0]);
    s2.setAppleToEat(a);

    // Move once to consume some time, then eat
    s2.direction = Direction.RIGHT;
    s2.move(); // timeLeft goes from 300 to 299

    // Place apple on new head position to eat
    a = new Apple(s2.getX()[0], s2.getY()[0]);
    s2.setAppleToEat(a);
    s2.checkApple(); // triggers increaseLifeSpan

    // With length 101 now (was 100, +1 from eating), effectiveMax = 300 + 101*2 = 502
    // timeLeft was 299, after lifeForApple (+300) = 599, capped to 502
    // Without the fix, timeLeft would stay at 299 (already < 300 = maxLife, so +300 = 599, capped at 300)
    // Actually without fix: timeLeft(299) < maxLife(300) -> timeLeft = 299 + 300 = 599 (no cap in old code!)
    // Hmm, old code just does: if(timeLeft < maxLife) timeLeft += lifeForApple
    // So 299 < 300 -> timeLeft = 299+300 = 599. No cap applied!
    // The issue is the CAP, not the ADD. Old code allows overshooting maxLife.
    // The real problem is: after many apples, timeLeft stays high but capped by the < maxLife guard.
    // If timeLeft >= maxLife (300), increaseLifeSpan does nothing.
    // So once you have 300+ timeLeft, eating gives no time bonus.

    // Let's test: when timeLeft >= maxLife but < effectiveMax, eating should still add time
    Snake s3 = new Snake(BOARD_W, BOARD_H, DELAY, false, null, DOT_SIZE, Collections.<Wall>emptyList());
    s3.setStartingPosition(s3.new XY(5, 5));
    s3.spawnApple();
    s3.setLength(100);

    // Don't move - timeLeft = 300 = maxLife
    // Old code: 300 < 300 is false -> no time added
    // New code: effectiveMax = 300 + 100*2 = 500, 300 < 500 -> adds time, caps at 500
    Apple a3 = new Apple(s3.getX()[0], s3.getY()[0]);
    s3.setAppleToEat(a3);
    s3.checkApple(); // increaseLifeSpan called

    // With the fix, timeLeft should be > 300 (got the bonus)
    // Without the fix, timeLeft stays at 300
    // We can't directly access timeLeft, but we can verify the snake survives longer
    // Count how many moves until death
    int moves = 0;
    while (s3.inGame && moves < 1000) {
        s3.direction = Direction.RIGHT;
        s3.move();
        moves++;
    }
    // With fix: should survive ~500 moves (effectiveMax after eating)
    // Without fix: should survive ~300 moves
    assertTrue(moves > 350,
        "Snake with length 100 should survive >350 moves after eating, got " + moves);
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SnakeTest#increaseLifeSpanScalesWithLength -pl .`
Expected: FAIL — snake dies around 300 moves because timeLeft was already at maxLife

**Step 3: Update increaseLifeSpan**

In `Snake.java`, replace `increaseLifeSpan()` (lines 371-375):

```java
public void increaseLifeSpan() {
    long effectiveMax = maxLife + length * 2;
    if (timeLeft < effectiveMax) {
        timeLeft += lifeForApple;
        if (timeLeft > effectiveMax) {
            timeLeft = effectiveMax;
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=SnakeTest#increaseLifeSpanScalesWithLength -pl .`
Expected: PASS

**Step 5: Run full test suite**

Run: `mvn test -pl .`
Expected: All tests PASS

**Step 6: Commit**

```bash
git add src/main/java/pl/morph/ai/snake/element/Snake.java src/test/java/pl/morph/ai/snake/element/SnakeTest.java
git commit -m "Scale time budget with snake length for longer survival at high scores"
```

---

### Task 4: Final Verification

**Step 1: Run full test suite**

Run: `mvn test -pl .`
Expected: All tests PASS

**Step 2: Build the JAR**

Run: `mvn clean package -pl .`
Expected: BUILD SUCCESS

**Step 3: Verify no BigDecimal imports remain in Snake.java**

Check that `import java.math.BigDecimal` and `import java.math.RoundingMode` are removed from `Snake.java` (they should have no remaining usages).

**Step 4: Commit cleanup if needed**

```bash
git add src/main/java/pl/morph/ai/snake/element/Snake.java
git commit -m "Remove unused BigDecimal imports"
```
