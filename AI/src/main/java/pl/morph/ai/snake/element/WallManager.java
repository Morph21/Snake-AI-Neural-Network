package pl.morph.ai.snake.element;

import java.util.ArrayList;
import java.util.List;

public class WallManager {
    public static List<Wall> prepareWalls(int dotSize) {
        List<Wall> walls = new ArrayList<>();


        walls.addAll(addInLine(120, 120, dotSize, 15, 1));

        walls.addAll(addInLine(120, 420, dotSize, 15, 1));

        return walls;

    }

    private static List<Wall> addInLine(int x, int y, int dotSize, int howMuchSteps, int rows) {
        List<Wall> walls = new ArrayList<>();
        for (int i = 1; i <= howMuchSteps; i++) {
            for (int j = 1; j <= rows; j++) {
                walls.add(new Wall(x + dotSize * i, y + dotSize * j, dotSize));
            }
        }
        return walls;
    }


}
