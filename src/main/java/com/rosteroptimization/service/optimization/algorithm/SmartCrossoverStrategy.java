package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.entity.Task;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.service.optimization.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class SmartCrossoverStrategy implements CrossoverStrategy {

    @Override
    public Chromosome crossover(Chromosome parent1, Chromosome parent2, OptimizationRequest request) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Calculate similarity between parents
        double similarity = calculateSimilarity(parent1, parent2);

        // Choose crossover strategy based on similarity
        if (similarity > 0.8) {
            return uniformCrossover(parent1, parent2);
        } else if (similarity < 0.3) {
            return preserveBestCrossover(parent1, parent2);
        } else {
            return smartSinglePointCrossover(parent1, parent2, request);
        }
    }

    private Chromosome uniformCrossover(Chromosome parent1, Chromosome parent2) {
        Map<String, Gene> genes1 = parent1.getGenes().stream()
                .collect(Collectors.toMap(Gene::getGeneId, g -> g));
        Map<String, Gene> genes2 = parent2.getGenes().stream()
                .collect(Collectors.toMap(Gene::getGeneId, g -> g));

        Set<String> allKeys = new HashSet<>(genes1.keySet());
        allKeys.addAll(genes2.keySet());

        List<Gene> offspringGenes = new ArrayList<>();
        Set<Task> assignedTasks = new HashSet<>();

        for (String geneId : allKeys) {
            Gene gene1 = genes1.get(geneId);
            Gene gene2 = genes2.get(geneId);

            Gene selectedGene = selectBetterGene(gene1, gene2, assignedTasks);
            if (selectedGene != null) {
                offspringGenes.add(selectedGene.copy());
                if (selectedGene.hasTasks()) {
                    assignedTasks.addAll(selectedGene.getTasks());
                }
            }
        }

        return new Chromosome(offspringGenes);
    }

    private Chromosome preserveBestCrossover(Chromosome parent1, Chromosome parent2) {
        // Determine better parent
        Chromosome betterParent = parent1.getFitnessScore() >= parent2.getFitnessScore() ? parent1 : parent2;
        Chromosome worseParent = betterParent == parent1 ? parent2 : parent1;

        // Start with better parent
        List<Gene> offspringGenes = betterParent.getGenes().stream()
                .map(Gene::copy)
                .collect(Collectors.toList());

        // Try to improve with genes from worse parent
        Map<String, Gene> worseParentGenes = worseParent.getGenes().stream()
                .collect(Collectors.toMap(Gene::getGeneId, g -> g));

        Set<Task> assignedTasks = offspringGenes.stream()
                .filter(Gene::hasTasks)
                .flatMap(g -> g.getTasks().stream())
                .collect(Collectors.toSet());

        for (int i = 0; i < offspringGenes.size(); i++) {
            Gene currentGene = offspringGenes.get(i);
            Gene alternativeGene = worseParentGenes.get(currentGene.getGeneId());

            if (alternativeGene != null && isImprovement(currentGene, alternativeGene, assignedTasks)) {
                offspringGenes.set(i, alternativeGene.copy());
                if (currentGene.hasTasks()) {
                    assignedTasks.removeAll(currentGene.getTasks());
                }
                if (alternativeGene.hasTasks()) {
                    assignedTasks.addAll(alternativeGene.getTasks());
                }
            }
        }

        return new Chromosome(offspringGenes);
    }

    private Chromosome smartSinglePointCrossover(Chromosome parent1, Chromosome parent2, OptimizationRequest request) {
        // Find optimal crossover point based on workload balance
        int bestCrossoverPoint = findOptimalCrossoverPoint(parent1, parent2, request);

        List<Gene> parent1Genes = parent1.getGenes();
        List<Gene> parent2Genes = parent2.getGenes();

        List<Gene> offspringGenes = new ArrayList<>();
        Set<Task> assignedTasks = new HashSet<>();

        // Combine genes from both parents at the crossover point
        for (int i = 0; i < Math.min(parent1Genes.size(), parent2Genes.size()); i++) {
            Gene selectedGene = i < bestCrossoverPoint ? parent1Genes.get(i) : parent2Genes.get(i);

            // Avoid task conflicts
            if (selectedGene.hasTasks() && hasTaskConflict(selectedGene, assignedTasks)) {
                Gene alternativeGene = i < bestCrossoverPoint ? parent2Genes.get(i) : parent1Genes.get(i);
                if (!hasTaskConflict(alternativeGene, assignedTasks)) {
                    selectedGene = alternativeGene;
                } else {
                    // Create day-off gene to avoid conflict
                    selectedGene = Gene.createDayOffGene(selectedGene.getStaff(), selectedGene.getDate());
                }
            }

            offspringGenes.add(selectedGene.copy());
            if (selectedGene.hasTasks()) {
                assignedTasks.addAll(selectedGene.getTasks());
            }
        }

        return new Chromosome(offspringGenes);
    }

    private double calculateSimilarity(Chromosome c1, Chromosome c2) {
        Map<String, Gene> genes1 = c1.getGenes().stream()
                .collect(Collectors.toMap(Gene::getGeneId, g -> g));
        Map<String, Gene> genes2 = c2.getGenes().stream()
                .collect(Collectors.toMap(Gene::getGeneId, g -> g));

        Set<String> allKeys = new HashSet<>(genes1.keySet());
        allKeys.addAll(genes2.keySet());

        if (allKeys.isEmpty()) return 1.0;

        int matches = 0;
        for (String key : allKeys) {
            Gene gene1 = genes1.get(key);
            Gene gene2 = genes2.get(key);
            if (Objects.equals(gene1, gene2)) {
                matches++;
            }
        }

        return (double) matches / allKeys.size();
    }

    private Gene selectBetterGene(Gene gene1, Gene gene2, Set<Task> assignedTasks) {
        if (gene1 == null) return gene2;
        if (gene2 == null) return gene1;

        // Avoid task conflicts
        if (gene1.hasTasks() && hasTaskConflict(gene1, assignedTasks)) {
            return gene2.hasTasks() && hasTaskConflict(gene2, assignedTasks) ?
                    Gene.createDayOffGene(gene1.getStaff(), gene1.getDate()) : gene2;
        }
        if (gene2.hasTasks() && hasTaskConflict(gene2, assignedTasks)) {
            return gene1;
        }

        // Prefer task-bearing genes
        if (gene1.hasTasks() && !gene2.hasTasks()) return gene1;
        if (gene2.hasTasks() && !gene1.hasTasks()) return gene2;

        // Random selection for similar genes
        return ThreadLocalRandom.current().nextBoolean() ? gene1 : gene2;
    }

    private boolean hasTaskConflict(Gene gene, Set<Task> assignedTasks) {
        return gene.hasTasks() && gene.getTasks().stream().anyMatch(assignedTasks::contains);
    }

    private boolean isImprovement(Gene current, Gene alternative, Set<Task> assignedTasks) {
        if (hasTaskConflict(alternative, assignedTasks)) return false;

        // Simple improvement heuristics
        if (!current.hasTasks() && alternative.hasTasks()) return true;
        if (current.hasTasks() && !alternative.hasTasks()) return false;

        return false;
    }

    private int findOptimalCrossoverPoint(Chromosome parent1, Chromosome parent2, OptimizationRequest request) {
        int size = Math.min(parent1.getGenes().size(), parent2.getGenes().size());
        if (size <= 1) return size / 2;

        double bestBalance = Double.MAX_VALUE;
        int bestPoint = size / 2;

        for (int point = size / 4; point <= 3 * size / 4; point += Math.max(1, size / 10)) {
            double balance = evaluateWorkloadBalance(parent1, parent2, point, request);
            if (balance < bestBalance) {
                bestBalance = balance;
                bestPoint = point;
            }
        }

        return bestPoint;
    }

    private double evaluateWorkloadBalance(Chromosome parent1, Chromosome parent2, int crossoverPoint, OptimizationRequest request) {
        Map<Long, Double> workloads = new HashMap<>();

        for (int i = 0; i < Math.min(parent1.getGenes().size(), parent2.getGenes().size()); i++) {
            Gene gene = i < crossoverPoint ? parent1.getGenes().get(i) : parent2.getGenes().get(i);
            Long staffId = gene.getStaff().getId();
            workloads.merge(staffId, gene.getWorkingHours(), Double::sum);
        }

        // Calculate variance in workloads
        double mean = workloads.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return workloads.values().stream()
                .mapToDouble(hours -> Math.pow(hours - mean, 2))
                .average()
                .orElse(0.0);
    }
}