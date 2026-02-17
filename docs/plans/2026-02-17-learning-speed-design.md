# Learning Speed Improvement Design

## Problem

Snake AI plateaus at 100-200 apples (25-50% of 20x20 board) around generation 100-500, unable to push higher.

## Root Causes

1. **Vision precision loss**: `BigDecimal.setScale(1)` rounds distances so that 3 and 4 both become 0.3, 5-9 all become 0.2. The snake cannot distinguish nearby vs far obstacles — critical when the board is crowded.
2. **Fitness function goes flat**: Above score 10, fitness grows linearly with score while `lifetime²` dominates. Almost zero evolutionary pressure to improve at high scores.
3. **Food distance invisible**: Food vision is binary (0/1), so the snake knows direction but not distance to food.
4. **Time budget caps out**: 300-step max doesn't scale with snake length. A 150-body snake needs far more steps to navigate than a 5-body snake.

## Changes

### 1. Vision Precision (Snake.lookInDirection)

Replace `BigDecimal` rounding with raw floating point for body and wall distances:

```java
// Before
look[1] = new BigDecimal(1 / distance).setScale(1, RoundingMode.HALF_UP).doubleValue();
look[2] = new BigDecimal(1 / distance).setScale(1, RoundingMode.HALF_UP).doubleValue();

// After
look[1] = 1.0 / distance;
look[2] = 1.0 / distance;
```

### 2. Food Distance (Snake.lookInDirection)

Change food detection from binary to distance-based:

```java
// Before
look[0] = 1;

// After
look[0] = 1.0 / distance;
```

### 3. Fitness Function (Snake.calculateFitness)

Keep exponential growth longer, switch to quadratic above score 10, stronger score bonus:

```java
// Before
if (score < 10) {
    fitness = floor(lifetime * lifetime) * pow(2, score);
} else {
    fitness = floor(lifetime * lifetime) * pow(2, 10) * (score - 9);
}
fitness += pow(score, 3) * 1000;

// After
if (score < 10) {
    fitness = floor(lifetime * lifetime) * pow(2, score);
} else {
    fitness = floor(lifetime * lifetime) * pow(2, 10) * pow(score - 9, 2);
}
fitness += pow(score, 4) * 500;
```

### 4. Time Budget Scaling (Snake.increaseLifeSpan)

Scale step cap with snake length:

```java
// Before
if (timeLeft < maxLife) {
    timeLeft += lifeForApple;
}

// After
long effectiveMax = maxLife + length * 2;
if (timeLeft < effectiveMax) {
    timeLeft += lifeForApple;
    if (timeLeft > effectiveMax) {
        timeLeft = effectiveMax;
    }
}
```

## Files Changed

- `Snake.java`: lookInDirection(), calculateFitness(), increaseLifeSpan()
- No new files, no API changes, no serialization changes

## Compatibility

- Input count remains 26 — saved .ser files still compatible
- Network architecture unchanged
- All existing hotkeys work the same
