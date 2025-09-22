package sim;

import java.util.Random;

/**
 * Provides deterministic random number generation for reproducible simulations.
 * Uses ThreadLocal Random with seeded generation to ensure consistent behavior across runs.
 */
public class Determinism {
    private static final ThreadLocal<Random> RANDOM = new ThreadLocal<>();
    private static long globalSeed = System.currentTimeMillis();
    
    /**
     * Set the global seed for deterministic random number generation
     */
    public static void setSeed(long seed) {
        globalSeed = seed;
        RANDOM.set(new Random(seed));
    }
    
    /**
     * Get the current ThreadLocal Random instance, creating one if needed
     */
    private static Random getRandom() {
        Random random = RANDOM.get();
        if (random == null) {
            random = new Random(globalSeed);
            RANDOM.set(random);
        }
        return random;
    }
    
    /**
     * Generate a random boolean with the given probability
     */
    public static boolean chance(double p) {
        return getRandom().nextDouble() < p;
    }
    
    /**
     * Generate a random integer with jitter between min and max (inclusive)
     */
    public static int jitter(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("minInclusive must be <= maxInclusive");
        }
        if (minInclusive == maxInclusive) {
            return minInclusive;
        }
        return minInclusive + getRandom().nextInt(maxInclusive - minInclusive + 1);
    }
    
    /**
     * Generate a random integer between 0 (inclusive) and bound (exclusive)
     */
    public static int nextInt(int bound) {
        return getRandom().nextInt(bound);
    }
    
    /**
     * Generate a random integer between min (inclusive) and max (exclusive)
     */
    public static int nextInt(int min, int max) {
        return min + getRandom().nextInt(max - min);
    }
    
    /**
     * Generate a random double between 0.0 (inclusive) and 1.0 (exclusive)
     */
    public static double nextDouble() {
        return getRandom().nextDouble();
    }
    
    /**
     * Generate a random boolean
     */
    public static boolean nextBoolean() {
        return getRandom().nextBoolean();
    }
    
    /**
     * Select a random element from an array
     */
    public static <T> T selectRandom(T[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException("Cannot select from empty array");
        }
        return array[nextInt(array.length)];
    }
    
    /**
     * Select a random element from a list
     */
    public static <T> T selectRandom(java.util.List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Cannot select from empty list");
        }
        return list.get(nextInt(list.size()));
    }
    
    /**
     * Shuffle a list in place
     */
    public static <T> void shuffle(java.util.List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }
    
    /**
     * Generate a random election timeout for Raft (typically 150-300ms)
     */
    public static int randomElectionTimeout() {
        return jitter(150, 300);
    }
    
    /**
     * Generate a random heartbeat interval for Raft (typically 50-100ms)
     */
    public static int randomHeartbeatInterval() {
        return jitter(50, 100);
    }
    
    /**
     * Get the current global seed
     */
    public static long getGlobalSeed() {
        return globalSeed;
    }
    
    /**
     * Reset the ThreadLocal Random to the current global seed
     */
    public static void reset() {
        RANDOM.set(new Random(globalSeed));
    }
}

