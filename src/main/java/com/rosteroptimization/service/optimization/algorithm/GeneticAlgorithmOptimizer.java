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
import java.util.stream.Collectors;

/**
 * Genetic Algorithm implementation for roster optimization
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeneticAlgorithmOptimizer implements Optimizer {

    private final ConstraintEvaluator constraintEvaluator;

    // GA Parameters (configurable via request parameters)
    private static final int DEFAULT_POPULATION_SIZE = 50;
    private static final int DEFAULT_MAX_GENERATIONS = 1000;
    private static final double DEFAULT_MUTATION_RATE = 0.1;
    private static final double DEFAULT_CROSSOVER_RATE = 0.8;
    private static final double DEFAULT_ELITE_RATE = 0.1;
    private static final int DEFAULT_TOURNAMENT_SIZE = 5;
    private static final int DEFAULT_STAGNATION_LIMIT = 100;

    // Algorithm state
    private volatile boolean cancelled = false;
    private volatile int currentGeneration = 0;
    private volatile double bestFitness = Double.MIN_VALUE;

    @Override
    public RosterPlan optimize(OptimizationRequest request) throws OptimizationException {
        log.info("Starting Genetic Algorithm optimization for {} staff members, {} tasks",
                request.getActiveStaff().size(), request.getActiveTasks().size());

        // Validate request
        validateRequest(request);

        // Reset algorithm state
        reset();

        // Extract GA parameters
        GAParameters params = extractGAParameters(request);
        log.info("GA Parameters: {}", params);

        // Generate all possible genes for the problem
        List<Gene> possibleGenes = generatePossibleGenes(request);
        log.info("Generated {} possible genes", possibleGenes.size());

        // Initialize population
        List<Chromosome> population = initializePopulation(params.getPopulationSize(), possibleGenes, request);
        log.info("Initialized population with {} chromosomes", population.size());

        // Evolution loop
        long startTime = System.currentTimeMillis();
        List<Double> fitnessHistory = new ArrayList<>();
        int stagnationCounter = 0;

        for (currentGeneration = 0; currentGeneration < params.getMaxGenerations() && !cancelled; currentGeneration++) {

            // Evaluate fitness for all chromosomes
            evaluatePopulation(population, request);

            // Sort by fitness (descending - higher is better)
            population.sort((c1, c2) -> Double.compare(c2.getFitnessScore(), c1.getFitnessScore()));

            // Track best fitness
            double currentBestFitness = population.get(0).getFitnessScore();
            fitnessHistory.add(currentBestFitness);

            // Check for improvement
            if (currentBestFitness > bestFitness) {
                bestFitness = currentBestFitness;
                stagnationCounter = 0;
                log.debug("Generation {}: New best fitness = {:.2f}", currentGeneration, bestFitness);
            } else {
                stagnationCounter++;
            }

            // Early termination if stagnated
            if (stagnationCounter >= params.getStagnationLimit()) {
                log.info("Early termination due to stagnation after {} generations", currentGeneration);
                break;
            }

            // Log progress periodically
            if (currentGeneration % 50 == 0) {
                log.info("Generation {}: Best fitness = {:.2f}, Average fitness = {:.2f}",
                        currentGeneration, currentBestFitness, getAverageFitness(population));
            }

            // Create next generation
            population = createNextGeneration(population, params, possibleGenes, request);
        }

        long executionTime = System.currentTimeMillis() - startTime;

        // Get best solution
        Chromosome bestChromosome = population.get(0);

        // Convert to roster plan
        RosterPlan rosterPlan = convertToRosterPlan(bestChromosome, request, executionTime);

        // Add algorithm metadata
        rosterPlan.addAlgorithmMetadata("generations", currentGeneration);
        rosterPlan.addAlgorithmMetadata("finalPopulationSize", population.size());
        rosterPlan.addAlgorithmMetadata("stagnationCounter", stagnationCounter);
        rosterPlan.addAlgorithmMetadata("fitnessHistory", fitnessHistory);
        rosterPlan.addAlgorithmMetadata("parameters", params.toMap());

        log.info("GA optimization completed in {} ms, {} generations, final fitness: {:.2f}",
                executionTime, currentGeneration, bestChromosome.getFitnessScore());

        return rosterPlan;
    }

    /**
     * Generate all possible genes for the optimization problem
     */
    private List<Gene> generatePossibleGenes(OptimizationRequest request) {
        List<Gene> possibleGenes = new ArrayList<>();
        List<Staff> activeStaff = request.getActiveStaff();
        List<Shift> activeShifts = request.getActiveShifts();
        List<Task> activeTasks = request.getActiveTasks();

        // Generate genes for each staff member for each day in the planning period
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {

            for (Staff staff : activeStaff) {

                // Day off gene
                possibleGenes.add(Gene.createDayOffGene(staff, currentDate));

                // Shift-only genes (for each compatible shift)
                for (Shift shift : activeShifts) {
                    possibleGenes.add(Gene.createShiftOnlyGene(staff, currentDate, shift));
                }

                // Shift + Task genes (for each compatible combination)
                for (Task task : activeTasks) {
                    // Check if task is scheduled for this date
                    if (isTaskScheduledForDate(task, currentDate)) {
                        // Check if staff is qualified for this task
                        if (isStaffQualifiedForTask(staff, task)) {
                            // Check department compatibility
                            if (staff.getDepartment().getId().equals(task.getDepartment().getId())) {
                                // Find compatible shifts for this task
                                for (Shift shift : getCompatibleShiftsForTask(activeShifts, task, currentDate)) {
                                    possibleGenes.add(Gene.createShiftWithTaskGene(staff, currentDate, shift, task));
                                }
                            }
                        }
                    }
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return possibleGenes;
    }

    /**
     * Initialize population with random chromosomes
     */
    private List<Chromosome> initializePopulation(int populationSize, List<Gene> possibleGenes,
                                                  OptimizationRequest request) {
        List<Chromosome> population = new ArrayList<>();
        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < populationSize; i++) {
            Chromosome chromosome = createRandomChromosome(possibleGenes, request, random);
            population.add(chromosome);
        }

        return population;
    }

    /**
     * Create a random valid chromosome
     */
    private Chromosome createRandomChromosome(List<Gene> possibleGenes, OptimizationRequest request, Random random) {
        Map<String, Gene> selectedGenes = new HashMap<>(); // staffId-date -> Gene

        // Group possible genes by staff and date
        Map<String, List<Gene>> genesByStaffAndDate = possibleGenes.stream()
                .collect(Collectors.groupingBy(gene -> gene.getStaff().getId() + "-" + gene.getDate()));

        // For each staff-date combination, randomly select one gene
        for (Map.Entry<String, List<Gene>> entry : genesByStaffAndDate.entrySet()) {
            List<Gene> candidateGenes = entry.getValue();

            // Weighted random selection (prefer day-off and shift-only over complex assignments)
            Gene selectedGene = selectRandomGeneWithBias(candidateGenes, random);
            selectedGenes.put(entry.getKey(), selectedGene);
        }

        return new Chromosome(new ArrayList<>(selectedGenes.values()));
    }

    /**
     * Select random gene with bias towards simpler assignments
     */
    private Gene selectRandomGeneWithBias(List<Gene> candidateGenes, Random random) {
        // Create weighted list (day-off and shift-only have higher probability)
        List<Gene> weightedGenes = new ArrayList<>();

        for (Gene gene : candidateGenes) {
            if (gene.isDayOff()) {
                // Add day-off genes 3 times (higher probability)
                Collections.addAll(weightedGenes, gene, gene, gene);
            } else if (!gene.hasTask()) {
                // Add shift-only genes 2 times
                Collections.addAll(weightedGenes, gene, gene);
            } else {
                // Add task genes once
                weightedGenes.add(gene);
            }
        }

        return weightedGenes.get(random.nextInt(weightedGenes.size()));
    }

    /**
     * Evaluate fitness for all chromosomes in population
     */
    private void evaluatePopulation(List<Chromosome> population, OptimizationRequest request) {
        population.parallelStream().forEach(chromosome -> {
            if (!chromosome.isFitnessCalculated()) {
                double fitness = calculateFitness(chromosome, request);
                chromosome.setFitnessScore(fitness);
            }
        });
    }

    /**
     * Calculate fitness score for a chromosome
     */
    private double calculateFitness(Chromosome chromosome, OptimizationRequest request) {
        // Convert chromosome to roster plan for evaluation
        RosterPlan tempPlan = RosterPlan.builder()
                .assignments(chromosome.toAssignments())
                .unassignedTasks(chromosome.getUnassignedTasks(request.getActiveTasks()))
                .build();

        // Evaluate constraints
        ConstraintEvaluationResult evaluation = constraintEvaluator.evaluateRosterPlan(tempPlan);

        // Calculate fitness score
        // Higher score is better
        // Start with base score and subtract penalties
        double fitness = 10000.0; // Base score

        // Heavy penalty for hard constraint violations
        fitness -= evaluation.getHardViolationCount() * 1000.0;

        // Light penalty for soft constraint violations
        fitness -= evaluation.getSoftViolationCount() * 10.0;

        // Bonus for task coverage
        double taskCoverageRate = tempPlan.getTaskCoverageRate();
        fitness += taskCoverageRate * 50.0; // Max 5000 bonus for 100% coverage

        // Bonus for staff utilization
        double staffUtilizationRate = tempPlan.getStaffUtilizationRate();
        fitness += staffUtilizationRate * 20.0; // Max 2000 bonus for 100% utilization

        // Small penalty for excessive working hours (encourage balance)
        double totalHours = chromosome.getTotalWorkingHours();
        double expectedHours = request.getActiveStaff().size() * request.getPlanningDays() * 8.0; // 8h per day expected
        double hoursDeviation = Math.abs(totalHours - expectedHours);
        fitness -= hoursDeviation * 0.1;

        return fitness;
    }

    /**
     * Create next generation using selection, crossover, and mutation
     */
    private List<Chromosome> createNextGeneration(List<Chromosome> currentPopulation, GAParameters params,
                                                  List<Gene> possibleGenes, OptimizationRequest request) {
        List<Chromosome> nextGeneration = new ArrayList<>();
        Random random = new Random();

        // Elite selection - keep best chromosomes
        int eliteCount = (int) (currentPopulation.size() * params.getEliteRate());
        for (int i = 0; i < eliteCount; i++) {
            nextGeneration.add(currentPopulation.get(i).copy());
        }

        // Fill rest with crossover and mutation
        while (nextGeneration.size() < params.getPopulationSize()) {

            if (random.nextDouble() < params.getCrossoverRate() && nextGeneration.size() < params.getPopulationSize() - 1) {
                // Crossover
                Chromosome parent1 = tournamentSelection(currentPopulation, params.getTournamentSize(), random);
                Chromosome parent2 = tournamentSelection(currentPopulation, params.getTournamentSize(), random);

                Chromosome offspring1 = parent1.crossover(parent2, random);
                Chromosome offspring2 = parent2.crossover(parent1, random);

                // Mutate offspring
                if (random.nextDouble() < params.getMutationRate()) {
                    offspring1.mutateRandomGene(random, possibleGenes);
                }
                if (random.nextDouble() < params.getMutationRate()) {
                    offspring2.mutateRandomGene(random, possibleGenes);
                }

                nextGeneration.add(offspring1);
                if (nextGeneration.size() < params.getPopulationSize()) {
                    nextGeneration.add(offspring2);
                }

            } else {
                // Direct selection with mutation
                Chromosome selected = tournamentSelection(currentPopulation, params.getTournamentSize(), random);
                Chromosome mutated = selected.copy();

                if (random.nextDouble() < params.getMutationRate()) {
                    mutated.mutateRandomGene(random, possibleGenes);
                }

                nextGeneration.add(mutated);
            }
        }

        return nextGeneration;
    }

    /**
     * Tournament selection
     */
    private Chromosome tournamentSelection(List<Chromosome> population, int tournamentSize, Random random) {
        Chromosome best = null;

        for (int i = 0; i < tournamentSize; i++) {
            Chromosome candidate = population.get(random.nextInt(population.size()));
            if (best == null || candidate.getFitnessScore() > best.getFitnessScore()) {
                best = candidate;
            }
        }

        return best;
    }

    /**
     * Convert best chromosome to roster plan
     */
    private RosterPlan convertToRosterPlan(Chromosome bestChromosome, OptimizationRequest request, long executionTime) {
        List<Assignment> assignments = bestChromosome.toAssignments();
        List<Task> unassignedTasks = bestChromosome.getUnassignedTasks(request.getActiveTasks());

        // Find underutilized staff
        List<Staff> underutilizedStaff = request.getActiveStaff().stream()
                .filter(staff -> bestChromosome.getWorkingDaysCount(staff) == 0)
                .toList();

        // Evaluate final constraints
        RosterPlan rosterPlan = RosterPlan.builder()
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

        // Final constraint evaluation
        ConstraintEvaluationResult evaluation = constraintEvaluator.evaluateRosterPlan(rosterPlan);
        rosterPlan.setHardConstraintViolations(evaluation.getHardViolationCount());
        rosterPlan.setSoftConstraintViolations(evaluation.getSoftViolationCount());

        // Add statistics
        rosterPlan.addStatistic("taskCoverageRate", rosterPlan.getTaskCoverageRate());
        rosterPlan.addStatistic("staffUtilizationRate", rosterPlan.getStaffUtilizationRate());
        rosterPlan.addStatistic("totalWorkingHours", bestChromosome.getTotalWorkingHours());
        rosterPlan.addStatistic("chromosomeStats", bestChromosome.getStatistics());

        return rosterPlan;
    }

    /**
     * Helper methods
     */
    private boolean isTaskScheduledForDate(Task task, LocalDate date) {
        LocalDate taskDate = task.getStartTime().toLocalDate();
        return taskDate.equals(date);
    }

    private boolean isStaffQualifiedForTask(Staff staff, Task task) {
        return task.getRequiredQualifications().stream()
                .allMatch(required -> staff.getQualifications().contains(required));
    }

    private List<Shift> getCompatibleShiftsForTask(List<Shift> shifts, Task task, LocalDate date) {
        return shifts.stream()
                .filter(shift -> {
                    // Check if shift time overlaps with task time
                    // This is a simplified check - in real implementation you'd need more sophisticated logic
                    return true; // For now, assume all shifts are compatible
                })
                .toList();
    }

    private double getAverageFitness(List<Chromosome> population) {
        return population.stream()
                .mapToDouble(Chromosome::getFitnessScore)
                .average()
                .orElse(0.0);
    }

    /**
     * Extract GA parameters from request
     */
    private GAParameters extractGAParameters(OptimizationRequest request) {
        return GAParameters.builder()
                .populationSize(request.getAlgorithmParameter("populationSize", DEFAULT_POPULATION_SIZE))
                .maxGenerations(request.getAlgorithmParameter("maxGenerations", DEFAULT_MAX_GENERATIONS))
                .mutationRate(request.getAlgorithmParameter("mutationRate", DEFAULT_MUTATION_RATE))
                .crossoverRate(request.getAlgorithmParameter("crossoverRate", DEFAULT_CROSSOVER_RATE))
                .eliteRate(request.getAlgorithmParameter("eliteRate", DEFAULT_ELITE_RATE))
                .tournamentSize(request.getAlgorithmParameter("tournamentSize", DEFAULT_TOURNAMENT_SIZE))
                .stagnationLimit(request.getAlgorithmParameter("stagnationLimit", DEFAULT_STAGNATION_LIMIT))
                .build();
    }

    private void reset() {
        cancelled = false;
        currentGeneration = 0;
        bestFitness = Double.MIN_VALUE;
    }

    @Override
    public String getAlgorithmName() {
        return "GENETIC_ALGORITHM";
    }

    @Override
    public String getAlgorithmDescription() {
        return "Genetic Algorithm optimizer using tournament selection, uniform crossover, and random mutation";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("populationSize", DEFAULT_POPULATION_SIZE);
        params.put("maxGenerations", DEFAULT_MAX_GENERATIONS);
        params.put("mutationRate", DEFAULT_MUTATION_RATE);
        params.put("crossoverRate", DEFAULT_CROSSOVER_RATE);
        params.put("eliteRate", DEFAULT_ELITE_RATE);
        params.put("tournamentSize", DEFAULT_TOURNAMENT_SIZE);
        params.put("stagnationLimit", DEFAULT_STAGNATION_LIMIT);
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

        if (request.getPlanningDays() <= 0) {
            throw new OptimizationException("Invalid planning period");
        }

        if (request.getPlanningDays() > 31) {
            throw new OptimizationException("Planning period too long (max 31 days)");
        }
    }

    @Override
    public boolean supportsParallelProcessing() {
        return true;
    }

    @Override
    public long getEstimatedExecutionTime(OptimizationRequest request) {
        // Rough estimation based on problem size
        int staffCount = request.getActiveStaff().size();
        int taskCount = request.getActiveTasks().size();
        int days = request.getPlanningDays();

        // Base time + complexity factor
        long baseTime = 5000; // 5 seconds base
        long complexityFactor = (staffCount * taskCount * days) / 10;

        return baseTime + complexityFactor;
    }

    @Override
    public Map<String, ParameterInfo> getConfigurableParameters() {
        Map<String, ParameterInfo> params = new HashMap<>();

        params.put("populationSize", new ParameterInfo(
                "populationSize", "Population size for genetic algorithm",
                Integer.class, DEFAULT_POPULATION_SIZE, 10, 200));

        params.put("maxGenerations", new ParameterInfo(
                "maxGenerations", "Maximum number of generations",
                Integer.class, DEFAULT_MAX_GENERATIONS, 100, 5000));

        params.put("mutationRate", new ParameterInfo(
                "mutationRate", "Probability of mutation",
                Double.class, DEFAULT_MUTATION_RATE, 0.01, 0.5));

        params.put("crossoverRate", new ParameterInfo(
                "crossoverRate", "Probability of crossover",
                Double.class, DEFAULT_CROSSOVER_RATE, 0.5, 1.0));

        params.put("eliteRate", new ParameterInfo(
                "eliteRate", "Percentage of elite chromosomes to keep",
                Double.class, DEFAULT_ELITE_RATE, 0.05, 0.3));

        params.put("tournamentSize", new ParameterInfo(
                "tournamentSize", "Tournament size for selection",
                Integer.class, DEFAULT_TOURNAMENT_SIZE, 2, 10));

        params.put("stagnationLimit", new ParameterInfo(
                "stagnationLimit", "Generations without improvement before stopping",
                Integer.class, DEFAULT_STAGNATION_LIMIT, 50, 500));

        return params;
    }

    @Override
    public boolean cancelOptimization() {
        cancelled = true;
        log.info("GA optimization cancellation requested");
        return true;
    }

    @Override
    public int getOptimizationProgress() {
        if (currentGeneration == 0) return 0;

        GAParameters params = GAParameters.builder()
                .maxGenerations(DEFAULT_MAX_GENERATIONS)
                .build();

        return Math.min(100, (currentGeneration * 100) / params.getMaxGenerations());
    }

    /**
     * GA Parameters data class
     */
    @Data
    @Builder
    private static class GAParameters {
        private int populationSize;
        private int maxGenerations;
        private double mutationRate;
        private double crossoverRate;
        private double eliteRate;
        private int tournamentSize;
        private int stagnationLimit;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("populationSize", populationSize);
            map.put("maxGenerations", maxGenerations);
            map.put("mutationRate", mutationRate);
            map.put("crossoverRate", crossoverRate);
            map.put("eliteRate", eliteRate);
            map.put("tournamentSize", tournamentSize);
            map.put("stagnationLimit", stagnationLimit);
            return map;
        }

        @Override
        public String toString() {
            return String.format("GAParameters{pop=%d, gen=%d, mut=%.2f, cross=%.2f, elite=%.2f, tour=%d, stag=%d}",
                    populationSize, maxGenerations, mutationRate, crossoverRate, eliteRate, tournamentSize, stagnationLimit);
        }
    }
}