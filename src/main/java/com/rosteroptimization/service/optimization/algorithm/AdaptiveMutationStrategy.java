package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.entity.Task;
import com.rosteroptimization.service.optimization.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AdaptiveMutationStrategy implements MutationStrategy {

    private Map<String, List<Gene>> geneSpace;

    public AdaptiveMutationStrategy() {
        // Default constructor for Spring
    }

    public AdaptiveMutationStrategy(Map<String, List<Gene>> geneSpace) {
        this.geneSpace = geneSpace;
    }

    public void setGeneSpace(Map<String, List<Gene>> geneSpace) {
        this.geneSpace = geneSpace;
    }

    @Override
    public void mutate(Chromosome chromosome, double mutationRate, OptimizationRequest request) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Adaptive mutation rate based on chromosome quality
        double adaptedRate = calculateAdaptiveMutationRate(chromosome, mutationRate, request);

        if (random.nextDouble() > adaptedRate) return;

        // Choose mutation type based on chromosome characteristics
        MutationType mutationType = selectMutationType(chromosome, request);

        switch (mutationType) {
            case TARGETED -> performTargetedMutation(chromosome, request);
            case WORKLOAD_BALANCING -> performWorkloadBalancingMutation(chromosome);
            case RANDOM -> performRandomMutation(chromosome);
            case TASK_FOCUSED -> performTaskFocusedMutation(chromosome, request);
        }
    }

    private double calculateAdaptiveMutationRate(Chromosome chromosome, double baseMutationRate, OptimizationRequest request) {
        // Increase mutation rate for poor solutions
        if (chromosome.getFitnessScore() < 1000) {
            return baseMutationRate * 2.0;
        }

        // Increase mutation rate if many tasks unassigned
        List<Task> unassigned = chromosome.getUnassignedTasks(request.getActiveTasks());
        if (unassigned.size() > request.getActiveTasks().size() * 0.2) {
            return baseMutationRate * 1.5;
        }

        return baseMutationRate;
    }

    private MutationType selectMutationType(Chromosome chromosome, OptimizationRequest request) {
        List<Task> unassigned = chromosome.getUnassignedTasks(request.getActiveTasks());
        double imbalanceScore = chromosome.getWorkloadImbalanceScore();

        if (!unassigned.isEmpty()) {
            return MutationType.TASK_FOCUSED;
        } else if (imbalanceScore > 10) {
            return MutationType.WORKLOAD_BALANCING;
        } else if (chromosome.getFitnessScore() < 5000) {
            return MutationType.TARGETED;
        } else {
            return MutationType.RANDOM;
        }
    }

    private void performTargetedMutation(Chromosome chromosome, OptimizationRequest request) {
        chromosome.mutate(geneSpace, 0.1);
    }

    private void performWorkloadBalancingMutation(Chromosome chromosome) {
        chromosome.balanceWorkloads();
    }

    private void performRandomMutation(Chromosome chromosome) {
        chromosome.mutate(geneSpace, 0.05);
    }

    private void performTaskFocusedMutation(Chromosome chromosome, OptimizationRequest request) {
        chromosome.mutate(geneSpace, 0.15);
    }

    private enum MutationType {
        TARGETED,
        WORKLOAD_BALANCING,
        RANDOM,
        TASK_FOCUSED
    }
}