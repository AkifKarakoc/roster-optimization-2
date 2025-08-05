package com.rosteroptimization.service.optimization.model;

import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Task;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a chromosome in genetic algorithm
 * Contains all genes (assignment decisions) for a complete roster plan
 */
@Data
@NoArgsConstructor
public class Chromosome {

    private List<Gene> genes = new ArrayList<>();
    private double fitnessScore = 0.0;
    private boolean fitnessCalculated = false;

    // Cached values for performance
    private List<Assignment> cachedAssignments;
    private Map<Staff, List<Gene>> cachedGenesByStaff;
    private Map<LocalDate, List<Gene>> cachedGenesByDate;

    public Chromosome(List<Gene> genes) {
        this.genes = new ArrayList<>(genes);
        invalidateCache();
    }

    /**
     * Add a gene to chromosome
     */
    public void addGene(Gene gene) {
        genes.add(gene);
        invalidateCache();
    }

    /**
     * Remove a gene from chromosome
     */
    public void removeGene(Gene gene) {
        genes.remove(gene);
        invalidateCache();
    }

    /**
     * Get all active genes
     */
    public List<Gene> getActiveGenes() {
        return genes.stream()
                .filter(Gene::isActive)
                .toList();
    }

    /**
     * Get all working genes (active and not day off)
     */
    public List<Gene> getWorkingGenes() {
        return genes.stream()
                .filter(Gene::isWorkingDay)
                .toList();
    }

    /**
     * Convert chromosome to roster plan assignments
     */
    public List<Assignment> toAssignments() {
        if (cachedAssignments == null) {
            cachedAssignments = getWorkingGenes().stream()
                    .map(Gene::toAssignment)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return cachedAssignments;
    }

    /**
     * Get genes for specific staff member
     */
    public List<Gene> getGenesForStaff(Staff staff) {
        if (cachedGenesByStaff == null) {
            cachedGenesByStaff = genes.stream()
                    .collect(Collectors.groupingBy(Gene::getStaff));
        }
        return cachedGenesByStaff.getOrDefault(staff, List.of());
    }

    /**
     * Get genes for specific date
     */
    public List<Gene> getGenesForDate(LocalDate date) {
        if (cachedGenesByDate == null) {
            cachedGenesByDate = genes.stream()
                    .collect(Collectors.groupingBy(Gene::getDate));
        }
        return cachedGenesByDate.getOrDefault(date, List.of());
    }

    /**
     * Get gene for specific staff and date
     */
    public Optional<Gene> getGeneForStaffAndDate(Staff staff, LocalDate date) {
        return genes.stream()
                .filter(gene -> gene.getStaff().getId().equals(staff.getId())
                        && gene.getDate().equals(date))
                .findFirst();
    }

    /**
     * Replace gene at specific position
     */
    public void replaceGene(int index, Gene newGene) {
        if (index >= 0 && index < genes.size()) {
            genes.set(index, newGene);
            invalidateCache();
        }
    }

    /**
     * Mutate a random gene
     */
    public void mutateRandomGene(Random random, List<Gene> possibleGenes) {
        if (genes.isEmpty() || possibleGenes.isEmpty()) return;

        int randomIndex = random.nextInt(genes.size());
        Gene originalGene = genes.get(randomIndex);

        // Find a compatible replacement from possible genes
        List<Gene> compatibleGenes = possibleGenes.stream()
                .filter(gene -> gene.getStaff().getId().equals(originalGene.getStaff().getId())
                        && gene.getDate().equals(originalGene.getDate()))
                .toList();

        if (!compatibleGenes.isEmpty()) {
            Gene newGene = compatibleGenes.get(random.nextInt(compatibleGenes.size()));
            replaceGene(randomIndex, newGene);
        }
    }

    /**
     * Get total working hours for specific staff
     */
    public double getTotalWorkingHours(Staff staff) {
        return getGenesForStaff(staff).stream()
                .mapToDouble(Gene::getWorkingHours)
                .sum();
    }

    /**
     * Get total working hours across all staff
     */
    public double getTotalWorkingHours() {
        return getWorkingGenes().stream()
                .mapToDouble(Gene::getWorkingHours)
                .sum();
    }

    /**
     * Get unassigned tasks (tasks not covered by any gene)
     */
    public List<Task> getUnassignedTasks(List<Task> allTasks) {
        Set<Task> assignedTasks = getWorkingGenes().stream()
                .filter(Gene::hasTask)
                .map(Gene::getTask)
                .collect(Collectors.toSet());

        return allTasks.stream()
                .filter(task -> !assignedTasks.contains(task))
                .toList();
    }

    /**
     * Check if chromosome has conflicts (multiple assignments for same staff on same day)
     */
    public boolean hasConflicts() {
        Set<String> assignmentKeys = new HashSet<>();

        for (Gene gene : getWorkingGenes()) {
            String key = gene.getStaff().getId() + "-" + gene.getDate();
            if (assignmentKeys.contains(key)) {
                return true; // Conflict found
            }
            assignmentKeys.add(key);
        }

        return false;
    }

    /**
     * Get working days count for specific staff
     */
    public int getWorkingDaysCount(Staff staff) {
        return (int) getGenesForStaff(staff).stream()
                .filter(Gene::isWorkingDay)
                .count();
    }

    /**
     * Get day off count for specific staff
     */
    public int getDayOffCount(Staff staff) {
        return (int) getGenesForStaff(staff).stream()
                .filter(Gene::isDayOff)
                .count();
    }

    /**
     * Create a deep copy of this chromosome
     */
    public Chromosome copy() {
        List<Gene> copiedGenes = genes.stream()
                .map(Gene::copy)
                .toList();

        Chromosome copy = new Chromosome(copiedGenes);
        copy.fitnessScore = this.fitnessScore;
        copy.fitnessCalculated = this.fitnessCalculated;

        return copy;
    }

    /**
     * Crossover with another chromosome to create offspring
     */
    public Chromosome crossover(Chromosome other, Random random) {
        List<Gene> offspringGenes = new ArrayList<>();

        // Ensure both chromosomes have the same gene structure
        Map<String, Gene> thisGenesMap = genes.stream()
                .collect(Collectors.toMap(Gene::getGeneId, gene -> gene));
        Map<String, Gene> otherGenesMap = other.genes.stream()
                .collect(Collectors.toMap(Gene::getGeneId, gene -> gene));

        // Uniform crossover - randomly choose genes from either parent
        for (String geneId : thisGenesMap.keySet()) {
            Gene selectedGene;
            if (otherGenesMap.containsKey(geneId)) {
                // Both parents have this gene, randomly select one
                selectedGene = random.nextBoolean() ?
                        thisGenesMap.get(geneId).copy() : otherGenesMap.get(geneId).copy();
            } else {
                // Only this parent has this gene
                selectedGene = thisGenesMap.get(geneId).copy();
            }
            offspringGenes.add(selectedGene);
        }

        // Add genes that only exist in the other parent
        for (String geneId : otherGenesMap.keySet()) {
            if (!thisGenesMap.containsKey(geneId)) {
                offspringGenes.add(otherGenesMap.get(geneId).copy());
            }
        }

        return new Chromosome(offspringGenes);
    }

    /**
     * Calculate chromosome size
     */
    public int size() {
        return genes.size();
    }

    /**
     * Get gene at specific index
     */
    public Gene getGene(int index) {
        return genes.get(index);
    }

    /**
     * Set fitness score and mark as calculated
     */
    public void setFitnessScore(double score) {
        this.fitnessScore = score;
        this.fitnessCalculated = true;
    }

    /**
     * Reset fitness calculation flag
     */
    public void resetFitness() {
        this.fitnessCalculated = false;
        this.fitnessScore = 0.0;
    }

    /**
     * Invalidate cached values when chromosome is modified
     */
    private void invalidateCache() {
        cachedAssignments = null;
        cachedGenesByStaff = null;
        cachedGenesByDate = null;
        resetFitness();
    }

    /**
     * Validate chromosome structure and constraints
     */
    public List<String> validate() {
        List<String> violations = new ArrayList<>();

        // Check for conflicts
        if (hasConflicts()) {
            violations.add("Chromosome has conflicting assignments (multiple shifts for same staff on same day)");
        }

        // Check for incompatible qualifications
        for (Gene gene : getWorkingGenes()) {
            if (!gene.isCompatibleWithStaff()) {
                violations.add(String.format("Gene %s: Staff lacks required qualifications", gene));
            }

            if (!gene.isDepartmentCompatible()) {
                violations.add(String.format("Gene %s: Staff and task department mismatch", gene));
            }
        }

        return violations;
    }

    /**
     * Get chromosome statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalGenes", genes.size());
        stats.put("activeGenes", (int) genes.stream().filter(Gene::isActive).count());
        stats.put("workingGenes", getWorkingGenes().size());
        stats.put("dayOffGenes", (int) genes.stream().filter(Gene::isDayOff).count());
        stats.put("totalWorkingHours", getTotalWorkingHours());
        stats.put("hasConflicts", hasConflicts());
        stats.put("fitnessScore", fitnessScore);
        stats.put("fitnessCalculated", fitnessCalculated);

        // Staff utilization
        long staffWithAssignments = genes.stream()
                .filter(Gene::isWorkingDay)
                .map(Gene::getStaff)
                .distinct()
                .count();
        stats.put("staffWithAssignments", staffWithAssignments);

        // Date coverage
        long datesWithAssignments = genes.stream()
                .filter(Gene::isWorkingDay)
                .map(Gene::getDate)
                .distinct()
                .count();
        stats.put("datesWithAssignments", datesWithAssignments);

        return stats;
    }

    @Override
    public String toString() {
        return String.format("Chromosome{genes=%d, workingGenes=%d, fitness=%.2f, calculated=%s}",
                genes.size(), getWorkingGenes().size(), fitnessScore, fitnessCalculated);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Chromosome that = (Chromosome) obj;
        return Objects.equals(genes, that.genes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genes);
    }
}