package com.rosteroptimization.service.optimization.constraint;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the results of constraint evaluation for a roster plan
 */
@Data
@NoArgsConstructor
public class ConstraintEvaluationResult {

    private List<ConstraintViolation> hardViolations = new ArrayList<>();
    private List<ConstraintViolation> softViolations = new ArrayList<>();

    /**
     * Add a hard constraint violation
     */
    public void addHardViolation(String description) {
        hardViolations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, description));
    }

    /**
     * Add a hard constraint violation with penalty
     */
    public void addHardViolation(String description, double penalty) {
        hardViolations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, description, penalty));
    }

    /**
     * Add a soft constraint violation
     */
    public void addSoftViolation(String description) {
        softViolations.add(new ConstraintViolation(ConstraintViolation.Type.SOFT, description));
    }

    /**
     * Add a soft constraint violation with penalty
     */
    public void addSoftViolation(String description, double penalty) {
        softViolations.add(new ConstraintViolation(ConstraintViolation.Type.SOFT, description, penalty));
    }

    /**
     * Get total number of hard violations
     */
    public int getHardViolationCount() {
        return hardViolations.size();
    }

    /**
     * Get total number of soft violations
     */
    public int getSoftViolationCount() {
        return softViolations.size();
    }

    /**
     * Get total number of violations
     */
    public int getTotalViolationCount() {
        return hardViolations.size() + softViolations.size();
    }

    /**
     * Check if solution is feasible (no hard violations)
     */
    public boolean isFeasible() {
        return hardViolations.isEmpty();
    }

    /**
     * Get total penalty score from hard violations
     */
    public double getHardPenaltyScore() {
        return hardViolations.stream()
                .mapToDouble(ConstraintViolation::getPenalty)
                .sum();
    }

    /**
     * Get total penalty score from soft violations
     */
    public double getSoftPenaltyScore() {
        return softViolations.stream()
                .mapToDouble(ConstraintViolation::getPenalty)
                .sum();
    }

    /**
     * Get total penalty score (hard + soft)
     */
    public double getTotalPenaltyScore() {
        return getHardPenaltyScore() + getSoftPenaltyScore();
    }

    /**
     * Get all violations (hard + soft)
     */
    public List<ConstraintViolation> getAllViolations() {
        List<ConstraintViolation> allViolations = new ArrayList<>();
        allViolations.addAll(hardViolations);
        allViolations.addAll(softViolations);
        return allViolations;
    }

    /**
     * Merge another evaluation result into this one
     */
    public void merge(ConstraintEvaluationResult other) {
        this.hardViolations.addAll(other.hardViolations);
        this.softViolations.addAll(other.softViolations);
    }

    /**
     * Clear all violations
     */
    public void clear() {
        hardViolations.clear();
        softViolations.clear();
    }

    @Override
    public String toString() {
        return String.format("ConstraintEvaluationResult{hardViolations=%d, softViolations=%d, feasible=%s, totalPenalty=%.2f}",
                getHardViolationCount(), getSoftViolationCount(), isFeasible(), getTotalPenaltyScore());
    }

    /**
     * Represents a single constraint violation
     */
    @Data
    @NoArgsConstructor
    public static class ConstraintViolation {
        private Type type;
        private String description;
        private double penalty;

        public ConstraintViolation(Type type, String description) {
            this(type, description, type == Type.HARD ? 1000.0 : 1.0); // Default penalties
        }

        public ConstraintViolation(Type type, String description, double penalty) {
            this.type = type;
            this.description = description;
            this.penalty = penalty;
        }

        public enum Type {
            HARD,   // Must be satisfied
            SOFT    // Preferred to be satisfied
        }

        @Override
        public String toString() {
            return String.format("%s: %s (penalty: %.1f)", type, description, penalty);
        }
    }
}