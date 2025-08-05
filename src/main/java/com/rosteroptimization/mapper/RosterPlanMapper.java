package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.RosterPlanDTO;
import com.rosteroptimization.service.optimization.model.Assignment;
import com.rosteroptimization.service.optimization.model.RosterPlan;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Shift;
import com.rosteroptimization.entity.Task;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting RosterPlan to RosterPlanDTO
 */
@Component
public class RosterPlanMapper {

    public RosterPlanDTO toDTO(RosterPlan rosterPlan) {
        if (rosterPlan == null) {
            return null;
        }

        return RosterPlanDTO.builder()
                .planId(rosterPlan.getPlanId())
                .generatedAt(rosterPlan.getGeneratedAt())
                .algorithmUsed(rosterPlan.getAlgorithmUsed())
                .startDate(rosterPlan.getStartDate())
                .endDate(rosterPlan.getEndDate())
                .assignments(mapAssignments(rosterPlan.getAssignments()))
                .fitnessScore(rosterPlan.getFitnessScore())
                .hardConstraintViolations(rosterPlan.getHardConstraintViolations())
                .softConstraintViolations(rosterPlan.getSoftConstraintViolations())
                .executionTimeMs(rosterPlan.getExecutionTimeMs())
                .feasible(rosterPlan.isFeasible())
                .unassignedTasks(mapTasks(rosterPlan.getUnassignedTasks()))
                .underutilizedStaff(mapStaff(rosterPlan.getUnderutilizedStaff()))
                .statistics(rosterPlan.getStatistics())
                .algorithmMetadata(rosterPlan.getAlgorithmMetadata())
                .totalAssignments(rosterPlan.getAssignments() != null ? rosterPlan.getAssignments().size() : 0)
                .uniqueStaffCount((int) (rosterPlan.getAssignments() != null ? 
                    rosterPlan.getAssignments().stream()
                        .map(assignment -> assignment.getStaff().getId())
                        .distinct()
                        .count() : 0))
                .taskCoverageRate(rosterPlan.getTaskCoverageRate())
                .staffUtilizationRate(rosterPlan.getStaffUtilizationRate())
                .build();
    }

    private List<RosterPlanDTO.AssignmentDTO> mapAssignments(List<Assignment> assignments) {
        if (assignments == null) {
            return null;
        }

        return assignments.stream()
                .map(this::mapAssignment)
                .collect(Collectors.toList());
    }

    private RosterPlanDTO.AssignmentDTO mapAssignment(Assignment assignment) {
        return new RosterPlanDTO.AssignmentDTO(
                mapStaff(assignment.getStaff()),
                mapShift(assignment.getShift()),
                mapTask(assignment.getTask()),
                assignment.getDate(),
                assignment.getDurationHours()
        );
    }

    private RosterPlanDTO.StaffDTO mapStaff(Staff staff) {
        if (staff == null) {
            return null;
        }

        return new RosterPlanDTO.StaffDTO(
                staff.getId(),
                staff.getName(),
                staff.getSurname(),
                staff.getRegistrationCode(),
                null // No title field in Staff entity
        );
    }

    private List<RosterPlanDTO.StaffDTO> mapStaff(List<Staff> staffList) {
        if (staffList == null) {
            return null;
        }

        return staffList.stream()
                .map(this::mapStaff)
                .collect(Collectors.toList());
    }

    private RosterPlanDTO.ShiftDTO mapShift(Shift shift) {
        if (shift == null) {
            return null;
        }

        return new RosterPlanDTO.ShiftDTO(
                shift.getId(),
                shift.getName(),
                shift.getStartTime() != null ? shift.getStartTime().toString() : null,
                shift.getEndTime() != null ? shift.getEndTime().toString() : null,
                shift.getIsNightShift() != null && shift.getIsNightShift() ? "NIGHT" : "DAY"
        );
    }

    private RosterPlanDTO.TaskDTO mapTask(Task task) {
        if (task == null) {
            return null;
        }

        return new RosterPlanDTO.TaskDTO(
                task.getId(),
                task.getName(),
                task.getDescription(),
                task.getStartTime(),
                task.getEndTime(),
                task.getPriority() != null ? task.getPriority().toString() : null,
                1 // Default required staff count since it's not in entity
        );
    }

    private List<RosterPlanDTO.TaskDTO> mapTasks(List<Task> tasks) {
        if (tasks == null) {
            return null;
        }

        return tasks.stream()
                .map(this::mapTask)
                .collect(Collectors.toList());
    }
}