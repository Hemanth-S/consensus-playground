package sim;

import java.util.Random;

/**
 * Provides deterministic random number generation for reproducible simulations.
 * Uses seeded random number generation to ensure consistent behavior across runs.
 */
public class Determinism {
    private final Random random;
    private final long seed;
    
    public Determinism() {
        this(System.currentTimeMillis());
    }
    
    public Determinism(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }
    
    /**
     * Get the seed used for random number generation
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Reset the random number generator to its initial state
     */
    public void reset() {
        random.setSeed(seed);
    }
    
    /**
     * Generate a random integer between 0 (inclusive) and bound (exclusive)
     */
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
    
    /**
     * Generate a random integer between min (inclusive) and max (exclusive)
     */
    public int nextInt(int min, int max) {
        return min + random.nextInt(max - min);
    }
    
    /**
     * Generate a random double between 0.0 (inclusive) and 1.0 (exclusive)
     */
    public double nextDouble() {
        return random.nextDouble();
    }
    
    /**
     * Generate a random boolean
     */
    public boolean nextBoolean() {
        return random.nextBoolean();
    }
    
    /**
     * Determine if a message should be dropped based on the given drop rate
     */
    public boolean shouldDropMessage(double dropRate) {
        return nextDouble() < dropRate;
    }
    
    /**
     * Select a random element from an array
     */
    public <T> T selectRandom(T[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException("Cannot select from empty array");
        }
        return array[nextInt(array.length)];
    }
    
    /**
     * Select a random element from a list
     */
    public <T> T selectRandom(java.util.List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Cannot select from empty list");
        }
        return list.get(nextInt(list.size()));
    }
    
    /**
     * Shuffle a list in place
     */
    public <T> void shuffle(java.util.List<T> list) {
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
    public int randomElectionTimeout() {
        return nextInt(150, 300);
    }
    
    /**
     * Generate a random heartbeat interval for Raft (typically 50-100ms)
     */
    public int randomHeartbeatInterval() {
        return nextInt(50, 100);
    }
    
    @Override
    public String toString() {
        return String.format("Determinism{seed=%d}", seed);
    }
}

