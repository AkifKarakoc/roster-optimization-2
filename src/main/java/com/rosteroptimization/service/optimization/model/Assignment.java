package com.rosteroptimization.service.optimization.model;

import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Shift;
import com.rosteroptimization.entity.Task;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Represents a single assignment in the roster plan
 * Links a staff member to a shift and optionally to a task on a specific date
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {

    private Staff staff;
    private Shift shift;
    private Task task; // Optional - staff might be assigned to shift without specific task
    private LocalDate date;

    /**
     * Check if this assignment has a specific task
     */
    public boolean hasTask() {
        return task != null;
    }

    /**
     * Get assignment duration in hours
     */
    public double getDurationHours() {
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
     * Check if this assignment overlaps with another assignment
     */
    public boolean overlapsWith(Assignment other) {
        if (!this.date.equals(other.date)) {
            return false;
        }

        if (this.shift == null || other.shift == null) {
            return false;
        }

        // Check time overlap
        boolean startBefore = this.shift.getStartTime().isBefore(other.shift.getEndTime());
        boolean endAfter = this.shift.getEndTime().isAfter(other.shift.getStartTime());

        return startBefore && endAfter;
    }

    @Override
    public String toString() {
        return String.format("Assignment{staff=%s, shift=%s, task=%s, date=%s}",
                staff != null ? staff.getName() + " " + staff.getSurname() : "null",
                shift != null ? shift.getName() : "null",
                task != null ? task.getName() : "none",
                date);
    }
}