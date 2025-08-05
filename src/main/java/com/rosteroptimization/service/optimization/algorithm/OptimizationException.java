package com.rosteroptimization.service.optimization.algorithm;

/**
 * Exception thrown when optimization fails
 */
public class OptimizationException extends Exception {
    public OptimizationException(String message) {
        super(message);
    }

    public OptimizationException(String message, Throwable cause) {
        super(message, cause);
    }
}