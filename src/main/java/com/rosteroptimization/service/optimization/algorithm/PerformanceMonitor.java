package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.service.optimization.model.RosterPlan;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class PerformanceMonitor {

    private final AtomicLong totalOptimizations = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicInteger feasibleSolutions = new AtomicInteger(0);
    private final AtomicLong totalGenerations = new AtomicLong(0);

    private volatile long currentOptimizationStart = 0;
    private volatile PerformanceMetrics currentMetrics;

    public void startOptimization() {
        currentOptimizationStart = System.currentTimeMillis();
        currentMetrics = new PerformanceMetrics();
        log.debug("Performance monitoring started");
    }

    public void endOptimization(RosterPlan result) {
        long executionTime = System.currentTimeMillis() - currentOptimizationStart;

        totalOptimizations.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);

        if (result.isFeasible()) {
            feasibleSolutions.incrementAndGet();
        }

        if (result.getAlgorithmMetadata() != null) {
            Object generations = result.getAlgorithmMetadata().get("finalGeneration");
            if (generations instanceof Integer) {
                totalGenerations.addAndGet((Integer) generations);
            }
        }

        currentMetrics.executionTimeMs = executionTime;
        currentMetrics.fitnessScore = result.getFitnessScore();
        currentMetrics.feasible = result.isFeasible();
        currentMetrics.hardViolations = result.getHardConstraintViolations();
        currentMetrics.softViolations = result.getSoftConstraintViolations();

        log.info("Optimization completed: {}ms, fitness={:.2f}, feasible={}, violations={}+{}",
                executionTime, result.getFitnessScore(), result.isFeasible(),
                result.getHardConstraintViolations(), result.getSoftConstraintViolations());
    }

    public void recordGeneration(int generation, double bestFitness, int hardViolations) {
        if (currentMetrics != null) {
            currentMetrics.currentGeneration = generation;
            currentMetrics.currentBestFitness = bestFitness;
            currentMetrics.currentHardViolations = hardViolations;
        }
    }

    public PerformanceMetrics getCurrentMetrics() {
        return currentMetrics != null ? currentMetrics.copy() : null;
    }

    public OverallStatistics getOverallStatistics() {
        long optimizations = totalOptimizations.get();
        return new OverallStatistics(
                optimizations,
                optimizations > 0 ? totalExecutionTime.get() / optimizations : 0,
                optimizations > 0 ? (double) feasibleSolutions.get() / optimizations : 0.0,
                optimizations > 0 ? totalGenerations.get() / optimizations : 0,
                totalExecutionTime.get()
        );
    }

    public void reset() {
        totalOptimizations.set(0);
        totalExecutionTime.set(0);
        feasibleSolutions.set(0);
        totalGenerations.set(0);
        currentOptimizationStart = 0;
        currentMetrics = null;
        log.debug("Performance monitor reset");
    }

    @Data
    public static class PerformanceMetrics {
        private long executionTimeMs;
        private double fitnessScore;
        private boolean feasible;
        private int hardViolations;
        private int softViolations;
        private int currentGeneration;
        private double currentBestFitness;
        private int currentHardViolations;

        public PerformanceMetrics copy() {
            PerformanceMetrics copy = new PerformanceMetrics();
            copy.executionTimeMs = this.executionTimeMs;
            copy.fitnessScore = this.fitnessScore;
            copy.feasible = this.feasible;
            copy.hardViolations = this.hardViolations;
            copy.softViolations = this.softViolations;
            copy.currentGeneration = this.currentGeneration;
            copy.currentBestFitness = this.currentBestFitness;
            copy.currentHardViolations = this.currentHardViolations;
            return copy;
        }
    }

    public record OverallStatistics(
            long totalOptimizations,
            long averageExecutionTimeMs,
            double feasibilityRate,
            long averageGenerations,
            long totalExecutionTimeMs
    ) {}
}