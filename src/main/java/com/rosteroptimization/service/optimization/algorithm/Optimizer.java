package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.service.optimization.model.OptimizationRequest;
import com.rosteroptimization.service.optimization.model.RosterPlan;

import java.util.Map;

/**
 * Strategy pattern interface for different optimization algorithms
 * Each algorithm implementation should provide this interface
 */
public interface Optimizer {

    /**
     * Main optimization method that generates roster plan
     *
     * @param request Contains all input data and constraints
     * @return Optimized roster plan
     * @throws OptimizationException if optimization fails
     */
    RosterPlan optimize(OptimizationRequest request) throws OptimizationException;

    /**
     * Get unique algorithm name/identifier
     *
     * @return Algorithm name (e.g., "GENETIC_ALGORITHM", "SIMULATED_ANNEALING")
     */
    String getAlgorithmName();

    /**
     * Get human-readable algorithm description
     *
     * @return Algorithm description
     */
    String getAlgorithmDescription();

    /**
     * Get default algorithm-specific parameters
     * These can be overridden in OptimizationRequest.algorithmParameters
     *
     * @return Map of parameter names and default values
     */
    Map<String, Object> getDefaultParameters();

    /**
     * Validate if the algorithm can handle the given request
     *
     * @param request Optimization request to validate
     * @throws OptimizationException if request is not valid for this algorithm
     */
    void validateRequest(OptimizationRequest request) throws OptimizationException;

    /**
     * Check if algorithm supports parallel processing
     *
     * @return true if algorithm can use multiple threads
     */
    boolean supportsParallelProcessing();

    /**
     * Get estimated execution time in milliseconds
     * This is a rough estimate based on input size
     *
     * @param request Optimization request
     * @return Estimated execution time in milliseconds
     */
    long getEstimatedExecutionTime(OptimizationRequest request);

    /**
     * Get algorithm-specific configuration info for UI
     *
     * @return Configuration parameters that can be shown/edited in UI
     */
    Map<String, ParameterInfo> getConfigurableParameters();

    /**
     * Cancel ongoing optimization (if supported)
     *
     * @return true if cancellation was successful
     */
    default boolean cancelOptimization() {
        return false; // Default implementation - override if cancellation is supported
    }

    /**
     * Get optimization progress (if supported)
     *
     * @return Progress percentage (0-100), -1 if not supported
     */
    default int getOptimizationProgress() {
        return -1; // Default implementation - override if progress tracking is supported
    }

    /**
     * Parameter information for UI configuration
     */
    class ParameterInfo {
        private final String name;
        private final String description;
        private final Class<?> type;
        private final Object defaultValue;
        private final Object minValue;
        private final Object maxValue;

        public ParameterInfo(String name, String description, Class<?> type, Object defaultValue) {
            this(name, description, type, defaultValue, null, null);
        }

        public ParameterInfo(String name, String description, Class<?> type, Object defaultValue,
                             Object minValue, Object maxValue) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Class<?> getType() { return type; }
        public Object getDefaultValue() { return defaultValue; }
        public Object getMinValue() { return minValue; }
        public Object getMaxValue() { return maxValue; }
    }
}

