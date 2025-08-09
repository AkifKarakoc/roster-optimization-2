package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.entity.*;
import com.rosteroptimization.service.optimization.constraint.ConstraintEvaluator;
import com.rosteroptimization.service.optimization.constraint.ConstraintEvaluationResult;
import com.rosteroptimization.service.optimization.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeneticAlgorithmOptimizer implements Optimizer {

    private final ConstraintEvaluator constraintEvaluator;
    private final TaskSplittingPreprocessor taskSplittingPreprocessor;
    private final PopulationManager populationManager;
    private final FitnessCalculator fitnessCalculator;
    private final ConstraintRelaxationEngine constraintRelaxationEngine;
    private final OptimizationCache optimizationCache;
    private final PerformanceMonitor performanceMonitor;

    @Data
    @Builder
    public static class GAConfig {
        private int populationSize;
        private int maxGenerations;
        private double mutationRate;
        private double crossoverRate;
        private double eliteRate;
        private int tournamentSize;
        private int stagnationLimit;
        private int numberOfIslands;
        private int migrationInterval;
        private int migrationSize;
        private boolean enableLocalSearch;

        public static GAConfig getDefault() {
            return GAConfig.builder()
                    .populationSize(50)
                    .maxGenerations(1000)
                    .mutationRate(0.1)
                    .crossoverRate(0.8)
                    .eliteRate(0.1)
                    .tournamentSize(5)
                    .stagnationLimit(100)
                    .numberOfIslands(3)
                    .migrationInterval(50)
                    .migrationSize(5)
                    .enableLocalSearch(true)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    public static class Island {
        private int id;
        private IslandType type;
        private Population population;
        private int stagnationCounter;
        private double bestFitness;

        public enum IslandType {
            CONSTRAINT_FOCUSED,
            BALANCED,
            QUALITY_FOCUSED
        }
    }

    private ExecutorService islandExecutor;
    private volatile boolean cancelled = false;
    private volatile int currentGeneration = 0;
    private GAConfig config;

    @Override
    public RosterPlan optimize(OptimizationRequest request) throws OptimizationException {
        log.info("Starting Advanced GA: {} staff, {} tasks, {} days",
                request.getActiveStaff().size(), request.getActiveTasks().size(), request.getPlanningDays());

        long startTime = System.currentTimeMillis();
        performanceMonitor.startOptimization();

        try {
            validateRequest(request);
            initializeConfig(request);

            // Task splitting preprocessing
            TaskSplittingPreprocessor.TaskSplittingResult splittingResult =
                    taskSplittingPreprocessor.preprocessTasks(request.getActiveTasks(), request.getActiveShifts());

            OptimizationRequest modifiedRequest = createModifiedRequest(request, splittingResult);

            // Auto constraint relaxation if needed
            if (constraintRelaxationEngine.isProblemInfeasible(modifiedRequest)) {
                log.warn("Applying automatic constraint relaxation");
                modifiedRequest = constraintRelaxationEngine.applyMinimalRelaxation(modifiedRequest);
            }

            // Generate gene space
            Map<String, List<Gene>> geneSpace = generateGeneSpace(modifiedRequest);

            // Run Island Model GA
            Chromosome bestSolution = runIslandModelGA(geneSpace, modifiedRequest);

            // Build final result
            RosterPlan plan = buildRosterPlan(bestSolution, modifiedRequest, splittingResult,
                    System.currentTimeMillis() - startTime);

            performanceMonitor.endOptimization(plan);
            return plan;

        } catch (Exception e) {
            log.error("GA optimization failed", e);
            throw new OptimizationException("Advanced GA failed: " + e.getMessage(), e);
        } finally {
            cleanup();
        }
    }

    private Chromosome runIslandModelGA(Map<String, List<Gene>> geneSpace, OptimizationRequest request) {
        log.info("Starting Island Model GA with {} islands", config.numberOfIslands);

        // Create specialized islands
        List<Island> islands = createIslands(geneSpace, request);

        // Run islands in parallel with migration coordination
        List<CompletableFuture<Chromosome>> islandFutures = islands.stream()
                .map(island -> CompletableFuture.supplyAsync(() ->
                        runSingleIslandEvolution(island, geneSpace, request), islandExecutor))
                .collect(Collectors.toList());

        // Migration coordination in separate thread
        CompletableFuture<Void> migrationCoordinator = CompletableFuture.runAsync(() ->
                coordinateMigration(islands));

        try {
            // Wait for all islands to complete
            List<Chromosome> islandResults = islandFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            migrationCoordinator.cancel(true);

            // Select best solution across all islands
            Chromosome bestSolution = islandResults.stream()
                    .max(Comparator.comparingDouble(Chromosome::getFitnessScore))
                    .orElse(islandResults.get(0));

            log.info("Island Model completed. Best fitness: {:.2f}", bestSolution.getFitnessScore());
            return bestSolution;

        } catch (Exception e) {
            log.error("Island Model failed, falling back to single population", e);
            return runFallbackGA(geneSpace, request);
        }
    }

    private List<Island> createIslands(Map<String, List<Gene>> geneSpace, OptimizationRequest request) {
        List<Island> islands = new ArrayList<>();

        // Island 1: Constraint-focused (high mutation, feasibility focus)
        Population pop1 = populationManager.createSpecializedPopulation(
                config.populationSize, geneSpace, request, PopulationManager.PopulationSpecialization.CONSTRAINT_FOCUSED);
        islands.add(new Island(0, Island.IslandType.CONSTRAINT_FOCUSED, pop1, 0, Double.NEGATIVE_INFINITY));

        // Island 2: Balanced approach
        Population pop2 = populationManager.createInitialPopulation(config.populationSize, geneSpace, request);
        islands.add(new Island(1, Island.IslandType.BALANCED, pop2, 0, Double.NEGATIVE_INFINITY));

        // Island 3: Quality-focused (low mutation, elite preservation)
        Population pop3 = populationManager.createSpecializedPopulation(
                (int)(config.populationSize * 1.2), geneSpace, request, PopulationManager.PopulationSpecialization.WORKLOAD_BALANCED);
        islands.add(new Island(2, Island.IslandType.QUALITY_FOCUSED, pop3, 0, Double.NEGATIVE_INFINITY));

        log.info("Created {} specialized islands", islands.size());
        return islands;
    }

    private Chromosome runSingleIslandEvolution(Island island, Map<String, List<Gene>> geneSpace, OptimizationRequest request) {
        log.debug("Starting evolution on Island {} ({})", island.getId(), island.getType());

        Population population = island.getPopulation();
        SelectionStrategy selectionStrategy = createSelectionStrategy(island.getType());
        CrossoverStrategy crossoverStrategy = new SmartCrossoverStrategy();
        MutationStrategy mutationStrategy = new AdaptiveMutationStrategy(geneSpace);

        for (int generation = 0; generation < config.maxGenerations && !cancelled; generation++) {
            currentGeneration = generation;

            // Evaluate population with island-specific focus
            evaluatePopulationWithIslandFocus(population, request, island);
            population.sortByFitness();

            Chromosome currentBest = population.getBest();
            double currentFitness = currentBest.getFitnessScore();

            // Update island statistics
            if (currentFitness > island.getBestFitness()) {
                island.setBestFitness(currentFitness);
                island.setStagnationCounter(0);
            } else {
                island.setStagnationCounter(island.getStagnationCounter() + 1);
            }

            // Early termination for constraint-focused island
            if (island.getType() == Island.IslandType.CONSTRAINT_FOCUSED && currentBest.isFeasible(request)) {
                log.debug("Island {} found feasible solution at generation {}", island.getId(), generation);
                break;
            }

            // Stagnation check
            if (island.getStagnationCounter() >= config.stagnationLimit / 2) {
                log.debug("Island {} terminated due to stagnation", island.getId());
                break;
            }

            // Evolve population
            population = evolvePopulation(population, selectionStrategy, crossoverStrategy,
                    mutationStrategy, island, geneSpace, request);
            island.setPopulation(population);

            // Local search for quality-focused island
            if (config.enableLocalSearch && island.getType() == Island.IslandType.QUALITY_FOCUSED && generation % 20 == 0) {
                applyLocalSearch(population, geneSpace, request);
            }

            // Diversity injection
            if (generation % 50 == 0 && generation > 0) {
                populationManager.injectDiversity(population, geneSpace, request, 0.3);
            }

            // Progress logging
            if (generation % 100 == 0) {
                log.debug("Island {} Gen {}: fitness={:.2f}, stagnation={}",
                        island.getId(), generation, currentFitness, island.getStagnationCounter());
            }
        }

        return population.getBest();
    }

    private void coordinateMigration(List<Island> islands) {
        try {
            int migrationCount = 0;
            while (!cancelled && currentGeneration < config.maxGenerations) {
                Thread.sleep(config.migrationInterval * 100);

                if (currentGeneration % config.migrationInterval == 0 && currentGeneration > 0) {
                    performMigration(islands);
                    migrationCount++;
                    log.debug("Migration {} completed at generation {}", migrationCount, currentGeneration);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Migration coordinator interrupted");
        }
    }

    private void performMigration(List<Island> islands) {
        // BEST_TO_WORST migration strategy
        islands.sort((i1, i2) -> Double.compare(i2.getBestFitness(), i1.getBestFitness()));

        int migrated = 0;
        for (int i = 0; i < islands.size() - 1; i++) {
            Island sourceIsland = islands.get(i);
            Island targetIsland = islands.get(i + 1);

            // Migrate best chromosomes from better to worse island
            List<Chromosome> migrants = sourceIsland.getPopulation().getBestN(config.migrationSize);
            targetIsland.getPopulation().replaceWorstWith(migrants);
            migrated += migrants.size();
        }

        log.debug("Migration: {} chromosomes migrated between {} islands", migrated, islands.size());
    }

    private Population evolvePopulation(Population currentPop, SelectionStrategy selectionStrategy,
                                        CrossoverStrategy crossoverStrategy, MutationStrategy mutationStrategy,
                                        Island island, Map<String, List<Gene>> geneSpace, OptimizationRequest request) {

        List<Chromosome> newGeneration = new ArrayList<>();
        int targetSize = currentPop.size();

        // Adaptive elitism based on island type
        double eliteRate = getAdaptiveEliteRate(island.getType());
        int eliteCount = Math.max(1, (int)(targetSize * eliteRate));

        for (int i = 0; i < eliteCount; i++) {
            newGeneration.add(currentPop.getChromosomes().get(i).copy());
        }

        // Generate offspring
        while (newGeneration.size() < targetSize && !cancelled) {
            double adaptiveMutationRate = getAdaptiveMutationRate(island);

            if (ThreadLocalRandom.current().nextDouble() < config.crossoverRate) {
                // Crossover
                Chromosome parent1 = selectionStrategy.select(currentPop.getChromosomes());
                Chromosome parent2 = selectionStrategy.select(currentPop.getChromosomes());

                Chromosome offspring = crossoverStrategy.crossover(parent1, parent2, request);
                mutationStrategy.mutate(offspring, adaptiveMutationRate, request);
                offspring.repairBasic();

                newGeneration.add(offspring);
            } else {
                // Mutation only
                Chromosome selected = selectionStrategy.select(currentPop.getChromosomes());
                Chromosome mutated = selected.copy();
                mutationStrategy.mutate(mutated, adaptiveMutationRate * 1.5, request);
                mutated.repairBasic();

                newGeneration.add(mutated);
            }
        }

        return new Population(newGeneration);
    }

    private void evaluatePopulationWithIslandFocus(Population population, OptimizationRequest request, Island island) {
        population.getChromosomes().parallelStream()
                .filter(c -> !c.isFitnessCalculated())
                .forEach(c -> {
                    String cacheKey = c.getSignature();
                    Double cachedFitness = optimizationCache.getFitness(cacheKey);

                    if (cachedFitness != null) {
                        c.setFitnessScore(cachedFitness);
                    } else {
                        double fitness = fitnessCalculator.calculateFitnessWithFocus(c, request, island.getType());
                        c.setFitnessScore(fitness);
                        optimizationCache.cacheFitness(cacheKey, fitness);
                    }
                });
    }

    private void applyLocalSearch(Population population, Map<String, List<Gene>> geneSpace, OptimizationRequest request) {
        int eliteCount = Math.max(1, (int)(population.size() * 0.1));

        for (int i = 0; i < eliteCount; i++) {
            Chromosome elite = population.getChromosomes().get(i);
            performLocalSearchOptimization(elite, geneSpace, request);
        }
    }

    private void performLocalSearchOptimization(Chromosome chromosome, Map<String, List<Gene>> geneSpace, OptimizationRequest request) {
        double currentFitness = chromosome.getFitnessScore();
        int maxIterations = 20;

        for (int iter = 0; iter < maxIterations && !cancelled; iter++) {
            // Try workload balancing
            Chromosome neighbor = chromosome.copy();
            if (neighbor.balanceWorkloads()) {
                neighbor.repairAdvanced();
                double neighborFitness = fitnessCalculator.calculateFitness(neighbor, request);

                if (neighborFitness > currentFitness) {
                    chromosome.setGenes(neighbor.getGenes());
                    chromosome.setFitnessScore(neighborFitness);
                    currentFitness = neighborFitness;
                    continue;
                }
            }

            // Try targeted mutation
            neighbor = chromosome.copy();
            neighbor.mutate(geneSpace, 0.05);
            neighbor.repairBasic();

            double neighborFitness = fitnessCalculator.calculateFitness(neighbor, request);
            if (neighborFitness > currentFitness) {
                chromosome.setGenes(neighbor.getGenes());
                chromosome.setFitnessScore(neighborFitness);
                currentFitness = neighborFitness;
            }
        }
    }

    private Chromosome runFallbackGA(Map<String, List<Gene>> geneSpace, OptimizationRequest request) {
        log.warn("Running fallback single-population GA");

        Population population = populationManager.createInitialPopulation(config.populationSize, geneSpace, request);
        SelectionStrategy selectionStrategy = new SelectionStrategy.TournamentSelection(config.tournamentSize);
        CrossoverStrategy crossoverStrategy = new SmartCrossoverStrategy();
        MutationStrategy mutationStrategy = new AdaptiveMutationStrategy(geneSpace);

        for (int generation = 0; generation < config.maxGenerations && !cancelled; generation++) {
            // Evaluate fitness
            population.getChromosomes().parallelStream()
                    .filter(c -> !c.isFitnessCalculated())
                    .forEach(c -> c.setFitnessScore(fitnessCalculator.calculateFitness(c, request)));

            population.sortByFitness();

            if (population.getBest().isFeasible(request)) {
                log.info("Fallback GA found feasible solution at generation {}", generation);
                break;
            }

            // Create next generation (simplified)
            List<Chromosome> newGeneration = new ArrayList<>();
            int eliteCount = Math.max(1, (int)(config.populationSize * config.eliteRate));

            for (int i = 0; i < eliteCount; i++) {
                newGeneration.add(population.getChromosomes().get(i).copy());
            }

            while (newGeneration.size() < config.populationSize) {
                Chromosome parent1 = selectionStrategy.select(population.getChromosomes());
                Chromosome parent2 = selectionStrategy.select(population.getChromosomes());

                Chromosome offspring = crossoverStrategy.crossover(parent1, parent2, request);
                mutationStrategy.mutate(offspring, config.mutationRate, request);
                offspring.repairBasic();

                newGeneration.add(offspring);
            }

            population = new Population(newGeneration);
        }

        return population.getBest();
    }

    // === UTILITY METHODS ===

    private Map<String, List<Gene>> generateGeneSpace(OptimizationRequest request) {
        log.info("Generating comprehensive gene space...");

        Map<String, List<Gene>> geneSpace = new HashMap<>();

        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            for (Staff staff : request.getActiveStaff()) {
                String key = staff.getId() + "-" + date;
                List<Gene> genes = generateGenesForStaffDate(staff, date, request);
                geneSpace.put(key, genes);
            }
        }

        int totalGenes = geneSpace.values().stream().mapToInt(List::size).sum();
        log.info("Generated {} gene combinations for {} staff-date pairs", totalGenes, geneSpace.size());

        return geneSpace;
    }

    private List<Gene> generateGenesForStaffDate(Staff staff, LocalDate date, OptimizationRequest request) {
        List<Gene> genes = new ArrayList<>();

        // Day off option
        genes.add(Gene.createDayOffGene(staff, date));

        // Shift-only options
        for (Shift shift : request.getActiveShifts()) {
            genes.add(Gene.createShiftOnlyGene(staff, date, shift));
        }

        // Single task assignments
        List<Task> dayTasks = getTasksForDate(request.getActiveTasks(), date);
        for (Task task : dayTasks) {
            if (isStaffQualifiedForTask(staff, task) && isStaffInSameDepartment(staff, task)) {
                for (Shift shift : getCompatibleShifts(request.getActiveShifts(), task)) {
                    genes.add(Gene.createShiftWithTaskGene(staff, date, shift, task));
                }
            }
        }

        // Multi-task combinations for same department
        for (Shift shift : request.getActiveShifts()) {
            List<List<Task>> taskCombinations = generateTaskCombinations(
                    dayTasks.stream()
                            .filter(task -> isStaffQualifiedForTask(staff, task))
                            .filter(task -> isStaffInSameDepartment(staff, task))
                            .collect(Collectors.toList()),
                    calculateShiftHours(shift)
            );

            for (List<Task> taskCombo : taskCombinations) {
                if (taskCombo.size() > 1) {
                    genes.add(Gene.createShiftWithMultipleTasksGene(staff, date, shift, taskCombo));
                }
            }
        }

        return genes;
    }

    private OptimizationRequest createModifiedRequest(OptimizationRequest original,
                                                      TaskSplittingPreprocessor.TaskSplittingResult splittingResult) {
        return OptimizationRequest.builder()
                .startDate(original.getStartDate())
                .endDate(original.getEndDate())
                .staffList(original.getStaffList())
                .taskList(splittingResult.getProcessedTasks())
                .shiftList(original.getShiftList())
                .department(original.getDepartment())
                .globalConstraints(original.getGlobalConstraints())
                .staffConstraintOverrides(original.getStaffConstraintOverrides())
                .algorithmParameters(original.getAlgorithmParameters())
                .algorithmType(original.getAlgorithmType())
                .maxExecutionTimeMinutes(original.getMaxExecutionTimeMinutes())
                .enableParallelProcessing(original.isEnableParallelProcessing())
                .build();
    }

    private RosterPlan buildRosterPlan(Chromosome bestChromosome, OptimizationRequest request,
                                       TaskSplittingPreprocessor.TaskSplittingResult splittingResult, long executionTime) {

        List<Assignment> assignments = bestChromosome.toAssignments();
        List<Task> unassignedTasks = bestChromosome.getUnassignedTasks(request.getActiveTasks());

        RosterPlan plan = RosterPlan.builder()
                .planId(UUID.randomUUID().toString())
                .generatedAt(LocalDateTime.now())
                .algorithmUsed(getAlgorithmName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .assignments(assignments)
                .fitnessScore(bestChromosome.getFitnessScore())
                .executionTimeMs(executionTime)
                .unassignedTasks(unassignedTasks)
                .underutilizedStaff(findUnderutilizedStaff(bestChromosome, request))
                .statistics(new HashMap<>())
                .algorithmMetadata(new HashMap<>())
                .build();

        // Set constraint violations
        ConstraintEvaluationResult eval = constraintEvaluator.evaluateRosterPlan(plan);
        plan.setHardConstraintViolations(eval.getHardViolationCount());
        plan.setSoftConstraintViolations(eval.getSoftViolationCount());

        // Add comprehensive statistics
        addStatistics(plan, bestChromosome, request, splittingResult);
        addAlgorithmMetadata(plan, splittingResult);

        return plan;
    }

    private void addStatistics(RosterPlan plan, Chromosome chromosome, OptimizationRequest request,
                               TaskSplittingPreprocessor.TaskSplittingResult splittingResult) {
        plan.addStatistic("taskCoverageRate", plan.getTaskCoverageRate());
        plan.addStatistic("staffUtilizationRate", plan.getStaffUtilizationRate());
        plan.addStatistic("totalWorkingHours", getTotalWorkingHours(chromosome));
        plan.addStatistic("averageWorkingHours", getAverageWorkingHours(chromosome, request));
        plan.addStatistic("workloadImbalance", chromosome.getWorkloadImbalanceScore());
        plan.addStatistic("splitTasksCount", splittingResult.getSplitTasksCount());
        plan.addStatistic("fairnessScore", calculateFairnessScore(chromosome, request));
    }

    private void addAlgorithmMetadata(RosterPlan plan, TaskSplittingPreprocessor.TaskSplittingResult splittingResult) {
        plan.addAlgorithmMetadata("algorithmVersion", "ADVANCED_ISLAND_MODEL_V1");
        plan.addAlgorithmMetadata("islandModel", true);
        plan.addAlgorithmMetadata("numberOfIslands", config.numberOfIslands);
        plan.addAlgorithmMetadata("migrationStrategy", "BEST_TO_WORST");
        plan.addAlgorithmMetadata("taskSplittingEnabled", splittingResult.hasSplitTasks());
        plan.addAlgorithmMetadata("constraintRelaxationApplied",
                plan.getAlgorithmMetadata().getOrDefault("constraintRelaxationApplied", false));
        plan.addAlgorithmMetadata("finalGeneration", currentGeneration);
        plan.addAlgorithmMetadata("cacheHitRate", optimizationCache.getHitRate());
    }

    // === HELPER METHODS ===

    private SelectionStrategy createSelectionStrategy(Island.IslandType type) {
        return switch (type) {
            case CONSTRAINT_FOCUSED -> new SelectionStrategy.TournamentSelection(config.tournamentSize);
            case BALANCED -> new SelectionStrategy.TournamentSelection(config.tournamentSize);
            case QUALITY_FOCUSED -> new SelectionStrategy.RankSelection();
        };
    }

    private double getAdaptiveEliteRate(Island.IslandType type) {
        return switch (type) {
            case CONSTRAINT_FOCUSED -> config.eliteRate * 0.5; // Less elitism
            case BALANCED -> config.eliteRate;
            case QUALITY_FOCUSED -> config.eliteRate * 2.0; // More elitism
        };
    }

    private double getAdaptiveMutationRate(Island island) {
        double baseMutation = config.mutationRate;

        return switch (island.getType()) {
            case CONSTRAINT_FOCUSED -> baseMutation * 2.0; // Higher mutation
            case BALANCED -> baseMutation;
            case QUALITY_FOCUSED -> baseMutation * 0.5; // Lower mutation
        };
    }

    private List<Task> getTasksForDate(List<Task> allTasks, LocalDate date) {
        return allTasks.stream()
                .filter(task -> task.getStartTime().toLocalDate().equals(date))
                .collect(Collectors.toList());
    }

    private boolean isStaffQualifiedForTask(Staff staff, Task task) {
        return task.getRequiredQualifications().stream()
                .allMatch(required -> staff.getQualifications().contains(required));
    }

    private boolean isStaffInSameDepartment(Staff staff, Task task) {
        return staff.getDepartment().getId().equals(task.getDepartment().getId());
    }

    private List<Shift> getCompatibleShifts(List<Shift> shifts, Task task) {
        return shifts; // Simplified - could add time compatibility logic
    }

    private double calculateShiftHours(Shift shift) {
        int startHour = shift.getStartTime().getHour();
        int startMinute = shift.getStartTime().getMinute();
        int endHour = shift.getEndTime().getHour();
        int endMinute = shift.getEndTime().getMinute();

        if (endHour < startHour) {
            endHour += 24;
        }

        double startDecimal = startHour + (startMinute / 60.0);
        double endDecimal = endHour + (endMinute / 60.0);

        return endDecimal - startDecimal;
    }

    private List<List<Task>> generateTaskCombinations(List<Task> tasks, double maxShiftHours) {
        List<List<Task>> combinations = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++) {
            for (int j = i + 1; j < tasks.size(); j++) {
                List<Task> combo = Arrays.asList(tasks.get(i), tasks.get(j));
                if (getTotalTaskHours(combo) <= maxShiftHours + 0.5) {
                    combinations.add(combo);
                }

                for (int k = j + 1; k < tasks.size(); k++) {
                    List<Task> combo3 = Arrays.asList(tasks.get(i), tasks.get(j), tasks.get(k));
                    if (getTotalTaskHours(combo3) <= maxShiftHours + 0.5) {
                        combinations.add(combo3);
                    }
                }
            }
        }

        return combinations;
    }

    private double getTotalTaskHours(List<Task> tasks) {
        return tasks.stream()
                .mapToDouble(task -> {
                    long minutes = java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
                    return minutes / 60.0;
                })
                .sum();
    }

    private List<Staff> findUnderutilizedStaff(Chromosome chromosome, OptimizationRequest request) {
        return request.getActiveStaff().stream()
                .filter(staff -> chromosome.getTotalWorkingHours(staff) < 25)
                .collect(Collectors.toList());
    }

    private double getTotalWorkingHours(Chromosome chromosome) {
        return chromosome.getGenes().stream()
                .mapToDouble(Gene::getWorkingHours)
                .sum();
    }

    private double getAverageWorkingHours(Chromosome chromosome, OptimizationRequest request) {
        return request.getActiveStaff().stream()
                .mapToDouble(chromosome::getTotalWorkingHours)
                .average()
                .orElse(0.0);
    }

    private double calculateFairnessScore(Chromosome chromosome, OptimizationRequest request) {
        Map<Staff, Double> workloads = new HashMap<>();
        for (Staff staff : request.getActiveStaff()) {
            workloads.put(staff, chromosome.getTotalWorkingHours(staff));
        }

        if (workloads.size() <= 1) return 1.0;

        double mean = workloads.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean == 0) return 1.0;

        double variance = workloads.values().stream()
                .mapToDouble(hours -> Math.pow(hours - mean, 2))
                .average().orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / mean;

        return Math.max(0, 1.0 - coefficientOfVariation);
    }

    private void initializeConfig(OptimizationRequest request) {
        this.config = GAConfig.builder()
                .populationSize(request.getAlgorithmParameter("populationSize", 50))
                .maxGenerations(request.getAlgorithmParameter("maxGenerations", 1000))
                .mutationRate(request.getAlgorithmParameter("mutationRate", 0.1))
                .crossoverRate(request.getAlgorithmParameter("crossoverRate", 0.8))
                .eliteRate(request.getAlgorithmParameter("eliteRate", 0.1))
                .tournamentSize(request.getAlgorithmParameter("tournamentSize", 5))
                .stagnationLimit(request.getAlgorithmParameter("stagnationLimit", 100))
                .numberOfIslands(request.getAlgorithmParameter("numberOfIslands", 3))
                .migrationInterval(request.getAlgorithmParameter("migrationInterval", 50))
                .migrationSize(request.getAlgorithmParameter("migrationSize", 5))
                .enableLocalSearch(request.getAlgorithmParameter("enableLocalSearch", true))
                .build();

        cancelled = false;
        currentGeneration = 0;
        optimizationCache.clear();
        
        // Initialize executor service for each run
        if (islandExecutor != null && !islandExecutor.isShutdown()) {
            islandExecutor.shutdown();
        }
        islandExecutor = Executors.newFixedThreadPool(config.numberOfIslands);

        log.info("Advanced GA Config: pop={}, gen={}, islands={}, migration={}gen",
                config.populationSize, config.maxGenerations, config.numberOfIslands, config.migrationInterval);
    }

    private void cleanup() {
        optimizationCache.cleanup();
        if (islandExecutor != null && !islandExecutor.isShutdown()) {
            islandExecutor.shutdown();
            try {
                if (!islandExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    islandExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                islandExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.debug("Advanced GA cleanup completed");
    }

    // === OPTIMIZER INTERFACE ===

    @Override
    public String getAlgorithmName() {
        return "ADVANCED_GENETIC_ALGORITHM_ISLAND_MODEL";
    }

    @Override
    public String getAlgorithmDescription() {
        return "Advanced Genetic Algorithm with Island Model, Migration, Task Splitting, Constraint Relaxation, and Performance Optimization";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        GAConfig defaultConfig = GAConfig.getDefault();
        Map<String, Object> params = new HashMap<>();
        params.put("populationSize", defaultConfig.populationSize);
        params.put("maxGenerations", defaultConfig.maxGenerations);
        params.put("mutationRate", defaultConfig.mutationRate);
        params.put("crossoverRate", defaultConfig.crossoverRate);
        params.put("eliteRate", defaultConfig.eliteRate);
        params.put("tournamentSize", defaultConfig.tournamentSize);
        params.put("stagnationLimit", defaultConfig.stagnationLimit);
        params.put("numberOfIslands", defaultConfig.numberOfIslands);
        params.put("migrationInterval", defaultConfig.migrationInterval);
        params.put("migrationSize", defaultConfig.migrationSize);
        params.put("enableLocalSearch", defaultConfig.enableLocalSearch);
        return params;
    }

    @Override
    public void validateRequest(OptimizationRequest request) throws OptimizationException {
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            throw new OptimizationException("Invalid request: " + e.getMessage(), e);
        }

        if (request.getActiveStaff().isEmpty()) {
            throw new OptimizationException("No active staff members found");
        }
        if (request.getActiveShifts().isEmpty()) {
            throw new OptimizationException("No active shifts found");
        }
        if (request.getPlanningDays() <= 0 || request.getPlanningDays() > 31) {
            throw new OptimizationException("Invalid planning period: " + request.getPlanningDays() + " days");
        }
    }

    @Override
    public boolean supportsParallelProcessing() {
        return true;
    }

    @Override
    public long getEstimatedExecutionTime(OptimizationRequest request) {
        int complexity = request.getActiveStaff().size() *
                request.getActiveTasks().size() *
                request.getPlanningDays();

        long baseTime = 15000; // 15 seconds base
        long complexityTime = complexity * 3; // 3ms per complexity unit
        long islandOverhead = config != null ? config.numberOfIslands * 2000 : 6000; // 2s per island

        return Math.max(baseTime, complexityTime + islandOverhead);
    }

    @Override
    public Map<String, ParameterInfo> getConfigurableParameters() {
        Map<String, ParameterInfo> params = new HashMap<>();

        params.put("populationSize", new ParameterInfo("populationSize",
                "Population size per island", Integer.class, 50, 20, 200));
        params.put("maxGenerations", new ParameterInfo("maxGenerations",
                "Maximum generations", Integer.class, 1000, 200, 5000));
        params.put("mutationRate", new ParameterInfo("mutationRate",
                "Mutation rate", Double.class, 0.1, 0.01, 0.5));
        params.put("crossoverRate", new ParameterInfo("crossoverRate",
                "Crossover rate", Double.class, 0.8, 0.5, 1.0));
        params.put("eliteRate", new ParameterInfo("eliteRate",
                "Elite preservation rate", Double.class, 0.1, 0.05, 0.3));
        params.put("numberOfIslands", new ParameterInfo("numberOfIslands",
                "Number of parallel islands", Integer.class, 3, 1, 5));
        params.put("migrationInterval", new ParameterInfo("migrationInterval",
                "Generations between migrations", Integer.class, 50, 20, 200));
        params.put("enableLocalSearch", new ParameterInfo("enableLocalSearch",
                "Enable local search optimization", Boolean.class, true));

        return params;
    }

    @Override
    public boolean cancelOptimization() {
        cancelled = true;
        log.info("Advanced GA optimization cancellation requested");
        return true;
    }

    @Override
    public int getOptimizationProgress() {
        if (config == null || currentGeneration == 0) return 0;
        return Math.min(100, (currentGeneration * 100) / config.maxGenerations);
    }
}