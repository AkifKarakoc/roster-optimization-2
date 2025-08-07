package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.TaskDTO;
import com.rosteroptimization.entity.Task;
import com.rosteroptimization.entity.Department;
import com.rosteroptimization.entity.Qualification;
import com.rosteroptimization.repository.DepartmentRepository;
import com.rosteroptimization.repository.QualificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final DepartmentRepository departmentRepository;
    private final QualificationRepository qualificationRepository;
    private final DepartmentMapper departmentMapper;
    private final QualificationMapper qualificationMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Convert Entity to DTO
     */
    public TaskDTO toDto(Task entity) {
        if (entity == null) {
            return null;
        }

        TaskDTO dto = new TaskDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setPriority(entity.getPriority());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());

        // Set foreign key references
        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartment(departmentMapper.toDto(entity.getDepartment()));
        }

        // Set required qualification references
        if (entity.getRequiredQualifications() != null) {
            dto.setRequiredQualificationIds(entity.getRequiredQualifications().stream()
                    .map(Qualification::getId)
                    .collect(Collectors.toSet()));
            dto.setRequiredQualifications(entity.getRequiredQualifications().stream()
                    .map(qualificationMapper::toDto)
                    .collect(Collectors.toSet()));
        }

        // Calculate computed fields
        calculateComputedFields(entity, dto);

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public Task toEntity(TaskDTO dto) {
        if (dto == null) {
            return null;
        }

        Task entity = new Task();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setPriority(dto.getPriority());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Set foreign key relationships
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + dto.getDepartmentId()));
            entity.setDepartment(department);
        }

        // Set required qualification relationships
        if (dto.getRequiredQualificationIds() != null && !dto.getRequiredQualificationIds().isEmpty()) {
            Set<Qualification> qualifications = new HashSet<>();
            for (Long qualificationId : dto.getRequiredQualificationIds()) {
                Qualification qualification = qualificationRepository.findById(qualificationId)
                        .orElseThrow(() -> new IllegalArgumentException("Qualification not found with ID: " + qualificationId));
                qualifications.add(qualification);
            }
            entity.setRequiredQualifications(qualifications);
        }

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(TaskDTO dto, Task entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setPriority(dto.getPriority());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Update foreign key relationships
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + dto.getDepartmentId()));
            entity.setDepartment(department);
        }

        // Update required qualification relationships
        if (dto.getRequiredQualificationIds() != null) {
            Set<Qualification> qualifications = new HashSet<>();
            for (Long qualificationId : dto.getRequiredQualificationIds()) {
                Qualification qualification = qualificationRepository.findById(qualificationId)
                        .orElseThrow(() -> new IllegalArgumentException("Qualification not found with ID: " + qualificationId));
                qualifications.add(qualification);
            }
            entity.setRequiredQualifications(qualifications);
        } else {
            entity.setRequiredQualifications(new HashSet<>());
        }
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<TaskDTO> toDtoList(List<Task> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculate computed fields for task
     */
    private void calculateComputedFields(Task entity, TaskDTO dto) {
        if (entity.getStartTime() == null || entity.getEndTime() == null) {
            return;
        }

        // Calculate duration
        Duration duration = Duration.between(entity.getStartTime(), entity.getEndTime());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        dto.setDurationHours(String.format("%d:%02d", hours, minutes));
        dto.setDurationMinutes((int) duration.toMinutes());

        // Priority level
        dto.setPriorityLevel(getPriorityLevel(entity.getPriority()));

        // Task status
        dto.setStatus(getTaskStatus(entity.getStartTime(), entity.getEndTime()));

        // Required qualification count
        if (entity.getRequiredQualifications() != null) {
            dto.setRequiredQualificationCount(entity.getRequiredQualifications().size());
        } else {
            dto.setRequiredQualificationCount(0);
        }

        // Check if crosses midnight
        dto.setCrossesMidnight(crossesMidnight(entity.getStartTime(), entity.getEndTime()));

        // Display formats
        dto.setDateDisplay(entity.getStartTime().format(DATE_FORMATTER));
        dto.setTimeDisplay(entity.getStartTime().format(TIME_FORMATTER) + " - " + entity.getEndTime().format(TIME_FORMATTER));
    }

    /**
     * Get priority level string
     */
    private String getPriorityLevel(Integer priority) {
        if (priority == null) return "UNKNOWN";
        if (priority <= 2) return "HIGH";
        if (priority <= 5) return "MEDIUM";
        return "LOW";
    }

    /**
     * Get task status based on current time
     */
    private String getTaskStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startTime)) {
            return "UPCOMING";
        } else if (now.isAfter(endTime)) {
            return "COMPLETED";
        } else {
            return "ONGOING";
        }
    }

    /**
     * Check if task crosses midnight
     */
    private Boolean crossesMidnight(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return !startTime.toLocalDate().equals(endTime.toLocalDate());
    }
}