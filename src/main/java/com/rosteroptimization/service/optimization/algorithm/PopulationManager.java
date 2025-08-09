package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Task;
import com.rosteroptimization.service.optimization.model.OptimizationRequest;
import com.rosteroptimization.service.optimization.model.Population;
import com.rosteroptimization.service.optimization.model.Chromosome;
import com.rosteroptimization.service.optimization.model.Gene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Manages population creation, maintenance, and evolution operations
 */
@Component
@Slf4j
public class PopulationManager {

    /**
     * Create initial population with diverse initialization strategies
     */
    public Population createInitialPopulation(int populationSize, Map<String, List<Gene>> geneSpace,
                                              OptimizationRequest request) {
        log.debug("Creating initial population of size {}", populationSize);

        List<Chromosome> chromosomes = new ArrayList<>();

        // 30% constraint-aware chromosomes (focus on feasibility)
        int constraintAware = populationSize * 3 / 10;
        for (int i = 0; i < constraintAware; i++) {
            chromosomes.add(Chromosome.createConstraintAware(geneSpace, request));
        }

        // 20% greedy chromosomes (focus on high-priority tasks)
        int greedy = populationSize * 2 / 10;
        for (int i = 0; i < greedy; i++) {
            chromosomes.add(createGreedyChromosome(geneSpace, request));
        }

        // 50% random chromosomes (maintain diversity)
        while (chromosomes.size() < populationSize) {
            chromosomes.add(Chromosome.createRandom(geneSpace, request));
        }

        log.debug("Created initial population: {} constraint-aware, {} greedy, {} random",
                constraintAware, greedy, populationSize - constraintAware - greedy);

        return new Population(chromosomes);
    }

    /**
     * Create greedy chromosome that prioritizes high-priority tasks
     */
    private Chromosome createGreedyChromosome(Map<String, List<Gene>> geneSpace, OptimizationRequest request) {
        Map<String, Gene> selected = new HashMap<>();
        Set<Task> assignedTasks = new HashSet<>();

        // Sort tasks by priority
        List<Task> prioritizedTasks = request.getActiveTasks().stream()
                .sorted(Comparator.comparingInt(Task::getPriority)
                        .thenComparing(task -> -calculateTaskHours(task))) // Then by duration (longer first)
                .collect(Collectors.toList());

        // Assign high-priority tasks first
        for (Task task : prioritizedTasks) {
            if (assignedTasks.contains(task)) continue;

            LocalDate taskDate = task.getStartTime().toLocalDate();

            // Find best staff for this task
            Optional<Staff> bestStaff = findBestStaffForTask(task, request.getActiveStaff(), selected, taskDate);

            if (bestStaff.isPresent()) {
                Staff staff = bestStaff.get();
                String key = staff.getId() + "-" + taskDate;

                // Find gene that includes this task
                Optional<Gene> taskGene = geneSpace.getOrDefault(key, Collections.emptyList())
                        .stream()
                        .filter(g -> g.hasTasks() && g.getTasks().contains(task))
                        .filter(g -> !hasConflictingTasks(g, assignedTasks))
                        .findFirst();

                if (taskGene.isPresent()) {
                    selected.put(key, taskGene.get());
                    assignedTasks.addAll(taskGene.get().getTasks());
                }
            }
        }

        // Fill remaining slots with reasonable choices
        for (String key : geneSpace.keySet()) {
            if (!selected.containsKey(key)) {
                List<Gene> candidates = geneSpace.get(key);
                if (!candidates.isEmpty()) {
                    // Prefer working genes over day-off, but avoid conflicts
                    Gene selectedGene = candidates.stream()
                            .filter(g -> !hasConflictingTasks(g, assignedTasks))
                            .filter(g -> !g.isDayOff())
                            .findFirst()
                            .orElse(candidates.stream()
                                    .filter(g -> !hasConflictingTasks(g, assignedTasks))
                                    .findFirst()
                                    .orElse(candidates.get(0)));

                    selected.put(key, selectedGene);
                    if (selectedGene.hasTasks()) {
                        assignedTasks.addAll(selectedGene.getTasks());
                    }
                }
            }
        }

        return new Chromosome(new ArrayList<>(selected.values()));
    }

    /**
     * Find best staff member for a specific task
     */
    private Optional<Staff> findBestStaffForTask(Task task, List<Staff> availableStaff,
                                                 Map<String, Gene> currentAssignments, LocalDate taskDate) {
        return availableStaff.stream()
                .filter(staff -> staff.getDepartment().getId().equals(task.getDepartment().getId()))
                .filter(staff -> task.getRequiredQualifications().stream()
                        .allMatch(req -> staff.getQualifications().contains(req)))
                .filter(staff -> !isStaffOverloaded(staff, currentAssignments, taskDate))
                .min(Comparator.comparingDouble(staff -> calculateStaffWorkload(staff, currentAssignments)));
    }

    /**
     * Check if staff member is already overloaded
     */
    private boolean isStaffOverloaded(Staff staff, Map<String, Gene> currentAssignments, LocalDate taskDate) {
        // Check current week workload
        LocalDate weekStart = taskDate.minusDays(taskDate.getDayOfWeek().getValue() - 1);

        double weeklyHours = 0;
        int workingDays = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate checkDate = weekStart.plusDays(i);
            String key = staff.getId() + "-" + checkDate;
            Gene gene = currentAssignments.get(key);

            if (gene != null && !gene.isDayOff()) {
                weeklyHours += gene.getWorkingHours();
                workingDays++;
            }
        }

        return weeklyHours >= 35 || workingDays >= 5; // Conservative limits
    }

    /**
     * Calculate current workload for staff member
     */
    private double calculateStaffWorkload(Staff staff, Map<String, Gene> currentAssignments) {
        return currentAssignments.values().stream()
                .filter(gene -> gene.getStaff().getId().equals(staff.getId()))
                .mapToDouble(Gene::getWorkingHours)
                .sum();
    }

    /**
     * Check if gene has conflicting tasks with already assigned tasks
     */
    private boolean hasConflictingTasks(Gene gene, Set<Task> assignedTasks) {
        if (!gene.hasTasks()) return false;

        return gene.getTasks().stream().anyMatch(assignedTasks::contains);
    }

    /**
     * Calculate task duration in hours
     */
    private double calculateTaskHours(Task task) {
        long minutes = java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
        return minutes / 60.0;
    }

    /**
     * Inject diversity into population to prevent premature convergence
     */
    public void injectDiversity(Population population, Map<String, List<Gene>> geneSpace,
                                OptimizationRequest request, double diversityThreshold) {
        double currentDiversity = calculatePopulationDiversity(population);

        if (currentDiversity < diversityThreshold) {
            int diversityInjectionCount = (int)(population.size() * 0.1); // Replace 10%

            log.debug("Injecting diversity: current={:.3f}, threshold={:.3f}, replacing={} chromosomes",
                    currentDiversity, diversityThreshold, diversityInjectionCount);

            // Replace worst chromosomes with new random ones
            List<Chromosome> chromosomes = population.getChromosomes();
            for (int i = chromosomes.size() - diversityInjectionCount; i < chromosomes.size(); i++) {
                chromosomes.set(i, Chromosome.createRandom(geneSpace, request));
            }

            population.invalidateCache();
        }
    }

    /**
     * Calculate population diversity using Hamming distance
     */
    private double calculatePopulationDiversity(Population population) {
        List<Chromosome> chromosomes = population.getChromosomes();
        if (chromosomes.size() < 2) return 1.0;

        int comparisons = 0;
        double totalDistance = 0;
        int maxComparisons = Math.min(100, (chromosomes.size() * (chromosomes.size() - 1)) / 2);

        for (int i = 0; i < chromosomes.size() && comparisons < maxComparisons; i++) {
            for (int j = i + 1; j < chromosomes.size() && comparisons < maxComparisons; j++) {
                totalDistance += calculateHammingDistance(chromosomes.get(i), chromosomes.get(j));
                comparisons++;
            }
        }

        return comparisons > 0 ? totalDistance / comparisons : 0;
    }

    /**
     * Calculate Hamming distance between two chromosomes
     */
    private double calculateHammingDistance(Chromosome c1, Chromosome c2) {
        Map<String, Gene> genes1 = c1.getGenes().stream()
                .collect(Collectors.toMap(Gene::getGeneId, g -> g));
        Map<String, Gene> genes2 = c2.getGenes().stream()
                .collect(Collectors.toMap(Gene::getGeneId, g -> g));

        Set<String> allGeneIds = new HashSet<>(genes1.keySet());
        allGeneIds.addAll(genes2.keySet());

        if (allGeneIds.isEmpty()) return 0;

        int differences = 0;
        for (String geneId : allGeneIds) {
            Gene gene1 = genes1.get(geneId);
            Gene gene2 = genes2.get(geneId);

            if (!Objects.equals(gene1, gene2)) {
                differences++;
            }
        }

        return (double) differences / allGeneIds.size();
    }

    /**
     * Perform population maintenance operations
     */
    public void performMaintenance(Population population, Map<String, List<Gene>> geneSpace,
                                   OptimizationRequest request, int generation) {

        // Diversity injection every 25 generations
        if (generation % 25 == 0 && generation > 0) {
            injectDiversity(population, geneSpace, request, 0.3);
        }

        // Population repair every 50 generations
        if (generation % 50 == 0) {
            repairPopulation(population);
        }

        // Adaptive population size adjustment (experimental)
        if (generation % 100 == 0 && generation > 0) {
            considerPopulationSizeAdjustment(population, geneSpace, request);
        }
    }

    /**
     * Repair population by fixing obvious violations
     */
    private void repairPopulation(Population population) {
        int repaired = 0;
        for (Chromosome chromosome : population.getChromosomes()) {
            if (chromosome.hasBasicViolations()) {
                chromosome.repairBasic();
                repaired++;
            }
        }

        if (repaired > 0) {
            log.debug("Repaired {} chromosomes in population maintenance", repaired);
            population.invalidateCache();
        }
    }

    /**
     * Consider dynamic population size adjustment based on convergence
     */
    private void considerPopulationSizeAdjustment(Population population, Map<String, List<Gene>> geneSpace,
                                                  OptimizationRequest request) {
        double diversity = calculatePopulationDiversity(population);

        // If diversity is very low, temporarily expand population
        if (diversity < 0.1 && population.size() < 100) {
            int addCount = Math.min(10, 100 - population.size());

            for (int i = 0; i < addCount; i++) {
                population.addChromosome(Chromosome.createRandom(geneSpace, request));
            }

            log.debug("Expanded population by {} chromosomes due to low diversity ({:.3f})",
                    addCount, diversity);
        }
    }

    /**
     * Create specialized population for specific optimization phase
     */
    public Population createSpecializedPopulation(int size, Map<String, List<Gene>> geneSpace,
                                                  OptimizationRequest request, PopulationSpecialization specialization) {

        List<Chromosome> chromosomes = new ArrayList<>();

        switch (specialization) {
            case CONSTRAINT_FOCUSED -> {
                // All constraint-aware chromosomes
                for (int i = 0; i < size; i++) {
                    chromosomes.add(Chromosome.createConstraintAware(geneSpace, request));
                }
            }
            case TASK_COVERAGE_FOCUSED -> {
                // All greedy chromosomes
                for (int i = 0; i < size; i++) {
                    chromosomes.add(createGreedyChromosome(geneSpace, request));
                }
            }
            case WORKLOAD_BALANCED -> {
                // Mix with focus on workload balancing
                for (int i = 0; i < size; i++) {
                    Chromosome chromosome = i % 2 == 0 ?
                            Chromosome.createConstraintAware(geneSpace, request) :
                            createWorkloadBalancedChromosome(geneSpace, request);
                    chromosomes.add(chromosome);
                }
            }
            case DIVERSE -> {
                // Maximum diversity
                for (int i = 0; i < size; i++) {
                    chromosomes.add(Chromosome.createRandom(geneSpace, request));
                }
            }
        }

        return new Population(chromosomes);
    }

    /**
     * Create chromosome with focus on workload balancing
     */
    private Chromosome createWorkloadBalancedChromosome(Map<String, List<Gene>> geneSpace,
                                                        OptimizationRequest request) {
        Map<String, Gene> selected = new HashMap<>();
        Map<Long, Double> staffWorkloads = new HashMap<>();

        // Initialize workload tracking
        for (Staff staff : request.getActiveStaff()) {
            staffWorkloads.put(staff.getId(), 0.0);
        }

        // Process dates to balance workload
        List<LocalDate> dates = request.getStartDate()
                .datesUntil(request.getEndDate().plusDays(1))
                .sorted()
                .collect(Collectors.toList());

        for (LocalDate date : dates) {
            // Sort staff by current workload (ascending)
            List<Staff> staffByWorkload = request.getActiveStaff().stream()
                    .sorted(Comparator.comparingDouble(staff -> staffWorkloads.get(staff.getId())))
                    .collect(Collectors.toList());

            for (Staff staff : staffByWorkload) {
                String key = staff.getId() + "-" + date;
                List<Gene> candidates = geneSpace.get(key);

                if (candidates != null && !candidates.isEmpty()) {
                    // Select gene that balances workload
                    Gene selectedGene = selectWorkloadBalancingGene(candidates, staffWorkloads.get(staff.getId()));
                    selected.put(key, selectedGene);

                    // Update workload
                    staffWorkloads.put(staff.getId(),
                            staffWorkloads.get(staff.getId()) + selectedGene.getWorkingHours());
                }
            }
        }

        return new Chromosome(new ArrayList<>(selected.values()));
    }

    /**
     * Select gene that helps balance workload
     */
    private Gene selectWorkloadBalancingGene(List<Gene> candidates, double currentWorkload) {
        // If staff is already heavily loaded, prefer day off or shorter shifts
        if (currentWorkload > 30) {
            return candidates.stream()
                    .filter(Gene::isDayOff)
                    .findFirst()
                    .orElse(candidates.stream()
                            .min(Comparator.comparingDouble(Gene::getWorkingHours))
                            .orElse(candidates.get(0)));
        }

        // If staff is underloaded, prefer working genes
        return candidates.stream()
                .filter(g -> !g.isDayOff())
                .findFirst()
                .orElse(candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())));
    }

    /**
     * Population specialization types
     */
    public enum PopulationSpecialization {
        CONSTRAINT_FOCUSED,
        TASK_COVERAGE_FOCUSED,
        WORKLOAD_BALANCED,
        DIVERSE
    }
}