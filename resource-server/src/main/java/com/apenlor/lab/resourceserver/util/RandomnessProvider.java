package com.apenlor.lab.resourceserver.util;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * A component that provides randomness for the chaos demo.
 */
@Component
@Profile("chaos")
public class RandomnessProvider {
    private final Random random = new Random();

    /**
     * Returns a pseudorandom, uniformly distributed int value between 0 (inclusive)
     * and the specified value (exclusive).
     *
     * @param bound the upper bound (exclusive). Must be positive.
     * @return the next pseudorandom int value from this random number generator's sequence
     */
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
}