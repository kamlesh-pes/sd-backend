package com.sahastra.backend.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight load-test metrics collector for scenario-level latency tracking.
 * This is designed for Java-based baseline checks and automated performance regressions,
 * without requiring external benchmarking tooling in the local build pipeline.
 */
public class LoadTestMetrics {

    private final String scenario;
    private final List<Long> latencies = new CopyOnWriteArrayList<>();

    public LoadTestMetrics(String scenario) {
        this.scenario = scenario == null || scenario.isBlank() ? "unknown" : scenario;
    }

    public void recordLatency(long latencyMs) {
        if (latencyMs < 0) {
            throw new IllegalArgumentException("Latency must be non-negative");
        }
        latencies.add(latencyMs);
    }

    public String getScenario() {
        return scenario;
    }

    public int getSampleCount() {
        return latencies.size();
    }

    public long getMinMs() {
        return latencies.stream().mapToLong(Long::longValue).min().orElse(0L);
    }

    public long getMaxMs() {
        return latencies.stream().mapToLong(Long::longValue).max().orElse(0L);
    }

    public double getAverageMs() {
        if (latencies.isEmpty()) {
            return 0.0;
        }

        return latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    public long getP50Ms() {
        return percentile(50);
    }

    public long getP95Ms() {
        return percentile(95);
    }

    public long getP99Ms() {
        return percentile(99);
    }

    public List<Long> snapshotLatencies() {
        return List.copyOf(latencies);
    }

    private long percentile(int percentile) {
        if (latencies.isEmpty()) {
            return 0L;
        }

        List<Long> sorted = new ArrayList<>(latencies);
        sorted.sort(Comparator.naturalOrder());

        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }
}
