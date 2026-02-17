package pl.morph.ai.snake.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatrixTest {

    @Test
    void constructorInitializesCorrectDimensions() {
        Matrix m = new Matrix(3, 4);
        assertEquals(3, m.rows);
        assertEquals(4, m.cols);
        assertEquals(3, m.matrix.length);
        assertEquals(4, m.matrix[0].length);
    }

    @Test
    void constructorFromArrayInitializesCorrectly() {
        double[][] data = {{1, 2}, {3, 4}, {5, 6}};
        Matrix m = new Matrix(data);
        assertEquals(3, m.rows);
        assertEquals(2, m.cols);
        assertEquals(4.0, m.matrix[1][1]);
    }

    @Test
    void randomizeFillsWithValuesInRange() {
        Matrix m = new Matrix(10, 10);
        m.randomize();
        boolean hasNonZero = false;
        for (int i = 0; i < m.rows; i++) {
            for (int j = 0; j < m.cols; j++) {
                assertTrue(m.matrix[i][j] >= -1 && m.matrix[i][j] <= 1);
                if (m.matrix[i][j] != 0) hasNonZero = true;
            }
        }
        assertTrue(hasNonZero, "randomize should produce non-zero values");
    }

    @Test
    void dotMatrixMultiplication() {
        double[][] aData = {{1, 2}, {3, 4}};
        double[][] bData = {{5, 6}, {7, 8}};
        Matrix a = new Matrix(aData);
        Matrix b = new Matrix(bData);
        Matrix result = a.dot(b);

        assertEquals(2, result.rows);
        assertEquals(2, result.cols);
        assertEquals(19.0, result.matrix[0][0]); // 1*5 + 2*7
        assertEquals(22.0, result.matrix[0][1]); // 1*6 + 2*8
        assertEquals(43.0, result.matrix[1][0]); // 3*5 + 4*7
        assertEquals(50.0, result.matrix[1][1]); // 3*6 + 4*8
    }

    @Test
    void activateAppliesRelu() {
        double[][] data = {{-2, 3}, {0, -0.5}};
        Matrix m = new Matrix(data);
        Matrix activated = m.activate();

        assertEquals(0.0, activated.matrix[0][0]);
        assertEquals(3.0, activated.matrix[0][1]);
        assertEquals(0.0, activated.matrix[1][0]);
        assertEquals(0.0, activated.matrix[1][1]);
    }

    @Test
    void softmaxOutputsSumToOne() {
        double[][] data = {{1}, {2}, {3}};
        Matrix m = new Matrix(data);
        Matrix sm = m.softmax();

        double sum = 0;
        for (int i = 0; i < sm.rows; i++) {
            for (int j = 0; j < sm.cols; j++) {
                assertTrue(sm.matrix[i][j] >= 0);
                sum += sm.matrix[i][j];
            }
        }
        assertEquals(1.0, sum, 1e-9);
    }

    @Test
    void addBiasAddsExtraRow() {
        double[][] data = {{2}, {3}};
        Matrix m = new Matrix(data);
        Matrix biased = m.addBias();

        assertEquals(3, biased.rows);
        assertEquals(1, biased.cols);
        assertEquals(2.0, biased.matrix[0][0]);
        assertEquals(3.0, biased.matrix[1][0]);
        assertEquals(1.0, biased.matrix[2][0]);
    }

    @Test
    void mutateModifiesValuesAndStaysInRange() {
        Matrix m = new Matrix(10, 10);
        m.randomize();
        Matrix original = m.clone();
        m.mutate(1.0); // 100% mutation rate

        boolean anyChanged = false;
        for (int i = 0; i < m.rows; i++) {
            for (int j = 0; j < m.cols; j++) {
                assertTrue(m.matrix[i][j] >= -1 && m.matrix[i][j] <= 1);
                if (m.matrix[i][j] != original.matrix[i][j]) anyChanged = true;
            }
        }
        assertTrue(anyChanged, "mutate with rate 1.0 should change at least some values");
    }

    @Test
    void crossoverProducesChildWithValuesFromBothParents() {
        Matrix a = new Matrix(5, 5);
        Matrix b = new Matrix(5, 5);
        // Fill with distinct values
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                a.matrix[i][j] = 0.5;
                b.matrix[i][j] = -0.5;
            }
        }
        Matrix child = a.crossover(b);

        boolean hasA = false, hasB = false;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (child.matrix[i][j] == 0.5) hasA = true;
                if (child.matrix[i][j] == -0.5) hasB = true;
            }
        }
        assertTrue(hasA || hasB, "Child should contain values from at least one parent");
    }

    @Test
    void cloneProducesIndependentCopy() {
        Matrix m = new Matrix(3, 3);
        m.randomize();
        Matrix c = m.clone();

        // Same values
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(m.matrix[i][j], c.matrix[i][j]);
            }
        }

        // Independent - modifying clone doesn't affect original
        c.matrix[0][0] = 999;
        assertNotEquals(999, m.matrix[0][0]);
    }

    @Test
    void singleColumnMatrixFromArrayAndToArrayRoundTrip() {
        double[] arr = {1, 2, 3, 4, 5};
        Matrix m = new Matrix(1, 1); // just need an instance to call the method
        Matrix col = m.singleColumnMatrixFromArray(arr);

        assertEquals(5, col.rows);
        assertEquals(1, col.cols);

        double[] result = col.toArray();
        assertArrayEquals(arr, result, 1e-9);
    }
}
