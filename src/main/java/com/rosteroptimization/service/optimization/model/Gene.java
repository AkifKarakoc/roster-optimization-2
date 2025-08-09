package com.rosteroptimization.service.optimization.model;

import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Shift;
import com.rosteroptimization.entity.Task;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Gene class with multiple tasks support for virtual task splitting
 */
@Data
@NoArgsConstructor
public class Gene {

    private Staff staff;        // Which staff member
    private LocalDate date;     // Which date
    private Shift shift;        // Which shift (null means day off)
    private List<Task> tasks;   // Multiple tasks (null/empty means no specific tasks)

    // Gene state
    private boolean active;     // Whether this gene is active (expressed)

    public Gene(Staff staff, LocalDate date, Shift shift, List<Task> tasks, boolean active) {
        this.staff = staff;
        this.date = date;
        this.shift = shift;
        this.tasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        this.active = active;
    }

    /**
     * Create a gene for day off (no shift, no tasks)
     */
    public static Gene createDayOffGene(Staff staff, LocalDate date) {
        return new Gene(staff, date, null, new ArrayList<>(), true);
    }

    /**
     * Create a gene for shift only (no specific tasks)
     */
    public static Gene createShiftOnlyGene(Staff staff, LocalDate date, Shift shift) {
        return new Gene(staff, date, shift, new ArrayList<>(), true);
    }

    /**
     * Create a gene for shift with single task
     */
    public static Gene createShiftWithTaskGene(Staff staff, LocalDate date, Shift shift, Task task) {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task);
        return new Gene(staff, date, shift, tasks, true);
    }

    /**
     * Create a gene for shift with multiple tasks
     */
    public static Gene createShiftWithMultipleTasksGene(Staff staff, LocalDate date, Shift shift, List<Task> tasks) {
        return new Gene(staff, date, shift, tasks != null ? new ArrayList<>(tasks) : new ArrayList<>(), true);
    }

    /**
     * Check if this gene represents a day off
     */
    public boolean isDayOff() {
        return shift == null;
    }

    /**
     * Check if this gene has any tasks assigned
     */
    public boolean hasTasks() {
        return tasks != null && !tasks.isEmpty();
    }

    /**
     * Get single task (for backward compatibility)
     */
    public Task getTask() {
        return hasTasks() ? tasks.get(0) : null;
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

        // For backward compatibility, create assignment with first task
        // Multiple tasks will be handled in enhanced Assignment model
        Task primaryTask = hasTasks() ? tasks.get(0) : null;
        return new Assignment(staff, shift, primaryTask, date);
    }

    /**
     * Get total working hours for this gene
     */
    public double getWorkingHours() {
        if (!isWorkingDay()) {
            return 0.0;
        }

        if (shift == null) return 0.0;

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
     * Get total task hours (sum of all assigned tasks)
     */
    public double getTotalTaskHours() {
        if (!hasTasks()) return 0.0;

        return tasks.stream()
                .mapToDouble(task -> {
                    long minutes = java.time.Duration.between(
                            task.getStartTime(),
                            task.getEndTime()
                    ).toMinutes();
                    return minutes / 60.0;
                })
                .sum();
    }

    /**
     * Get shift utilization rate (task hours / shift hours)
     */
    public double getShiftUtilizationRate() {
        double shiftHours = getWorkingHours();
        if (shiftHours == 0) return 0.0;

        double taskHours = getTotalTaskHours();
        return Math.min(1.0, taskHours / shiftHours);
    }

    /**
     * Check if gene is compatible with staff qualifications
     */
    public boolean isCompatibleWithStaff() {
        if (!hasTasks()) {
            return true; // No specific task requirements
        }

        // Check if staff has all required qualifications for all tasks
        return tasks.stream().allMatch(task ->
                task.getRequiredQualifications().stream()
                        .allMatch(required -> staff.getQualifications().contains(required))
        );
    }

    /**
     * Check if gene is compatible with department
     */
    public boolean isDepartmentCompatible() {
        if (!hasTasks()) {
            return true; // No specific task requirements
        }

        return tasks.stream().allMatch(task ->
                staff.getDepartment().getId().equals(task.getDepartment().getId())
        );
    }

    /**
     * Check if shift can accommodate all assigned tasks
     */
    public boolean isShiftCapacityCompatible() {
        if (!hasTasks() || shift == null) return true;

        double shiftHours = getWorkingHours();
        double taskHours = getTotalTaskHours();

        // Allow small buffer (30 minutes)
        return taskHours <= shiftHours + 0.5;
    }

    /**
     * Add task to this gene (if shift has capacity)
     */
    public boolean addTask(Task task) {
        if (isDayOff() || task == null) return false;

        if (tasks == null) {
            tasks = new ArrayList<>();
        }

        // Check capacity before adding
        double currentTaskHours = getTotalTaskHours();
        long newTaskMinutes = java.time.Duration.between(
                task.getStartTime(),
                task.getEndTime()
        ).toMinutes();
        double newTaskHours = newTaskMinutes / 60.0;

        double shiftHours = getWorkingHours();
        if (currentTaskHours + newTaskHours <= shiftHours + 0.5) { // 30 min buffer
            tasks.add(task);
            return true;
        }

        return false;
    }

    /**
     * Remove task from this gene
     */
    public boolean removeTask(Task task) {
        if (tasks == null || task == null) return false;
        return tasks.remove(task);
    }

    /**
     * Create a copy of this gene
     */
    public Gene copy() {
        List<Task> copiedTasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        return new Gene(staff, date, shift, copiedTasks, active);
    }

    /**
     * Create a mutated version of this gene
     */
    public Gene mutate(Shift newShift, List<Task> newTasks) {
        List<Task> copiedTasks = newTasks != null ? new ArrayList<>(newTasks) : new ArrayList<>();
        return new Gene(staff, date, newShift, copiedTasks, active);
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

    /**
     * Check if any assigned tasks have time conflicts
     */
    public boolean hasTaskTimeConflicts() {
        if (!hasTasks() || tasks.size() <= 1) return false;

        for (int i = 0; i < tasks.size(); i++) {
            for (int j = i + 1; j < tasks.size(); j++) {
                Task task1 = tasks.get(i);
                Task task2 = tasks.get(j);

                // Check for time overlap
                if (task1.getStartTime().isBefore(task2.getEndTime()) &&
                        task2.getStartTime().isBefore(task1.getEndTime())) {
                    return true; // Overlap found
                }
            }
        }

        return false;
    }

    /**
     * Get tasks sorted by start time
     */
    public List<Task> getTasksSortedByTime() {
        if (!hasTasks()) return new ArrayList<>();

        return tasks.stream()
                .sorted((t1, t2) -> t1.getStartTime().compareTo(t2.getStartTime()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String toString() {
        if (isDayOff()) {
            return String.format("Gene{%s %s: DAY_OFF}",
                    staff.getName() + " " + staff.getSurname(), date);
        } else {
            String tasksStr = hasTasks() ?
                    String.format(" + %d tasks", tasks.size()) :
                    "";
            return String.format("Gene{%s %s: %s%s}",
                    staff.getName() + " " + staff.getSurname(),
                    date,
                    shift.getName(),
                    tasksStr);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Gene gene = (Gene) obj;
        return getGeneId().equals(gene.getGeneId()) &&
                java.util.Objects.equals(shift, gene.shift) &&
                java.util.Objects.equals(tasks, gene.tasks);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getGeneId(), shift, tasks);
    }
}