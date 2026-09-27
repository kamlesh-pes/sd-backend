package com.sahastra.backend.performance;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadTestMetricsTest {

    @Test
    void recordsLatencyPercentiles() {
        LoadTestMetrics metrics = new LoadTestMetrics("search");

        long[] values = {50L, 70L, 90L, 110L, 130L, 150L, 200L, 260L, 340L, 500L, 700L, 900L};
        for (long value : values) {
            metrics.recordLatency(value);
        }

        assertEquals(12, metrics.getSampleCount());
        assertEquals(150L, metrics.getP50Ms());
        assertEquals(900L, metrics.getP95Ms());
        assertEquals(900L, metrics.getP99Ms());
        assertEquals(50L, metrics.getMinMs());
        assertEquals(900L, metrics.getMaxMs());
    }

    @Test
    void handlesConcurrentSamples() throws InterruptedException {
        LoadTestMetrics metrics = new LoadTestMetrics("checkout");
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    long latency = 25L + (ThreadLocalRandomHolder.random() % 120L);
                    metrics.recordLatency(latency);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
        }

        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(100, metrics.getSampleCount());
        assertTrue(metrics.getP95Ms() <= 145L);
    }

    private static final class ThreadLocalRandomHolder {
        private static final java.util.concurrent.ThreadLocalRandom RANDOM = java.util.concurrent.ThreadLocalRandom.current();

        static long random() {
            return RANDOM.nextLong(120L);
        }
    }
}
