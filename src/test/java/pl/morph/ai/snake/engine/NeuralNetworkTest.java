package pl.morph.ai.snake.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NeuralNetworkTest {

    private NeuralNetwork createNetwork() {
        return new NeuralNetwork(26, 24, 3, 2);
    }

    @Test
    void constructorCreatesCorrectWeightMatrixDimensions() {
        NeuralNetwork nn = createNetwork();
        // hLayers+1 = 3 weight matrices
        assertEquals(3, nn.weights.length);
        // First: hNodes x (iNodes+1) = 24 x 27
        assertEquals(24, nn.weights[0].rows);
        assertEquals(27, nn.weights[0].cols);
        // Middle: hNodes x (hNodes+1) = 24 x 25
        assertEquals(24, nn.weights[1].rows);
        assertEquals(25, nn.weights[1].cols);
        // Last: oNodes x (hNodes+1) = 3 x 25
        assertEquals(3, nn.weights[2].rows);
        assertEquals(25, nn.weights[2].cols);
    }

    @Test
    void outputReturnsArrayOfCorrectSize() {
        NeuralNetwork nn = createNetwork();
        double[] input = new double[26];
        double[] output = nn.output(input);
        assertEquals(3, output.length);
    }

    @Test
    void outputSoftmaxValuesSumToOne() {
        NeuralNetwork nn = createNetwork();
        double[] input = new double[26];
        for (int i = 0; i < input.length; i++) {
            input[i] = Math.random();
        }
        double[] output = nn.output(input);

        double sum = 0;
        for (double v : output) {
            assertTrue(v >= 0, "softmax output should be non-negative");
            sum += v;
        }
        assertEquals(1.0, sum, 1e-6);
    }

    @Test
    void mutateChangesWeights() {
        NeuralNetwork nn = createNetwork();
        NeuralNetwork original = nn.clone();
        nn.mutate(1.0); // 100% mutation rate

        boolean anyChanged = false;
        for (int w = 0; w < nn.weights.length; w++) {
            for (int i = 0; i < nn.weights[w].rows; i++) {
                for (int j = 0; j < nn.weights[w].cols; j++) {
                    if (nn.weights[w].matrix[i][j] != original.weights[w].matrix[i][j]) {
                        anyChanged = true;
                    }
                }
            }
        }
        assertTrue(anyChanged, "mutate should change at least some weights");
    }

    @Test
    void crossoverProducesChildWithMixedWeights() {
        NeuralNetwork a = createNetwork();
        NeuralNetwork b = createNetwork();
        NeuralNetwork child = a.crossover(b);

        assertEquals(a.weights.length, child.weights.length);
        for (int w = 0; w < child.weights.length; w++) {
            assertEquals(a.weights[w].rows, child.weights[w].rows);
            assertEquals(a.weights[w].cols, child.weights[w].cols);
        }
    }

    @Test
    void cloneProducesIndependentCopy() {
        NeuralNetwork nn = createNetwork();
        NeuralNetwork cloned = nn.clone();

        // Same structure
        assertEquals(nn.weights.length, cloned.weights.length);

        // Same values
        for (int w = 0; w < nn.weights.length; w++) {
            for (int i = 0; i < nn.weights[w].rows; i++) {
                for (int j = 0; j < nn.weights[w].cols; j++) {
                    assertEquals(nn.weights[w].matrix[i][j], cloned.weights[w].matrix[i][j]);
                }
            }
        }

        // Independent
        cloned.weights[0].matrix[0][0] = 999;
        assertNotEquals(999, nn.weights[0].matrix[0][0]);
    }
}
