package pl.morph.ai.snake.engine;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.floor;

public class Matrix implements Serializable {
    private static final long serialVersionUID = 568494227179495863L;
    int rows, cols;
    double[][] matrix;

    public Matrix(int r, int c) {
        rows = r;
        cols = c;
        matrix = new double[rows][cols];
    }

    public Matrix(double[][] m) {
        matrix = m;
        rows = matrix.length;
        cols = matrix[0].length;
    }

    Matrix dot(Matrix n) {
        Matrix result = new Matrix(rows, n.cols);

        if (cols == n.rows) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < n.cols; j++) {
                    double sum = 0;
                    for (int k = 0; k < cols; k++) {
                        sum += matrix[i][k] * n.matrix[k][j];
                    }
                    result.matrix[i][j] = sum;
                }
            }
        }
        return result;
    }

    void randomize() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random(-1, 1);
            }
        }
    }

    Matrix singleColumnMatrixFromArray(double[] arr) {
        Matrix n = new Matrix(arr.length, 1);
        for (int i = 0; i < arr.length; i++) {
            n.matrix[i][0] = arr[i];
        }
        return n;
    }

    double[] toArray() {
        double[] arr = new double[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                arr[j + i * cols] = matrix[i][j];
            }
        }
        return arr;
    }

    Matrix addBias() {
        Matrix n = new Matrix(rows + 1, 1);
        for (int i = 0; i < rows; i++) {
            n.matrix[i][0] = matrix[i][0];
        }
        n.matrix[rows][0] = 1;
        return n;
    }

    Matrix activate() {
        Matrix n = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                n.matrix[i][j] = relu(matrix[i][j]);
            }
        }
        return n;
    }

    double relu(double x) {
        return Math.max(0, x);
    }

    Matrix softmax() {
        Matrix n = new Matrix(rows, cols);
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix[i][j] > max) max = matrix[i][j];
            }
        }
        double sum = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                n.matrix[i][j] = Math.exp(matrix[i][j] - max);
                sum += n.matrix[i][j];
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                n.matrix[i][j] /= sum;
            }
        }
        return n;
    }

    void mutate(double mutationRate) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double rand = random(0,1);
                if (rand < mutationRate) {
                    // 10% chance to fully reset weight for exploration
                    if (random(0, 1) < 0.1) {
                        matrix[i][j] = random(-1, 1);
                    } else {
                        matrix[i][j] += randomGaussian() / 5;
                        if (matrix[i][j] > 1) {
                            matrix[i][j] = 1;
                        }
                        if (matrix[i][j] < -1) {
                            matrix[i][j] = -1;
                        }
                    }
                }
            }
        }
    }

    Matrix crossover(Matrix partner) {
        Matrix child = new Matrix(rows, cols);

        int randC = (int) floor(random(0,cols));
        int randR = (int) floor(random(0,rows));

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if ((i < randR) || (i == randR && j <= randC)) {
                    child.matrix[i][j] = matrix[i][j];
                } else {
                    child.matrix[i][j] = partner.matrix[i][j];
                }
            }
        }
        return child;
    }

    public Matrix clone() {
        Matrix clone = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                clone.matrix[i][j] = matrix[i][j];
            }
        }
        return clone;
    }

    private static double randomGaussian() {
        return ThreadLocalRandom.current().nextGaussian();
    }

    public static double random(int from, int to) {
        return from + (to - from) * ThreadLocalRandom.current().nextDouble();
    }

    public static double random(double from, double to) {
        return from + (to - from) * ThreadLocalRandom.current().nextDouble();
    }


}
