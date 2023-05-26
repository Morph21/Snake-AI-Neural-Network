package pl.morph.ai.snake.element;

import pl.morph.ai.snake.page.Board;

import java.util.ArrayList;
import java.util.List;

public class WallManager {
    public static List<Wall> prepareWalls(int dotSize) {
        List<Wall> walls = new ArrayList<>();

        int rows = Board.B_HEIGHT / dotSize;
        int columns = Board.B_WIDTH / dotSize;

//        walls.addAll(addInLine(-1 * dotSize, ((rows /2)-1) * dotSize, dotSize, 10, 1));
//        walls.addAll(addInLine((columns-11) * dotSize, ((rows /2)-1) * dotSize, dotSize, 10, 1));




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
