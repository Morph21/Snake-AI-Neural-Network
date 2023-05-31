# Snake AI

Best snake video 313 out of 396 max points, it's speed up a bit since on normal speed it takes 15 minutes

https://github.com/Morph21/Snake-AI-Neural-Network/assets/38075691/8f953d2a-483f-4f4e-b406-68ba8923b8de


Learning process video with only 100 snakes:

https://github.com/Morph21/Snake-AI-Neural-Network/assets/38075691/55d8d79f-c861-4cd6-9b94-21da6ee2e102


# How it all works

## Natural selection of snakes:
* The population of snakes is 2000
* First generation have they neural network randomized
* After each generation each snake will be scored based on fitness function
* Then snakes are sorted based on fitness
* Only 50% of best snakes will survive
* Best snake won't be crossed over between generations, only mutation will happen for him
* mutation rate is set on 3% that means that 3% of all neural network values will be randomized
* every other snake have 90% chance to cross over with randomized one to change genes
* every 100 generations best snake will populate all population (you could say it will be copied 2000 times)

## Fitness function
I tried few fitness function, this one did work best since fitness sometimes was higher for snakes with lower scores

```
if (snakeScore.getScore() < 10) {
    fitness = floor(lifetime * lifetime) * pow(2, snakeScore.getScore());
} else {
    fitness = floor(lifetime * lifetime);
    fitness *= pow(2, 10);
    fitness *= (snakeScore.getScore() - 9);
}
```

## What snake can see and what is very important

* there are 12 vision inputs of snake
* First 2 are head direction and tail direction
* Snake can see in 3 directions Left, Right, In front of snake
* Each direction takes up 3 vision inputs:
* first is for seeing apples which is binary 1/0 value (I tested 1/distance but it was a lot worse)
* second is distance to snake body if any was find in direction 1/distance rounded to 2 decimal points
* third is distance to walls 1/distance rounded to 2 decimal points

I was experimenting with snake looking behind himself but there wasn't any significant improvements there

### The most important thing with inputs is that you need to think where snake is looking and not where you look. 
### So when snake is going to the right side of monitor and is looking up/front it means that it is looking to your right
### It feels obvious but believe me it isn't and after I changed how snake see things I saw the biggest improvement in learning speed
### head and tail direction is important after some point. Without these inputs my snake best score was 112

## Why snakes have different colours?
Colour of snake is based on some math in matrix (neural network) and it determines the snake colour. So basically in theory you can see how similar to each other snakes are (I wouldn't base any real informations on that :))

# How to run best snake for yourself

* download SnakeAi.jar from release
* download best.ser from release
* run SnakeAi.jar then press "l" (small L on keyboard)
* select best.ser file
* wait until it's loaded
* press "r" to resume snake movements