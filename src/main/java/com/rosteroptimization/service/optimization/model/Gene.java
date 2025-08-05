package com.rosteroptimization.service.optimization.model;

import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Shift;
import com.rosteroptimization.entity.Task;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Represents a gene in genetic algorithm chromosome
 * Each gene encodes one assignment decision for a specific staff member on a specific day
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Gene {

    private Staff staff;        // Which staff member
    private LocalDate date;     // Which date
    private Shift shift;        // Which shift (null means day off)
    private Task task;          // Which task (null means no specific task, just shift coverage)

    // Gene state
    private boolean active;     // Whether this gene is active (expressed)

    /**
     * Create a gene for day off (no shift, no task)
     */
    public static Gene createDayOffGene(Staff staff, LocalDate date) {
        return new Gene(staff, date, null, null, true);
    }

    /**
     * Create a gene for shift only (no specific task)
     */
    public static Gene createShiftOnlyGene(Staff staff, LocalDate date, Shift shift) {
        return new Gene(staff, date, shift, null, true);
    }

    /**
     * Create a gene for shift with specific task
     */
    public static Gene createShiftWithTaskGene(Staff staff, LocalDate date, Shift shift, Task task) {
        return new Gene(staff, date, shift, task, true);
    }

    /**
     * Check if this gene represents a day off
     */
    public boolean isDayOff() {
        return shift == null;
    }

    /**
     * Check if this gene has a specific task assigned
     */
    public boolean hasTask() {
        return task != null;
    }

    /**
     * Check if this gene represents a working day
     */
    public boolean isWorkingDay() {
        return !isDayOff() && active;
    }

    /**
     * Convert gene to assignment (if active and not day off)
     */
    public Assignment toAssignment() {
        if (!active || isDayOff()) {
            return null;
        }
        return new Assignment(staff, shift, task, date);
    }

    /**
     * Get working hours for this gene
     */
    public double getWorkingHours() {
        if (!isWorkingDay()) {
            return 0.0;
        }

        int startHour = shift.getStartTime().getHour();
        int startMinute = shift.getStartTime().getMinute();
        int endHour = shift.getEndTime().getHour();
        int endMinute = shift.getEndTime().getMinute();

        // Handle overnight shifts
        if (endHour < startHour) {
            endHour += 24;
        }

        double startDecimal = startHour + (startMinute / 60.0);
        double endDecimal = endHour + (endMinute / 60.0);

        return endDecimal - startDecimal;
    }

    /**
     * Check if gene is compatible with staff qualifications
     */
    public boolean isCompatibleWithStaff() {
        if (!hasTask()) {
            return true; // No specific task requirements
        }

        // Check if staff has all required qualifications for the task
        return task.getRequiredQualifications().stream()
                .allMatch(required -> staff.getQualifications().contains(required));
    }

    /**
     * Check if gene is compatible with department
     */
    public boolean isDepartmentCompatible() {
        if (!hasTask()) {
            return true; // No specific task requirements
        }

        return staff.getDepartment().getId().equals(task.getDepartment().getId());
    }

    /**
     * Create a copy of this gene
     */
    public Gene copy() {
        return new Gene(staff, date, shift, task, active);
    }

    /**
     * Create a mutated version of this gene
     * Changes the shift assignment but keeps the same staff and date
     */
    public Gene mutate(Shift newShift, Task newTask) {
        return new Gene(staff, date, newShift, newTask, active);
    }

    /**
     * Get gene identifier for easy comparison
     */
    public String getGeneId() {
        return String.format("%d-%s", staff.getId(), date.toString());
    }

    /**
     * Check if this gene conflicts with another gene (same staff, same day, both active)
     */
    public boolean conflictsWith(Gene other) {
        if (!this.active || !other.active) {
            return false;
        }

        if (!this.staff.getId().equals(other.staff.getId())) {
            return false;
        }

        if (!this.date.equals(other.date)) {
            return false;
        }

        // Same staff, same day, both active - potential conflict
        // Check if both are working (not day off)
        return this.isWorkingDay() && other.isWorkingDay();
    }

    @Override
    public String toString() {
        if (isDayOff()) {
            return String.format("Gene{%s %s: DAY_OFF}",
                    staff.getName() + " " + staff.getSurname(), date);
        } else {
            return String.format("Gene{%s %s: %s%s}",
                    staff.getName() + " " + staff.getSurname(),
                    date,
                    shift.getName(),
                    hasTask() ? " + " + task.getName() : "");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Gene gene = (Gene) obj;
        return getGeneId().equals(gene.getGeneId());
    }

    @Override
    public int hashCode() {
        return getGeneId().hashCode();
    }
}