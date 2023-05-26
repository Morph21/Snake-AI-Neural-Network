package pl.morph.ai.snake.element;

public class Thinking {
    double[] see;
    Direction direction;
    double[] input;
    double[] output;

    public double[] getSee() {
        return see;
    }

    public void setSee(double[] see) {
        this.see = see;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public double[] getInput() {
        return input;
    }

    public void setInput(double[] input) {
        this.input = input;
    }

    public double[] getOutput() {
        return output;
    }

    public void setOutput(double[] output) {
        this.output = output;
    }
}
