package com.server;  // ADD THIS LINE

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class Words {
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
    private static final int HISTORY_SIZE = 10; // Adjust based on preference
    private static final Queue<String> wordHistory = new LinkedList<>();

    // Method to get a single random word
    public static String getRandomWord() {
        return words.get(random.nextInt(words.size()));
    }

    // Method to get 4 random words for word selection with history tracking
    public static List<String> getRandomWordChoices() {
        List<String> availableWords = new ArrayList<>(words);
        
        // Remove recently used words from available choices
        availableWords.removeAll(wordHistory);

        // If we have fewer available words than needed, reset history
        if (availableWords.size() < 4) {
            wordHistory.clear(); // Clear history to allow reuse
            availableWords = new ArrayList<>(words); // Reset available words
        }

        // Select 4 random words
        List<String> selectedWords = new ArrayList<>();
        Collections.shuffle(availableWords);
        selectedWords.addAll(availableWords.subList(0, Math.min(4, availableWords.size())));

        // Update history
        wordHistory.addAll(selectedWords);
        while (wordHistory.size() > HISTORY_SIZE) {
            wordHistory.poll(); // Remove oldest words to maintain history size
        }

        return selectedWords;
    }
}