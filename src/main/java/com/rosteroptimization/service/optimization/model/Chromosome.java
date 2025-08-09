package com.rosteroptimization.service.optimization.model;

import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Task;
import com.rosteroptimization.entity.Shift;
import com.rosteroptimization.service.optimization.model.Assignment;
import com.rosteroptimization.service.optimization.model.OptimizationRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@Slf4j
public class Chromosome {

    private List<Gene> genes = new ArrayList<>();
    private double fitnessScore = 0.0;
    private boolean fitnessCalculated = false;

    private transient Map<Staff, List<Gene>> genesByStaff;
    private transient Map<LocalDate, List<Gene>> genesByDate;
    private transient String signatureCache;

    public Chromosome(List<Gene> genes) {
        this.genes = new ArrayList<>(genes);
        repairBasic();
        invalidateCache();
    }

    // === FACTORY METHODS ===

    public static Chromosome createRandom(Map<String, List<Gene>> groupedGenes, OptimizationRequest request) {
        Map<String, Gene> selected = new HashMap<>();
        Map<Long, WorkloadTracker> trackers = initializeTrackers(request.getActiveStaff());

        List<LocalDate> dates = request.getStartDate()
                .datesUntil(request.getEndDate().plusDays(1))
                .sorted()
                .collect(Collectors.toList());

        for (LocalDate date : dates) {
            for (Staff staff : request.getActiveStaff()) {
                String key = staff.getId() + "-" + date;
                List<Gene> candidates = groupedGenes.get(key);

                if (candidates != null && !candidates.isEmpty()) {
                    Gene gene = selectSmartRandom(candidates, trackers.get(staff.getId()));
                    selected.put(key, gene);
                    trackers.get(staff.getId()).addGene(gene);
                }
            }
        }

        return new Chromosome(new ArrayList<>(selected.values()));
    }

    public static Chromosome createConstraintAware(Map<String, List<Gene>> groupedGenes, OptimizationRequest request) {
        Map<String, Gene> selected = new HashMap<>();
        Map<Long, WorkloadTracker> trackers = initializeTrackers(request.getActiveStaff());

        List<LocalDate> dates = request.getStartDate()
                .datesUntil(request.getEndDate().plusDays(1))
                .sorted()
                .collect(Collectors.toList());

        // First pass: Assign high-priority tasks
        for (LocalDate date : dates) {
            List<Task> dayTasks = request.getActiveTasks().stream()
                    .filter(task -> task.getStartTime().toLocalDate().equals(date))
                    .filter(task -> task.getPriority() <= 2)
                    .sorted(Comparator.comparingInt(Task::getPriority))
                    .collect(Collectors.toList());

            Set<Long> assignedStaffIds = new HashSet<>();

            for (Task task : dayTasks) {
                Optional<Staff> bestStaff = findBestStaffForTask(task, request.getActiveStaff(), trackers, assignedStaffIds);

                if (bestStaff.isPresent()) {
                    Staff staff = bestStaff.get();
                    String key = staff.getId() + "-" + date;

                    Optional<Gene> taskGene = groupedGenes.getOrDefault(key, Collections.emptyList())
                            .stream()
                            .filter(g -> g.hasTasks() && g.getTasks().contains(task))
                            .filter(g -> trackers.get(staff.getId()).canAccommodate(g))
                            .findFirst();

                    if (taskGene.isPresent()) {
                        selected.put(key, taskGene.get());
                        trackers.get(staff.getId()).addGene(taskGene.get());
                        assignedStaffIds.add(staff.getId());
                    }
                }
            }
        }

        // Second pass: Fill remaining slots
        for (String key : groupedGenes.keySet()) {
            if (!selected.containsKey(key)) {
                List<Gene> candidates = groupedGenes.get(key);
                Long staffId = extractStaffId(key);

                if (candidates != null && !candidates.isEmpty() && staffId != null) {
                    WorkloadTracker tracker = trackers.get(staffId);
                    Gene gene = selectConstraintCompliant(candidates, tracker);
                    selected.put(key, gene);
                    tracker.addGene(gene);
                }
            }
        }

        return new Chromosome(new ArrayList<>(selected.values()));
    }

    // === GENETIC OPERATORS ===

    public Chromosome crossover(Chromosome other) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double fitnessDiff = Math.abs(this.fitnessScore - other.fitnessScore);
        boolean useFitnessWeighted = fitnessDiff > 100;

        Map<String, Gene> thisGenes = this.genes.stream()
                .collect(Collectors.toMap(Gene::getGeneId, g -> g));
        Map<String, Gene> otherGenes = other.genes.stream()
                .collect(Collectors.toMap(Gene::getGeneId, g -> g));

        Set<String> allKeys = new HashSet<>(thisGenes.keySet());
        allKeys.addAll(otherGenes.keySet());

        List<Gene> offspringGenes = new ArrayList<>();
        Set<Task> assignedTasks = new HashSet<>();

        for (String geneId : allKeys) {
            Gene gene1 = thisGenes.get(geneId);
            Gene gene2 = otherGenes.get(geneId);

            Gene selected = selectBetterGene(gene1, gene2, assignedTasks, useFitnessWeighted, random);
            if (selected != null) {
                offspringGenes.add(selected.copy());
                if (selected.hasTasks()) {
                    assignedTasks.addAll(selected.getTasks());
                }
            }
        }

        return new Chromosome(offspringGenes);
    }

    public void mutate(Map<String, List<Gene>> groupedGenes, double mutationRate) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (genes.isEmpty() || random.nextDouble() > mutationRate) return;

        int targetIndex = selectMutationTarget(random);
        Gene oldGene = genes.get(targetIndex);
        String key = oldGene.getGeneId();

        List<Gene> alternatives = groupedGenes.getOrDefault(key, Collections.emptyList());
        if (alternatives.size() <= 1) return;

        List<Gene> validAlternatives = alternatives.stream()
                .filter(g -> !g.equals(oldGene))
                .collect(Collectors.toList());

        if (validAlternatives.isEmpty()) return;

        Gene newGene = selectBetterMutation(oldGene, validAlternatives, random);
        genes.set(targetIndex, newGene);
        invalidateCache();
    }

    public boolean balanceWorkloads() {
        Map<Staff, Double> workloadMap = new HashMap<>();
        Map<Staff, List<Gene>> staffGeneMap = new HashMap<>();

        for (Gene gene : genes) {
            Staff staff = gene.getStaff();
            double hours = gene.getWorkingHours();
            workloadMap.merge(staff, hours, Double::sum);
            staffGeneMap.computeIfAbsent(staff, k -> new ArrayList<>()).add(gene);
        }

        double avgWorkload = workloadMap.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        List<Staff> overworked = workloadMap.entrySet().stream()
                .filter(e -> e.getValue() > avgWorkload + 5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Staff> underutilized = workloadMap.entrySet().stream()
                .filter(e -> e.getValue() < avgWorkload - 5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (overworked.isEmpty() || underutilized.isEmpty()) return false;

        for (Staff over : overworked) {
            for (Staff under : underutilized) {
                if (tryWorkloadSwap(over, under, staffGeneMap)) {
                    invalidateCache();
                    return true;
                }
            }
        }

        return false;
    }

    // === REPAIR METHODS ===

    public void repairBasic() {
        Map<String, Gene> uniqueGenes = new HashMap<>();
        List<Gene> duplicates = new ArrayList<>();

        for (Gene gene : genes) {
            String key = gene.getGeneId();
            if (uniqueGenes.containsKey(key)) {
                Gene existing = uniqueGenes.get(key);
                if (getGenePriority(gene) > getGenePriority(existing)) {
                    duplicates.add(existing);
                    uniqueGenes.put(key, gene);
                } else {
                    duplicates.add(gene);
                }
            } else {
                uniqueGenes.put(key, gene);
            }
        }

        if (!duplicates.isEmpty()) {
            genes.removeAll(duplicates);
            invalidateCache();
        }
    }

    public void repairAdvanced() {
        repairBasic();

        Map<Long, List<Gene>> staffGenes = genes.stream()
                .filter(g -> !g.isDayOff())
                .collect(Collectors.groupingBy(g -> g.getStaff().getId()));

        List<Gene> toModify = new ArrayList<>();

        for (Map.Entry<Long, List<Gene>> entry : staffGenes.entrySet()) {
            List<Gene> workingGenes = entry.getValue();

            Map<LocalDate, Double> dailyHours = workingGenes.stream()
                    .collect(Collectors.groupingBy(Gene::getDate,
                            Collectors.summingDouble(Gene::getWorkingHours)));

            dailyHours.entrySet().stream()
                    .filter(e -> e.getValue() > 12)
                    .forEach(e -> {
                        workingGenes.stream()
                                .filter(g -> g.getDate().equals(e.getKey()))
                                .filter(g -> !g.hasTasks())
                                .findFirst()
                                .ifPresent(toModify::add);
                    });
        }

        for (Gene gene : toModify) {
            int index = genes.indexOf(gene);
            if (index >= 0) {
                Gene dayOff = Gene.createDayOffGene(gene.getStaff(), gene.getDate());
                genes.set(index, dayOff);
            }
        }

        if (!toModify.isEmpty()) {
            invalidateCache();
        }
    }

    public boolean hasBasicViolations() {
        Map<String, Gene> geneMap = new HashMap<>();
        for (Gene gene : genes) {
            String key = gene.getGeneId();
            if (geneMap.containsKey(key)) {
                return true; // Duplicate found
            }
            geneMap.put(key, gene);
        }
        return false;
    }

    public boolean isFeasible(OptimizationRequest request) {
        // Basic feasibility check - no duplicate assignments
        return !hasBasicViolations() && getUnassignedTasks(request.getActiveTasks()).size() < request.getActiveTasks().size() * 0.5;
    }

    // === CORE FUNCTIONALITY ===

    public List<Assignment> toAssignments() {
        return genes.stream()
                .filter(Gene::isWorkingDay)
                .map(Gene::toAssignment)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Task> getUnassignedTasks(List<Task> allTasks) {
        Set<Task> assignedTasks = genes.stream()
                .filter(Gene::hasTasks)
                .flatMap(g -> g.getTasks().stream())
                .collect(Collectors.toSet());

        return allTasks.stream()
                .filter(task -> !assignedTasks.contains(task))
                .collect(Collectors.toList());
    }

    public List<Gene> getGenesForStaff(Staff staff) {
        if (genesByStaff == null) {
            genesByStaff = genes.stream()
                    .collect(Collectors.groupingBy(Gene::getStaff));
        }
        return genesByStaff.getOrDefault(staff, Collections.emptyList());
    }

    public List<Gene> getGenesForDate(LocalDate date) {
        if (genesByDate == null) {
            genesByDate = genes.stream()
                    .collect(Collectors.groupingBy(Gene::getDate));
        }
        return genesByDate.getOrDefault(date, Collections.emptyList());
    }

    public double getTotalWorkingHours(Staff staff) {
        return getGenesForStaff(staff).stream()
                .mapToDouble(Gene::getWorkingHours)
                .sum();
    }

    public int getWorkingDaysCount(Staff staff) {
        return (int) getGenesForStaff(staff).stream()
                .filter(Gene::isWorkingDay)
                .count();
    }

    public double getWorkloadImbalanceScore() {
        Map<Staff, Double> workloads = new HashMap<>();
        for (Gene gene : genes) {
            workloads.merge(gene.getStaff(), gene.getWorkingHours(), Double::sum);
        }

        if (workloads.size() <= 1) return 0;

        double mean = workloads.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = workloads.values().stream()
                .mapToDouble(w -> Math.pow(w - mean, 2))
                .average().orElse(0);

        return Math.sqrt(variance);
    }

    public String getSignature() {
        if (signatureCache == null && !genes.isEmpty()) {
            signatureCache = genes.stream()
                    .map(gene -> String.format("%d-%s-%s-%s",
                            gene.getStaff().getId(),
                            gene.getDate(),
                            gene.getShift() != null ? gene.getShift().getId() : "0",
                            gene.hasTasks() ? gene.getTasks().stream()
                                    .map(t -> t.getId().toString())
                                    .collect(Collectors.joining(",")) : "0"))
                    .sorted()
                    .collect(Collectors.joining("|"));
        }
        return signatureCache != null ? signatureCache : "EMPTY";
    }

    public Chromosome copy() {
        List<Gene> copiedGenes = genes.stream()
                .map(Gene::copy)
                .collect(Collectors.toList());

        Chromosome copy = new Chromosome();
        copy.genes = copiedGenes;
        copy.fitnessScore = this.fitnessScore;
        copy.fitnessCalculated = this.fitnessCalculated;
        return copy;
    }

    // === UTILITY METHODS ===

    private static Map<Long, WorkloadTracker> initializeTrackers(List<Staff> staff) {
        return staff.stream()
                .collect(Collectors.toMap(Staff::getId, s -> new WorkloadTracker()));
    }

    private static Gene selectSmartRandom(List<Gene> candidates, WorkloadTracker tracker) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (tracker.shouldRest()) {
            Optional<Gene> dayOff = candidates.stream()
                    .filter(Gene::isDayOff)
                    .findFirst();
            if (dayOff.isPresent()) return dayOff.get();
        }

        List<Gene> weighted = new ArrayList<>();
        for (Gene gene : candidates) {
            int weight = getGeneWeight(gene, tracker);
            for (int i = 0; i < weight; i++) {
                weighted.add(gene);
            }
        }

        return weighted.get(random.nextInt(weighted.size()));
    }

    private static Gene selectConstraintCompliant(List<Gene> candidates, WorkloadTracker tracker) {
        if (tracker.mustRest()) {
            return candidates.stream()
                    .filter(Gene::isDayOff)
                    .findFirst()
                    .orElse(candidates.get(0));
        }

        List<Gene> taskGenes = candidates.stream()
                .filter(g -> g.hasTasks() && tracker.canAccommodate(g))
                .collect(Collectors.toList());

        if (!taskGenes.isEmpty()) {
            return taskGenes.get(ThreadLocalRandom.current().nextInt(taskGenes.size()));
        }

        return candidates.stream()
                .filter(g -> tracker.canAccommodate(g))
                .findFirst()
                .orElse(candidates.stream().filter(Gene::isDayOff).findFirst().orElse(candidates.get(0)));
    }

    private static Optional<Staff> findBestStaffForTask(Task task, List<Staff> availableStaff,
                                                        Map<Long, WorkloadTracker> trackers,
                                                        Set<Long> assignedStaffIds) {
        return availableStaff.stream()
                .filter(staff -> !assignedStaffIds.contains(staff.getId()))
                .filter(staff -> staff.getDepartment().getId().equals(task.getDepartment().getId()))
                .filter(staff -> task.getRequiredQualifications().stream()
                        .allMatch(req -> staff.getQualifications().contains(req)))
                .filter(staff -> trackers.get(staff.getId()).canAccommodateTask())
                .min(Comparator.comparingDouble(staff -> trackers.get(staff.getId()).getWorkload()));
    }

    private Gene selectBetterGene(Gene gene1, Gene gene2, Set<Task> assignedTasks,
                                  boolean useFitnessWeighted, ThreadLocalRandom random) {
        if (gene1 == null) return gene2;
        if (gene2 == null) return gene1;

        if (gene1.hasTasks() && gene1.getTasks().stream().anyMatch(assignedTasks::contains)) {
            return gene2.hasTasks() && gene2.getTasks().stream().anyMatch(assignedTasks::contains) ?
                    (gene1.isDayOff() ? gene1 : gene2.isDayOff() ? gene2 : null) : gene2;
        }
        if (gene2.hasTasks() && gene2.getTasks().stream().anyMatch(assignedTasks::contains)) {
            return gene1;
        }

        if (gene1.hasTasks() && !gene2.hasTasks()) return gene1;
        if (gene2.hasTasks() && !gene1.hasTasks()) return gene2;

        return random.nextBoolean() ? gene1 : gene2;
    }

    private int selectMutationTarget(ThreadLocalRandom random) {
        List<Integer> nonTaskIndices = new ArrayList<>();
        for (int i = 0; i < genes.size(); i++) {
            if (!genes.get(i).hasTasks()) {
                nonTaskIndices.add(i);
            }
        }

        if (!nonTaskIndices.isEmpty() && random.nextDouble() < 0.7) {
            return nonTaskIndices.get(random.nextInt(nonTaskIndices.size()));
        }

        return random.nextInt(genes.size());
    }

    private Gene selectBetterMutation(Gene oldGene, List<Gene> alternatives, ThreadLocalRandom random) {
        if (!oldGene.isDayOff() && oldGene.getWorkingHours() > 10) {
            Optional<Gene> dayOff = alternatives.stream()
                    .filter(Gene::isDayOff)
                    .findFirst();
            if (dayOff.isPresent() && random.nextDouble() < 0.4) {
                return dayOff.get();
            }
        }

        if (!oldGene.isDayOff() && oldGene.getWorkingHours() > 8) {
            Optional<Gene> shorter = alternatives.stream()
                    .filter(g -> !g.isDayOff() && g.getWorkingHours() < oldGene.getWorkingHours())
                    .min(Comparator.comparingDouble(Gene::getWorkingHours));
            if (shorter.isPresent()) {
                return shorter.get();
            }
        }

        return alternatives.get(random.nextInt(alternatives.size()));
    }

    private int getGenePriority(Gene gene) {
        if (gene.isDayOff()) return 0;
        if (gene.hasTasks()) return 2;
        return 1;
    }

    private static int getGeneWeight(Gene gene, WorkloadTracker tracker) {
        if (gene.isDayOff()) {
            return tracker.shouldRest() ? 4 : 2;
        } else if (gene.hasTasks()) {
            return tracker.canAccommodateTask() ? 3 : 1;
        } else {
            return tracker.canAccommodate(gene) ? 2 : 1;
        }
    }

    private static Long extractStaffId(String geneId) {
        try {
            String[] parts = geneId.split("-");
            return Long.parseLong(parts[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean tryWorkloadSwap(Staff overworked, Staff underutilized,
                                    Map<Staff, List<Gene>> staffGeneMap) {
        if (!overworked.getDepartment().getId().equals(underutilized.getDepartment().getId())) {
            return false;
        }

        List<Gene> overGenes = staffGeneMap.get(overworked);
        List<Gene> underGenes = staffGeneMap.get(underutilized);

        for (Gene overGene : overGenes) {
            if (overGene.isDayOff() || !overGene.hasTasks()) continue;

            Optional<Gene> swappableUnder = underGenes.stream()
                    .filter(g -> g.getDate().equals(overGene.getDate()))
                    .filter(g -> g.isDayOff() || (!g.hasTasks() && isShiftCompatible(g, overGene)))
                    .filter(g -> areTasksCompatible(overGene.getTasks(), underutilized))
                    .findFirst();

            if (swappableUnder.isPresent()) {
                Gene underGene = swappableUnder.get();

                Gene newOverGene = underGene.isDayOff() ?
                        Gene.createDayOffGene(overworked, underGene.getDate()) :
                        Gene.createShiftOnlyGene(overworked, underGene.getDate(), underGene.getShift());

                Gene newUnderGene = Gene.createShiftWithMultipleTasksGene(
                        underutilized, overGene.getDate(), overGene.getShift(), overGene.getTasks());

                int overIndex = genes.indexOf(overGene);
                int underIndex = genes.indexOf(underGene);

                genes.set(overIndex, newOverGene);
                genes.set(underIndex, newUnderGene);

                return true;
            }
        }

        return false;
    }

    private boolean isShiftCompatible(Gene g1, Gene g2) {
        if (g1.getShift() == null || g2.getShift() == null) return false;
        return Math.abs(g1.getWorkingHours() - g2.getWorkingHours()) <= 2;
    }

    private boolean areTasksCompatible(List<Task> tasks, Staff staff) {
        return tasks.stream().allMatch(task ->
                task.getRequiredQualifications().stream()
                        .allMatch(req -> staff.getQualifications().contains(req)));
    }

    private void invalidateCache() {
        genesByStaff = null;
        genesByDate = null;
        signatureCache = null;
        fitnessCalculated = false;
    }

    public void setFitnessScore(double score) {
        this.fitnessScore = score;
        this.fitnessCalculated = true;
    }

    // === HELPER CLASSES ===

    private static class WorkloadTracker {
        private double weeklyHours = 0;
        private int consecutiveDays = 0;
        private int weeklyDays = 0;

        public void addGene(Gene gene) {
            if (!gene.isDayOff()) {
                weeklyHours += gene.getWorkingHours();
                consecutiveDays++;
                weeklyDays++;
            } else {
                consecutiveDays = 0;
            }
        }

        public boolean canAccommodate(Gene gene) {
            if (gene.isDayOff()) return true;
            return weeklyHours + gene.getWorkingHours() <= 40 &&
                    consecutiveDays < 5 &&
                    weeklyDays < 6;
        }

        public boolean canAccommodateTask() {
            return weeklyHours < 35 && consecutiveDays < 4 && weeklyDays < 5;
        }

        public boolean shouldRest() {
            return weeklyHours >= 32 || consecutiveDays >= 4 || weeklyDays >= 5;
        }

        public boolean mustRest() {
            return weeklyHours >= 38 || consecutiveDays >= 5 || weeklyDays >= 6;
        }

        public double getWorkload() {
            return weeklyHours;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chromosome that = (Chromosome) o;
        return Objects.equals(this.getSignature(), that.getSignature());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSignature());
    }

    @Override
    public String toString() {
        return String.format("Chromosome{genes=%d, fitness=%.2f, calculated=%s}",
                genes.size(), fitnessScore, fitnessCalculated);
    }
}