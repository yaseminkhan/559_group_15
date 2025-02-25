package com.server;  // ADD THIS LINE

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Words {
    // note: implementation only works with one word entries
    private static final List<String> words = Arrays.asList(
        "Sun", "Tree", "House", "Cat", "Dog", "Fish", "Car", "Balloon", "Book", "Moon",
        "Airplane", "Elephant", "Bicycle", "Rainbow", "Guitar", "Pumpkin", "Turtle", 
        "Ladder", "Basketball", "Telescope", "Rollercoaster", "Mermaid", "Kangaroo", 
        "Microscope", "Castle", "Volcano", "Octopus", "Dinosaur", "Robot", "Treasure", 
        "Jungle", "Island", "Fireworks", "Running", "Jumping", "Swimming", "Dancing", 
        "Flying", "Superhero", "Wizard", "Movie", "Gargoyle", "Pyramid", "Metronome", 
        "Spaceship", "Magic", "Laptop", "Smartphone", "Television", "Headphones", 
        "Camera", "Halloween", "Lightning", "Tornado", "Snowman", "Waterfall", "Desert", 
        "Umbrella", "Accordion", "Parachute", "Chandelier"
    );

    private static final Random random = new Random();

    // Method to get a single random word
    public static String getRandomWord() {
        return words.get(random.nextInt(words.size()));
    }

    // Method to get 4 random words for word selection
    public static List<String> getRandomWordChoices() {
        return random.ints(0, words.size())
                     .distinct()
                     .limit(4)
                     .mapToObj(words::get)
                     .toList();
    }
}