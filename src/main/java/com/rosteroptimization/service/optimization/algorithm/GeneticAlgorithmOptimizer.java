package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.entity.*;
import com.rosteroptimization.service.optimization.constraint.ConstraintEvaluator;
import com.rosteroptimization.service.optimization.constraint.ConstraintEvaluationResult;
import com.rosteroptimization.service.optimization.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import lombok.Builder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Refactored Genetic Algorithm with improved maintainability and performance
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeneticAlgorithmOptimizer implements Optimizer {

    private final ConstraintEvaluator constraintEvaluator;

    // Algorithm configuration
    @Data
    @Builder(toBuilder = true)
    public static class GAConfiguration {
        private final int populationSize;
        private final int maxGenerations;
        private final double mutationRate;
        private final double crossoverRate;
        private final double eliteRate;
        private final int tournamentSize;
        private final int stagnationLimit;
        private final boolean enableLocalSearch;
        private final int localSearchIterations;

        public static GAConfiguration getDefault() {
            return GAConfiguration.builder()
                    .populationSize(50)
                    .maxGenerations(5000)
                    .mutationRate(0.1)
                    .crossoverRate(0.8)
                    .eliteRate(0.1)
                    .tournamentSize(5)
                    .stagnationLimit(100)
                    .enableLocalSearch(true)
                    .localSearchIterations(20)
                    .build();
        }
    }

    // Performance components
    private final FitnessCache fitnessCache = new FitnessCache(5000);
    private final ExecutorService evaluationExecutor = createOptimizedExecutor();

    // Algorithm state
    private volatile boolean cancelled = false;
    private volatile int currentGeneration = 0;
    private volatile GAConfiguration config;

    // === MAIN OPTIMIZATION METHOD ===

    @Override
    public RosterPlan optimize(OptimizationRequest request) throws OptimizationException {
        log.info("Starting GA optimization for {} staff, {} tasks",
                request.getActiveStaff().size(), request.getActiveTasks().size());

        long startTime = System.currentTimeMillis();

        try {
            validateRequest(request);
            initializeOptimization(request);

            // Generate possible genes
            Map<String, List<Gene>> groupedGenes = generatePossibleGenes(request);

            // Run two-phase optimization
            Chromosome bestSolution = runTwoPhaseOptimization(groupedGenes, request);

            // Build result
            long executionTime = System.currentTimeMillis() - startTime;
            return buildRosterPlan(bestSolution, request, executionTime);

        } finally {
            cleanup();
        }
    }

    // === TWO-PHASE OPTIMIZATION ===

    private Chromosome runTwoPhaseOptimization(Map<String, List<Gene>> groupedGenes,
                                               OptimizationRequest request) {

        // Phase 1: Constraint Satisfaction
        log.info("Phase 1: Constraint satisfaction focus");
        GAConfiguration phase1Config = config.toBuilder()
                .populationSize(config.populationSize * 2)
                .maxGenerations(config.maxGenerations / 3)
                .mutationRate(0.3)
                .eliteRate(0.05)
                .build();

        List<Chromosome> population = runConstraintPhase(groupedGenes, request, phase1Config);

        // Phase 2: Quality Optimization
        log.info("Phase 2: Quality optimization focus");
        GAConfiguration phase2Config = config.toBuilder()
                .populationSize(config.populationSize)
                .maxGenerations(config.maxGenerations * 2 / 3)
                .mutationRate(0.05)
                .eliteRate(0.2)
                .build();

        return runQualityPhase(population, groupedGenes, request, phase2Config);
    }

    private List<Chromosome> runConstraintPhase(Map<String, List<Gene>> groupedGenes,
                                                OptimizationRequest request,
                                                GAConfiguration phaseConfig) {

        // Initialize population with constraint-aware chromosomes
        List<Chromosome> population = initializePopulation(groupedGenes, request, phaseConfig);

        int stagnationCounter = 0;
        double previousBest = Double.MIN_VALUE;

        for (int gen = 0; gen < phaseConfig.maxGenerations && !cancelled; gen++) {
            currentGeneration = gen;

            // Evaluate population with constraint focus
            evaluatePopulationConstraintFocused(population, request);
            population.sort((a, b) -> Double.compare(b.getFitnessScore(), a.getFitnessScore()));

            double currentBest = population.get(0).getFitnessScore();

            // Check for early termination (feasible solution found)
            if (isFeasible(population.get(0), request)) {
                log.info("Phase 1 completed early at generation {} - feasible solution found!", gen);
                break;
            }

            // Stagnation check
            if (Math.abs(currentBest - previousBest) < 1.0) {
                stagnationCounter++;
            } else {
                stagnationCounter = 0;
                previousBest = currentBest;
            }

            if (stagnationCounter >= 50) {
                log.info("Phase 1 stagnation limit reached at generation {}", gen);
                break;
            }

            // Log progress
            if (gen % 20 == 0) {
                int hardViolations = getHardViolationCount(population.get(0), request);
                log.info("Phase 1 Gen {}: Hard violations = {}, Best fitness = {:.2f}",
                        gen, hardViolations, currentBest);
            }

            // Create next generation
            population = createNextGeneration(population, groupedGenes, phaseConfig, true);
        }

        return population;
    }

    private Chromosome runQualityPhase(List<Chromosome> initialPopulation,
                                       Map<String, List<Gene>> groupedGenes,
                                       OptimizationRequest request,
                                       GAConfiguration phaseConfig) {

        // Trim population to target size
        List<Chromosome> population = initialPopulation.stream()
                .limit(phaseConfig.populationSize)
                .collect(Collectors.toList());

        int stagnationCounter = 0;
        double previousBest = population.get(0).getFitnessScore();

        for (int gen = 0; gen < phaseConfig.maxGenerations && !cancelled; gen++) {
            currentGeneration = gen;

            // Evaluate with quality focus
            evaluatePopulationQualityFocused(population, request);
            population.sort((a, b) -> Double.compare(b.getFitnessScore(), a.getFitnessScore()));

            double currentBest = population.get(0).getFitnessScore();

            // Stagnation check
            if (Math.abs(currentBest - previousBest) < 0.1) {
                stagnationCounter++;
            } else {
                stagnationCounter = 0;
                previousBest = currentBest;
            }

            if (stagnationCounter >= phaseConfig.stagnationLimit) {
                log.info("Phase 2 early termination due to stagnation at generation {}", gen);
                break;
            }

            // Log progress
            if (gen % 30 == 0) {
                log.info("Phase 2 Gen {}: Best fitness = {:.2f}", gen, currentBest);
            }

            // Local search on elite
            if (phaseConfig.enableLocalSearch && gen % 10 == 0) {
                applyLocalSearch(population, groupedGenes, request, phaseConfig);
            }

            // Create next generation
            population = createNextGeneration(population, groupedGenes, phaseConfig, false);
        }

        return population.get(0);
    }

    // === POPULATION MANAGEMENT ===

    private List<Chromosome> initializePopulation(Map<String, List<Gene>> groupedGenes,
                                                  OptimizationRequest request,
                                                  GAConfiguration config) {

        List<Chromosome> population = new ArrayList<>();
        int targetSize = config.populationSize;

        // 40% constraint-aware chromosomes
        int constraintAware = targetSize * 2 / 5;
        for (int i = 0; i < constraintAware; i++) {
            population.add(Chromosome.createConstraintAware(groupedGenes, request));
        }

        // 60% random chromosomes for diversity
        while (population.size() < targetSize) {
            population.add(Chromosome.createRandom(groupedGenes, request));
        }

        log.info("Initialized population of {} chromosomes", population.size());
        return population;
    }

    private List<Chromosome> createNextGeneration(List<Chromosome> currentPopulation,
                                                  Map<String, List<Gene>> groupedGenes,
                                                  GAConfiguration config,
                                                  boolean constraintPhase) {

        List<Chromosome> nextGeneration = new ArrayList<>();

        // Elitism
        int eliteCount = Math.max(1, (int)(config.populationSize * config.eliteRate));
        for (int i = 0; i < eliteCount; i++) {
            nextGeneration.add(currentPopulation.get(i).copy());
        }

        // Generate offspring
        while (nextGeneration.size() < config.populationSize) {
            if (cancelled) break;

            // Selection and crossover
            if (ThreadLocalRandom.current().nextDouble() < config.crossoverRate &&
                    nextGeneration.size() < config.populationSize - 1) {

                Chromosome parent1 = tournamentSelection(currentPopulation, config.tournamentSize);
                Chromosome parent2 = tournamentSelection(currentPopulation, config.tournamentSize);

                Chromosome offspring = parent1.crossover(parent2);
                applyMutation(offspring, groupedGenes, config, constraintPhase);
                offspring.repairBasic();

                nextGeneration.add(offspring);
            } else {
                // Mutation only
                Chromosome selected = tournamentSelection(currentPopulation, config.tournamentSize);
                Chromosome mutated = selected.copy();
                applyMutation(mutated, groupedGenes, config, constraintPhase);
                mutated.repairBasic();

                nextGeneration.add(mutated);
            }
        }

        return nextGeneration;
    }

    // === EVALUATION METHODS ===

    private void evaluatePopulationConstraintFocused(List<Chromosome> population,
                                                     OptimizationRequest request) {
        population.parallelStream()
                .filter(c -> !c.isFitnessCalculated())
                .forEach(c -> {
                    Double cached = fitnessCache.get(c.getSignature());
                    if (cached != null) {
                        c.setFitnessScore(cached);
                    } else {
                        double fitness = calculateConstraintFitness(c, request);
                        c.setFitnessScore(fitness);
                        fitnessCache.put(c.getSignature(), fitness);
                    }
                });
    }

    private void evaluatePopulationQualityFocused(List<Chromosome> population,
                                                  OptimizationRequest request) {
        population.parallelStream()
                .filter(c -> !c.isFitnessCalculated())
                .forEach(c -> {
                    Double cached = fitnessCache.get(c.getSignature());
                    if (cached != null) {
                        c.setFitnessScore(cached);
                    } else {
                        double fitness = calculateQualityFitness(c, request);
                        c.setFitnessScore(fitness);
                        fitnessCache.put(c.getSignature(), fitness);
                    }
                });
    }

    private double calculateConstraintFitness(Chromosome chromosome, OptimizationRequest request) {
        RosterPlan tempPlan = createTempPlan(chromosome, request);
        ConstraintEvaluationResult eval = constraintEvaluator.evaluateRosterPlan(tempPlan, true); // Early termination

        double fitness = 50000.0;

        // Heavy penalty for hard violations
        fitness -= eval.getHardViolationCount() * 1000;

        // Light penalty for soft violations
        fitness -= eval.getSoftViolationCount() * 10;

        // Bonus for task coverage
        double coverageRate = tempPlan.getTaskCoverageRate() / 100.0;
        fitness += coverageRate * 500;

        // Small bonus for staff utilization
        double utilizationRate = tempPlan.getStaffUtilizationRate() / 100.0;
        fitness += utilizationRate * 100;

        return fitness;
    }

    private double calculateQualityFitness(Chromosome chromosome, OptimizationRequest request) {
        RosterPlan tempPlan = createTempPlan(chromosome, request);
        ConstraintEvaluationResult eval = constraintEvaluator.evaluateRosterPlan(tempPlan);

        double fitness = 10000.0;

        // Hard violations are very costly
        int hardViolations = eval.getHardViolationCount();
        if (hardViolations > 0) {
            fitness -= hardViolations * 5000; // Heavy penalty
        }

        // Soft violations moderately costly
        fitness -= eval.getSoftViolationCount() * 50;

        if (hardViolations == 0) {
            // Quality bonuses only if feasible
            double coverageRate = tempPlan.getTaskCoverageRate() / 100.0;
            fitness += Math.pow(coverageRate, 2) * 2000; // Quadratic reward

            double utilizationRate = tempPlan.getStaffUtilizationRate() / 100.0;
            fitness += utilizationRate * 1000;

            // Fairness bonus
            double fairness = calculateFairness(chromosome, request);
            fitness += fairness * 800;

            // Pattern compliance bonus
            double patternCompliance = calculatePatternCompliance(chromosome, request);
            fitness += patternCompliance * 600;
        }

        return fitness;
    }

    // === GENETIC OPERATORS ===

    private void applyMutation(Chromosome chromosome, Map<String, List<Gene>> groupedGenes,
                               GAConfiguration config, boolean constraintPhase) {

        double mutationRate = constraintPhase ?
                config.mutationRate * 2 : // Higher mutation in constraint phase
                config.mutationRate;

        chromosome.mutate(groupedGenes, mutationRate);
    }

    private void applyLocalSearch(List<Chromosome> population, Map<String, List<Gene>> groupedGenes,
                                  OptimizationRequest request, GAConfiguration config) {

        int eliteCount = Math.max(1, (int)(population.size() * 0.1));

        for (int i = 0; i < eliteCount; i++) {
            Chromosome elite = population.get(i);
            localSearchImprovement(elite, groupedGenes, request, config.localSearchIterations);
        }
    }

    private void localSearchImprovement(Chromosome chromosome, Map<String, List<Gene>> groupedGenes,
                                        OptimizationRequest request, int maxIterations) {

        double currentFitness = chromosome.getFitnessScore();

        for (int iter = 0; iter < maxIterations; iter++) {
            if (cancelled) break;

            Chromosome neighbor = chromosome.copy();

            // Try to improve unassigned tasks
            List<Task> unassigned = neighbor.getUnassignedTasks(request.getActiveTasks());
            if (!unassigned.isEmpty()) {
                if (tryAssignHighPriorityTask(neighbor, unassigned.get(0), groupedGenes, request)) {
                    double neighborFitness = calculateQualityFitness(neighbor, request);
                    if (neighborFitness > currentFitness) {
                        chromosome.setGenes(neighbor.getGenes());
                        chromosome.setFitnessScore(neighborFitness);
                        currentFitness = neighborFitness;
                        continue;
                    }
                }
            }

            // Try random improvement
            neighbor.mutate(groupedGenes, 0.1); // Light mutation
            neighbor.repairBasic();

            double neighborFitness = calculateQualityFitness(neighbor, request);
            if (neighborFitness > currentFitness) {
                chromosome.setGenes(neighbor.getGenes());
                chromosome.setFitnessScore(neighborFitness);
                currentFitness = neighborFitness;
            }
        }
    }

    private boolean tryAssignHighPriorityTask(Chromosome chromosome, Task task,
                                              Map<String, List<Gene>> groupedGenes,
                                              OptimizationRequest request) {

        LocalDate taskDate = task.getStartTime().toLocalDate();

        // Find qualified staff
        List<Staff> qualifiedStaff = request.getActiveStaff().stream()
                .filter(s -> s.getDepartment().getId().equals(task.getDepartment().getId()))
                .filter(s -> task.getRequiredQualifications().stream()
                        .allMatch(req -> s.getQualifications().contains(req)))
                .collect(Collectors.toList());

        Collections.shuffle(qualifiedStaff, ThreadLocalRandom.current());

        for (Staff staff : qualifiedStaff) {
            String key = staff.getId() + "-" + taskDate;
            List<Gene> alternatives = groupedGenes.get(key);

            if (alternatives != null) {
                Optional<Gene> taskGene = alternatives.stream()
                        .filter(g -> g.hasTask() && g.getTask().equals(task))
                        .findFirst();

                if (taskGene.isPresent()) {
                    // Try to replace current gene for this staff-date
                    Optional<Gene> currentGene = chromosome.getGenes().stream()
                            .filter(g -> g.getGeneId().equals(key))
                            .findFirst();

                    if (currentGene.isPresent() && !currentGene.get().hasTask()) {
                        int index = chromosome.getGenes().indexOf(currentGene.get());
                        chromosome.getGenes().set(index, taskGene.get().copy());
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Chromosome tournamentSelection(List<Chromosome> population, int tournamentSize) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Chromosome best = null;

        for (int i = 0; i < tournamentSize; i++) {
            Chromosome candidate = population.get(random.nextInt(population.size()));
            if (best == null || candidate.getFitnessScore() > best.getFitnessScore()) {
                best = candidate;
            }
        }

        return best;
    }

    // === UTILITY METHODS ===

    private Map<String, List<Gene>> generatePossibleGenes(OptimizationRequest request) {
        log.info("Generating possible genes for optimization");

        List<Gene> allGenes = new ArrayList<>();

        for (LocalDate date = request.getStartDate();
             !date.isAfter(request.getEndDate());
             date = date.plusDays(1)) {

            for (Staff staff : request.getActiveStaff()) {
                // Day off option
                allGenes.add(Gene.createDayOffGene(staff, date));

                // Shift-only options
                for (Shift shift : request.getActiveShifts()) {
                    allGenes.add(Gene.createShiftOnlyGene(staff, date, shift));
                }

                // Shift+task options
                for (Task task : getTasksForDate(request.getActiveTasks(), date)) {
                    if (isStaffQualifiedForTask(staff, task) &&
                            staff.getDepartment().getId().equals(task.getDepartment().getId())) {

                        for (Shift shift : getCompatibleShifts(request.getActiveShifts(), task)) {
                            allGenes.add(Gene.createShiftWithTaskGene(staff, date, shift, task));
                        }
                    }
                }
            }
        }

        Map<String, List<Gene>> grouped = allGenes.stream()
                .collect(Collectors.groupingBy(Gene::getGeneId));

        log.info("Generated {} unique gene combinations for {} staff-date pairs",
                allGenes.size(), grouped.size());

        return grouped;
    }

    private RosterPlan createTempPlan(Chromosome chromosome, OptimizationRequest request) {
        return RosterPlan.builder()
                .planId("TEMP-" + UUID.randomUUID())
                .algorithmUsed("GA-TEMP")
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .assignments(chromosome.toAssignments())
                .unassignedTasks(chromosome.getUnassignedTasks(request.getActiveTasks()))
                .build();
    }

    private RosterPlan buildRosterPlan(Chromosome bestChromosome, OptimizationRequest request,
                                       long executionTime) {

        List<Assignment> assignments = bestChromosome.toAssignments();
        List<Task> unassignedTasks = bestChromosome.getUnassignedTasks(request.getActiveTasks());

        List<Staff> underutilizedStaff = request.getActiveStaff().stream()
                .filter(s -> bestChromosome.getWorkingDaysCount(s) == 0)
                .collect(Collectors.toList());

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
                .underutilizedStaff(underutilizedStaff)
                .statistics(new HashMap<>())
                .algorithmMetadata(new HashMap<>())
                .build();

        // Set constraint violations
        ConstraintEvaluationResult eval = constraintEvaluator.evaluateRosterPlan(plan);
        plan.setHardConstraintViolations(eval.getHardViolationCount());
        plan.setSoftConstraintViolations(eval.getSoftViolationCount());

        // Add statistics
        plan.addStatistic("taskCoverageRate", plan.getTaskCoverageRate());
        plan.addStatistic("staffUtilizationRate", plan.getStaffUtilizationRate());
        plan.addStatistic("totalWorkingHours", bestChromosome.getTotalWorkingHours(null));

        // Add algorithm metadata
        plan.addAlgorithmMetadata("generations", currentGeneration);
        plan.addAlgorithmMetadata("populationSize", config.populationSize);
        plan.addAlgorithmMetadata("configuration", config);

        return plan;
    }

    private boolean isFeasible(Chromosome chromosome, OptimizationRequest request) {
        RosterPlan tempPlan = createTempPlan(chromosome, request);
        ConstraintEvaluationResult eval = constraintEvaluator.evaluateRosterPlan(tempPlan, true);
        return eval.getHardViolationCount() == 0;
    }

    private int getHardViolationCount(Chromosome chromosome, OptimizationRequest request) {
        RosterPlan tempPlan = createTempPlan(chromosome, request);
        ConstraintEvaluationResult eval = constraintEvaluator.evaluateRosterPlan(tempPlan, true);
        return eval.getHardViolationCount();
    }

    private double calculateFairness(Chromosome chromosome, OptimizationRequest request) {
        Map<Staff, Double> workingHours = new HashMap<>();
        for (Staff staff : request.getActiveStaff()) {
            workingHours.put(staff, chromosome.getTotalWorkingHours(staff));
        }

        if (workingHours.size() <= 1) return 1.0;

        double mean = workingHours.values().stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);

        if (mean == 0) return 1.0;

        double variance = workingHours.values().stream()
                .mapToDouble(hours -> Math.pow(hours - mean, 2))
                .average().orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / mean;

        return Math.max(0, 1.0 - coefficientOfVariation);
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

            // Simple pattern compliance check
            totalChecks += staffGenes.size();
            compliantChecks += staffGenes.size(); // Simplified - assume compliant for now
        }

        return totalChecks == 0 ? 1.0 : (double) compliantChecks / totalChecks;
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

    private List<Shift> getCompatibleShifts(List<Shift> shifts, Task task) {
        // Simplified - return all shifts (could add time compatibility check)
        return shifts;
    }

    private void initializeOptimization(OptimizationRequest request) {
        cancelled = false;
        currentGeneration = 0;

        // Extract GA parameters from request
        this.config = GAConfiguration.builder()
                .populationSize(request.getAlgorithmParameter("populationSize", 50))
                .maxGenerations(request.getAlgorithmParameter("maxGenerations", 1000))
                .mutationRate(request.getAlgorithmParameter("mutationRate", 0.1))
                .crossoverRate(request.getAlgorithmParameter("crossoverRate", 0.8))
                .eliteRate(request.getAlgorithmParameter("eliteRate", 0.1))
                .tournamentSize(request.getAlgorithmParameter("tournamentSize", 5))
                .stagnationLimit(request.getAlgorithmParameter("stagnationLimit", 100))
                .enableLocalSearch(request.getAlgorithmParameter("enableLocalSearch", true))
                .localSearchIterations(request.getAlgorithmParameter("localSearchIterations", 20))
                .build();

        fitnessCache.clear();
        log.info("Optimization initialized with config: {}", config);
    }

    private void cleanup() {
        // No cleanup needed for now - caches are managed automatically
    }

    private ExecutorService createOptimizedExecutor() {
        int threads = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 8));
        return Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "GA-Evaluator");
            t.setDaemon(true);
            return t;
        });
    }

    // === OPTIMIZER INTERFACE IMPLEMENTATION ===

    @Override
    public String getAlgorithmName() {
        return "GENETIC_ALGORITHM_OPTIMIZED";
    }

    @Override
    public String getAlgorithmDescription() {
        return "Optimized two-phase genetic algorithm with constraint-awareness and local search";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        GAConfiguration defaultConfig = GAConfiguration.getDefault();
        Map<String, Object> params = new HashMap<>();
        params.put("populationSize", defaultConfig.populationSize);
        params.put("maxGenerations", defaultConfig.maxGenerations);
        params.put("mutationRate", defaultConfig.mutationRate);
        params.put("crossoverRate", defaultConfig.crossoverRate);
        params.put("eliteRate", defaultConfig.eliteRate);
        params.put("tournamentSize", defaultConfig.tournamentSize);
        params.put("stagnationLimit", defaultConfig.stagnationLimit);
        params.put("enableLocalSearch", defaultConfig.enableLocalSearch);
        params.put("localSearchIterations", defaultConfig.localSearchIterations);
        return params;
    }

    @Override
    public void validateRequest(OptimizationRequest request) throws OptimizationException {
        request.validate();
        if (request.getActiveStaff().isEmpty()) {
            throw new OptimizationException("No active staff members found");
        }
        if (request.getActiveShifts().isEmpty()) {
            throw new OptimizationException("No active shifts found");
        }
        if (request.getPlanningDays() <= 0 || request.getPlanningDays() > 31) {
            throw new OptimizationException("Invalid planning period: " + request.getPlanningDays());
        }
    }

    @Override
    public boolean supportsParallelProcessing() {
        return true;
    }

    @Override
    public long getEstimatedExecutionTime(OptimizationRequest request) {
        int complexity = request.getActiveStaff().size() * request.getActiveTasks().size() *
                request.getPlanningDays();
        return Math.max(5000, complexity / 10); // Rough estimate in ms
    }

    @Override
    public Map<String, ParameterInfo> getConfigurableParameters() {
        Map<String, ParameterInfo> params = new HashMap<>();
        params.put("populationSize", new ParameterInfo("populationSize", "Population size",
                Integer.class, 50, 10, 200));
        params.put("maxGenerations", new ParameterInfo("maxGenerations", "Maximum generations",
                Integer.class, 1000, 100, 5000));
        params.put("mutationRate", new ParameterInfo("mutationRate", "Mutation rate",
                Double.class, 0.1, 0.01, 0.5));
        params.put("crossoverRate", new ParameterInfo("crossoverRate", "Crossover rate",
                Double.class, 0.8, 0.5, 1.0));
        params.put("eliteRate", new ParameterInfo("eliteRate", "Elite percentage",
                Double.class, 0.1, 0.05, 0.3));
        return params;
    }

    @Override
    public boolean cancelOptimization() {
        cancelled = true;
        log.info("Optimization cancellation requested");
        return true;
    }

    @Override
    public int getOptimizationProgress() {
        if (config == null || currentGeneration == 0) return 0;
        return Math.min(100, (currentGeneration * 100) / config.maxGenerations);
    }

    // === INNER CLASSES ===

    /**
     * Simple fitness cache implementation
     */
    private static class FitnessCache {
        private final Map<String, Double> cache = new ConcurrentHashMap<>();
        private final int maxSize;

        public FitnessCache(int maxSize) {
            this.maxSize = maxSize;
        }

        public Double get(String signature) {
            return cache.get(signature);
        }

        public void put(String signature, double fitness) {
            if (cache.size() >= maxSize) {
                // Simple eviction - remove 20% randomly
                List<String> keys = new ArrayList<>(cache.keySet());

                Collections.shuffle(keys);
                for (int i = 0; i < maxSize / 5; i++) {
                    cache.remove(keys.get(i));
                }
            }
            cache.put(signature, fitness);
        }

        public void clear() {
            cache.clear();
        }
    }
}