package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Task;
import com.rosteroptimization.service.optimization.constraint.ConstraintEvaluator;
import com.rosteroptimization.service.optimization.constraint.ConstraintEvaluationResult;
import com.rosteroptimization.service.optimization.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import lombok.Builder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FitnessCalculator {

    private final ConstraintEvaluator constraintEvaluator;

    public double calculateFitness(Chromosome chromosome, OptimizationRequest request) {
        RosterPlan tempPlan = createTemporaryPlan(chromosome, request);
        ConstraintEvaluationResult evaluation = constraintEvaluator.evaluateRosterPlan(tempPlan);

        return calculateBalancedFitness(chromosome, request, evaluation);
    }

    public double calculateFitnessWithFocus(Chromosome chromosome, OptimizationRequest request,
                                            GeneticAlgorithmOptimizer.Island.IslandType focus) {

        RosterPlan tempPlan = createTemporaryPlan(chromosome, request);
        ConstraintEvaluationResult evaluation = constraintEvaluator.evaluateRosterPlan(tempPlan);

        return switch (focus) {
            case CONSTRAINT_FOCUSED -> calculateConstraintFocusedFitness(chromosome, request, evaluation);
            case QUALITY_FOCUSED -> calculateQualityFocusedFitness(chromosome, request, evaluation);
            case BALANCED -> calculateBalancedFitness(chromosome, request, evaluation);
        };
    }

    private double calculateConstraintFocusedFitness(Chromosome chromosome, OptimizationRequest request,
                                                     ConstraintEvaluationResult evaluation) {
        double baseFitness = 10000.0;

        // Heavy penalties for hard constraint violations
        double hardPenalty = evaluation.getHardViolationCount() * 2000.0;
        baseFitness -= hardPenalty;

        // Light penalties for soft constraints
        double softPenalty = evaluation.getSoftViolationCount() * 25.0;
        baseFitness -= softPenalty;

        // Bonus for feasible solutions
        if (evaluation.isFeasible()) {
            baseFitness += 5000.0;
            baseFitness += calculateTaskCoverageBonus(chromosome, request) * 200;
            baseFitness += calculateBasicFairness(chromosome, request) * 100;
        }

        // Penalty for unassigned high-priority tasks
        baseFitness -= calculateUnassignedTasksPenalty(chromosome, request) * 500;

        return Math.max(0, baseFitness);
    }

    private double calculateQualityFocusedFitness(Chromosome chromosome, OptimizationRequest request,
                                                  ConstraintEvaluationResult evaluation) {
        double baseFitness = 50000.0;

        // Hard violations are critical but not instantly disqualifying
        double hardPenalty = evaluation.getHardViolationCount() * 1000.0;
        baseFitness -= hardPenalty;

        // Moderate penalty for soft violations
        double softPenalty = evaluation.getSoftViolationCount() * 50.0;
        baseFitness -= softPenalty;

        if (evaluation.getHardViolationCount() == 0) {
            // Task coverage excellence (quadratic reward)
            double coverageRate = calculateTaskCoverageRate(chromosome, request);
            baseFitness += Math.pow(coverageRate, 2) * 3000;

            // Advanced fairness scoring
            double fairnessScore = calculateAdvancedFairness(chromosome, request);
            baseFitness += fairnessScore * 2000;

            // Staff utilization optimization
            double utilizationScore = calculateStaffUtilization(chromosome, request);
            baseFitness += utilizationScore * 1500;

            // Pattern compliance bonus
            double patternCompliance = calculatePatternCompliance(chromosome, request);
            baseFitness += patternCompliance * 1000;

            // Priority task satisfaction
            double priorityScore = calculatePriorityTaskSatisfaction(chromosome, request);
            baseFitness += priorityScore * 1200;

            // Workload distribution
            double distributionScore = calculateWorkloadDistribution(chromosome, request);
            baseFitness += distributionScore * 800;

            // Efficiency bonus
            double efficiencyScore = calculateEfficiency(chromosome, request);
            baseFitness += efficiencyScore * 600;
        }

        // Critical issues penalty
        baseFitness -= calculateCriticalIssuesPenalty(chromosome, request);

        return Math.max(0, baseFitness);
    }

    private double calculateBalancedFitness(Chromosome chromosome, OptimizationRequest request,
                                            ConstraintEvaluationResult evaluation) {
        double baseFitness = 25000.0;

        // Balanced penalties
        double hardPenalty = evaluation.getHardViolationCount() * 1500.0;
        double softPenalty = evaluation.getSoftViolationCount() * 40.0;
        baseFitness -= (hardPenalty + softPenalty);

        // Core objectives
        double coverageBonus = calculateTaskCoverageBonus(chromosome, request) * 800;
        double fairnessBonus = calculateBasicFairness(chromosome, request) * 600;
        double utilizationBonus = calculateStaffUtilization(chromosome, request) * 400;

        baseFitness += (coverageBonus + fairnessBonus + utilizationBonus);

        // Pattern compliance for feasible solutions
        if (evaluation.getHardViolationCount() == 0) {
            double patternBonus = calculatePatternCompliance(chromosome, request) * 300;
            baseFitness += patternBonus;
        }

        // Unassigned tasks penalty
        baseFitness -= calculateUnassignedTasksPenalty(chromosome, request) * 200;

        return Math.max(0, baseFitness);
    }

    // === COMPONENT CALCULATIONS ===

    private double calculateTaskCoverageRate(Chromosome chromosome, OptimizationRequest request) {
        List<Task> unassignedTasks = chromosome.getUnassignedTasks(request.getActiveTasks());
        int totalTasks = request.getActiveTasks().size();

        if (totalTasks == 0) return 1.0;

        return (double)(totalTasks - unassignedTasks.size()) / totalTasks;
    }

    private double calculateTaskCoverageBonus(Chromosome chromosome, OptimizationRequest request) {
        return calculateTaskCoverageRate(chromosome, request);
    }

    private double calculateAdvancedFairness(Chromosome chromosome, OptimizationRequest request) {
        Map<Staff, Double> workingHours = new HashMap<>();
        Map<Staff, Integer> workingDays = new HashMap<>();
        Map<Staff, Integer> taskCounts = new HashMap<>();

        for (Staff staff : request.getActiveStaff()) {
            double hours = chromosome.getTotalWorkingHours(staff);
            int days = chromosome.getWorkingDaysCount(staff);
            int tasks = (int) chromosome.getGenesForStaff(staff).stream()
                    .mapToLong(g -> g.getTasks().size())
                    .sum();

            workingHours.put(staff, hours);
            workingDays.put(staff, days);
            taskCounts.put(staff, tasks);
        }

        if (workingHours.size() <= 1) return 1.0;

        // Calculate fairness for each dimension
        double hoursFairness = calculateFairnessScore(
                workingHours.values().stream().mapToDouble(Double::doubleValue).boxed().collect(Collectors.toList()));
        double daysFairness = calculateFairnessScore(
                workingDays.values().stream().mapToDouble(Integer::doubleValue).boxed().collect(Collectors.toList()));
        double tasksFairness = calculateFairnessScore(
                taskCounts.values().stream().mapToDouble(Integer::doubleValue).boxed().collect(Collectors.toList()));

        // Weighted combination
        return (hoursFairness * 0.5) + (daysFairness * 0.3) + (tasksFairness * 0.2);
    }

    private double calculateBasicFairness(Chromosome chromosome, OptimizationRequest request) {
        Map<Staff, Double> workingHours = new HashMap<>();
        for (Staff staff : request.getActiveStaff()) {
            workingHours.put(staff, chromosome.getTotalWorkingHours(staff));
        }

        if (workingHours.size() <= 1) return 1.0;

        return calculateFairnessScore(workingHours.values().stream()
                .mapToDouble(Double::doubleValue).boxed().collect(Collectors.toList()));
    }

    private double calculateFairnessScore(List<Double> values) {
        if (values.size() <= 1) return 1.0;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean == 0) return 1.0;

        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / mean;

        return Math.max(0, 1.0 - coefficientOfVariation);
    }

    private double calculateStaffUtilization(Chromosome chromosome, OptimizationRequest request) {
        int totalStaff = request.getActiveStaff().size();
        if (totalStaff == 0) return 1.0;

        long utilizatedStaff = request.getActiveStaff().stream()
                .mapToLong(staff -> chromosome.getTotalWorkingHours(staff) >= 20 ? 1 : 0)
                .sum();

        return (double) utilizatedStaff / totalStaff;
    }

    private double calculatePatternCompliance(Chromosome chromosome, OptimizationRequest request) {
        int totalChecks = 0;
        int compliantChecks = 0;

        for (Staff staff : request.getActiveStaff()) {
            if (staff.getSquad() == null || staff.getSquad().getSquadWorkingPattern() == null) {
                continue;
            }

            List<Gene> staffGenes = chromosome.getGenesForStaff(staff);
            if (staffGenes.isEmpty()) continue;

            totalChecks += staffGenes.size();

            // Simplified pattern compliance - 80% compliance assumed
            compliantChecks += (int)(staffGenes.size() * 0.8);
        }

        return totalChecks == 0 ? 1.0 : (double) compliantChecks / totalChecks;
    }

    private double calculatePriorityTaskSatisfaction(Chromosome chromosome, OptimizationRequest request) {
        List<Task> allTasks = request.getActiveTasks();
        List<Task> unassignedTasks = chromosome.getUnassignedTasks(allTasks);

        if (allTasks.isEmpty()) return 1.0;

        // Weight by inverse priority (lower priority number = higher importance)
        double totalWeight = allTasks.stream()
                .mapToDouble(task -> 1.0 / Math.max(1, task.getPriority()))
                .sum();

        double unassignedWeight = unassignedTasks.stream()
                .mapToDouble(task -> 1.0 / Math.max(1, task.getPriority()))
                .sum();

        return totalWeight == 0 ? 1.0 : (totalWeight - unassignedWeight) / totalWeight;
    }

    private double calculateWorkloadDistribution(Chromosome chromosome, OptimizationRequest request) {
        Map<Staff, Double> dailyAverages = new HashMap<>();

        for (Staff staff : request.getActiveStaff()) {
            List<Gene> staffGenes = chromosome.getGenesForStaff(staff);
            Map<java.time.LocalDate, Double> dailyHours = staffGenes.stream()
                    .collect(Collectors.groupingBy(Gene::getDate,
                            Collectors.summingDouble(Gene::getWorkingHours)));

            double averageDailyHours = dailyHours.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            dailyAverages.put(staff, averageDailyHours);
        }

        return calculateFairnessScore(dailyAverages.values().stream()
                .mapToDouble(Double::doubleValue).boxed().collect(Collectors.toList()));
    }

    private double calculateEfficiency(Chromosome chromosome, OptimizationRequest request) {
        double totalWorkingHours = chromosome.getGenes().stream()
                .mapToDouble(Gene::getWorkingHours)
                .sum();

        if (totalWorkingHours == 0) return 0.0;

        int totalTasksAssigned = chromosome.getGenes().stream()
                .mapToInt(gene -> gene.getTasks().size())
                .sum();

        // Efficiency = tasks per hour
        double efficiency = totalTasksAssigned / totalWorkingHours;

        // Normalize to 0-1 range (assuming 0.5 tasks per hour is excellent)
        return Math.min(1.0, efficiency / 0.5);
    }

    private double calculateUnassignedTasksPenalty(Chromosome chromosome, OptimizationRequest request) {
        List<Task> unassignedTasks = chromosome.getUnassignedTasks(request.getActiveTasks());

        return unassignedTasks.stream()
                .mapToDouble(task -> 1.0 / Math.max(1, task.getPriority()))
                .sum();
    }

    private double calculateCriticalIssuesPenalty(Chromosome chromosome, OptimizationRequest request) {
        double penalty = 0.0;

        // Penalty for staff working below minimum threshold
        for (Staff staff : request.getActiveStaff()) {
            double workingHours = chromosome.getTotalWorkingHours(staff);
            if (workingHours > 0 && workingHours < 15) {
                penalty += (15 - workingHours) * 10;
            }
        }

        // Penalty for extreme workload imbalance
        double imbalanceScore = chromosome.getWorkloadImbalanceScore();
        if (imbalanceScore > 15) {
            penalty += (imbalanceScore - 15) * 20;
        }

        return penalty;
    }

    private RosterPlan createTemporaryPlan(Chromosome chromosome, OptimizationRequest request) {
        return RosterPlan.builder()
                .planId("TEMP-" + UUID.randomUUID())
                .algorithmUsed("GA-TEMP")
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .assignments(chromosome.toAssignments())
                .unassignedTasks(chromosome.getUnassignedTasks(request.getActiveTasks()))
                .build();
    }

    public double estimateFitness(Chromosome chromosome, OptimizationRequest request) {
        double coverage = calculateTaskCoverageRate(chromosome, request);
        double fairness = calculateBasicFairness(chromosome, request);
        double utilization = calculateStaffUtilization(chromosome, request);

        return (coverage * 1000) + (fairness * 500) + (utilization * 300);
    }

    public double calculateImprovementPotential(Chromosome chromosome, OptimizationRequest request) {
        double coverageGap = 1.0 - calculateTaskCoverageRate(chromosome, request);
        double fairnessGap = 1.0 - calculateAdvancedFairness(chromosome, request);
        double utilizationGap = 1.0 - calculateStaffUtilization(chromosome, request);

        return (coverageGap * 800) + (fairnessGap * 600) + (utilizationGap * 400);
    }

    public Chromosome selectBetter(Chromosome c1, Chromosome c2, OptimizationRequest request) {
        double fitness1 = c1.isFitnessCalculated() ? c1.getFitnessScore() : calculateFitness(c1, request);
        double fitness2 = c2.isFitnessCalculated() ? c2.getFitnessScore() : calculateFitness(c2, request);

        if (!c1.isFitnessCalculated()) c1.setFitnessScore(fitness1);
        if (!c2.isFitnessCalculated()) c2.setFitnessScore(fitness2);

        return fitness1 >= fitness2 ? c1 : c2;
    }

    @Data
    @Builder
    public static class FitnessBreakdown {
        private double totalFitness;
        private double hardViolationsPenalty;
        private double softViolationsPenalty;
        private double taskCoverageScore;
        private double fairnessScore;
        private double utilizationScore;
        private double patternComplianceScore;
        private double priorityTaskScore;
        private double efficiencyScore;
        private double unassignedTasksPenalty;
        private double criticalIssuesPenalty;

        public Map<String, Double> toMap() {
            Map<String, Double> breakdown = new HashMap<>();
            breakdown.put("totalFitness", totalFitness);
            breakdown.put("hardViolationsPenalty", -hardViolationsPenalty);
            breakdown.put("softViolationsPenalty", -softViolationsPenalty);
            breakdown.put("taskCoverageScore", taskCoverageScore);
            breakdown.put("fairnessScore", fairnessScore);
            breakdown.put("utilizationScore", utilizationScore);
            breakdown.put("patternComplianceScore", patternComplianceScore);
            breakdown.put("priorityTaskScore", priorityTaskScore);
            breakdown.put("efficiencyScore", efficiencyScore);
            breakdown.put("unassignedTasksPenalty", -unassignedTasksPenalty);
            breakdown.put("criticalIssuesPenalty", -criticalIssuesPenalty);
            return breakdown;
        }
    }

    public FitnessBreakdown getDetailedFitnessBreakdown(Chromosome chromosome, OptimizationRequest request) {
        RosterPlan tempPlan = createTemporaryPlan(chromosome, request);
        ConstraintEvaluationResult evaluation = constraintEvaluator.evaluateRosterPlan(tempPlan);

        return FitnessBreakdown.builder()
                .totalFitness(calculateFitness(chromosome, request))
                .hardViolationsPenalty(evaluation.getHardViolationCount() * 1500.0)
                .softViolationsPenalty(evaluation.getSoftViolationCount() * 40.0)
                .taskCoverageScore(calculateTaskCoverageBonus(chromosome, request) * 800)
                .fairnessScore(calculateBasicFairness(chromosome, request) * 600)
                .utilizationScore(calculateStaffUtilization(chromosome, request) * 400)
                .patternComplianceScore(calculatePatternCompliance(chromosome, request) * 300)
                .priorityTaskScore(calculatePriorityTaskSatisfaction(chromosome, request) * 400)
                .efficiencyScore(calculateEfficiency(chromosome, request) * 200)
                .unassignedTasksPenalty(calculateUnassignedTasksPenalty(chromosome, request) * 200)
                .criticalIssuesPenalty(calculateCriticalIssuesPenalty(chromosome, request))
                .build();
    }
}