package com.rosteroptimization.service;

import com.rosteroptimization.dto.TaskDTO;
import com.rosteroptimization.entity.Task;
import com.rosteroptimization.entity.Department;
import com.rosteroptimization.mapper.TaskMapper;
import com.rosteroptimization.repository.TaskRepository;
import com.rosteroptimization.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskMapper taskMapper;

    /**
     * Create new task
     */
    public TaskDTO create(TaskDTO dto) {
        log.info("Creating new task: {}", dto.getName());

        // Validate task data
        validateTaskData(dto);

        // Check if task with same name already exists
        Optional<Task> existing = taskRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Task with name '" + dto.getName() + "' already exists");
        }

        Task entity = taskMapper.toEntity(dto);
        Task saved = taskRepository.save(entity);

        log.info("Task created with ID: {}", saved.getId());
        return taskMapper.toDto(saved);
    }

    /**
     * Update existing task
     */
    public TaskDTO update(Long id, TaskDTO dto) {
        log.info("Updating task with ID: {}", id);

        // Validate task data
        validateTaskData(dto);

        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));

        // Check if another task with same name exists
        Optional<Task> duplicateName = taskRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (duplicateName.isPresent() && !duplicateName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Task with name '" + dto.getName() + "' already exists");
        }

        taskMapper.updateEntityFromDto(dto, existing);
        Task updated = taskRepository.save(existing);

        log.info("Task updated: {}", updated.getName());
        return taskMapper.toDto(updated);
    }

    /**
     * Soft delete task
     */
    public void delete(Long id) {
        log.info("Soft deleting task with ID: {}", id);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));

        task.setActive(false);
        taskRepository.save(task);

        log.info("Task soft deleted: {}", task.getName());
    }

    /**
     * Find task by ID
     */
    @Transactional(readOnly = true)
    public TaskDTO findById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));

        return taskMapper.toDto(task);
    }

    /**
     * Find all active tasks
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findAll() {
        List<Task> tasks = taskRepository.findByActiveTrueOrderByStartTimeAsc();
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find all tasks ordered by priority
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findAllByPriority() {
        List<Task> tasks = taskRepository.findByActiveTrueOrderByPriorityAscStartTimeAsc();
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find all tasks (including inactive)
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findAllIncludingInactive() {
        List<Task> tasks = taskRepository.findAllByOrderByStartTimeAsc();
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Search tasks by name
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> searchByName(String name, boolean includeInactive) {
        List<Task> tasks = taskRepository.searchByNameContaining(name, !includeInactive);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find tasks by department
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findByDepartment(Long departmentId) {
        List<Task> tasks = taskRepository.findByDepartmentIdAndActiveTrue(departmentId);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find tasks by priority
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findByPriority(Integer priority) {
        List<Task> tasks = taskRepository.findByPriorityAndActiveTrueOrderByStartTimeAsc(priority);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find high priority tasks
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findHighPriorityTasks() {
        List<Task> tasks = taskRepository.findHighPriorityTasks();
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find tasks by date
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findByDate(LocalDate date) {
        List<Task> tasks = taskRepository.findByDate(date);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find tasks by date range
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findByDateRange(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        List<Task> tasks = taskRepository.findByDateRange(fromDate, toDate);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find tasks by datetime range
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findByDateTimeRange(LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        validateDateTimeRange(fromDateTime, toDateTime);
        List<Task> tasks = taskRepository.findByDateTimeRange(fromDateTime, toDateTime);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find overlapping tasks
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findOverlappingTasks(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        validateDateTimeRange(startDateTime, endDateTime);
        List<Task> tasks = taskRepository.findOverlappingTasks(startDateTime, endDateTime);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find tasks by required qualification
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findByRequiredQualification(Long qualificationId) {
        List<Task> tasks = taskRepository.findByRequiredQualificationId(qualificationId);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find tasks requiring all specified qualifications
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findByAllRequiredQualifications(List<Long> qualificationIds) {
        if (qualificationIds == null || qualificationIds.isEmpty()) {
            return List.of();
        }
        List<Task> tasks = taskRepository.findByAllRequiredQualifications(qualificationIds, qualificationIds.size());
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find tasks requiring any of the specified qualifications
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findByAnyRequiredQualification(List<Long> qualificationIds) {
        if (qualificationIds == null || qualificationIds.isEmpty()) {
            return List.of();
        }
        List<Task> tasks = taskRepository.findByAnyRequiredQualification(qualificationIds);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find tasks without required qualifications
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findTasksWithoutRequiredQualifications() {
        List<Task> tasks = taskRepository.findTasksWithoutRequiredQualifications();
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find upcoming tasks
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findUpcomingTasks() {
        List<Task> tasks = taskRepository.findUpcomingTasks();
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find ongoing tasks
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findOngoingTasks() {
        List<Task> tasks = taskRepository.findOngoingTasks();
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Find completed tasks
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findCompletedTasks() {
        List<Task> tasks = taskRepository.findCompletedTasks();
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Get active task count
     */
    @Transactional(readOnly = true)
    public long getActiveCount() {
        return taskRepository.countByActiveTrue();
    }

    /**
     * Get task count by department
     */
    @Transactional(readOnly = true)
    public long getCountByDepartment(Long departmentId) {
        return taskRepository.countByDepartmentIdAndActiveTrue(departmentId);
    }

    /**
     * Get task count by priority
     */
    @Transactional(readOnly = true)
    public long getCountByPriority(Integer priority) {
        return taskRepository.countByPriorityAndActiveTrue(priority);
    }

    /**
     * Find tasks by IDs (for bulk operations)
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findByIds(List<Long> ids) {
        List<Task> tasks = taskRepository.findByIdInAndActiveTrue(ids);
        return taskMapper.toDtoList(tasks);
    }

    /**
     * Validate task data
     */
    private void validateTaskData(TaskDTO dto) {
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }

        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }

        if (dto.getStartTime().equals(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time and end time cannot be the same");
        }

        if (dto.getPriority() == null || dto.getPriority() < 1 || dto.getPriority() > 10) {
            throw new IllegalArgumentException("Priority must be between 1 and 10");
        }

        // Validate department exists and is active
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + dto.getDepartmentId()));

            if (!department.getActive()) {
                throw new IllegalArgumentException("Cannot assign task to inactive department");
            }
        }

        log.debug("Task validation passed for: {}", dto.getName());
    }

    /**
     * Validate date range
     */
    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Both from date and to date are required");
        }

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        log.debug("Date range validation passed: {} to {}", fromDate, toDate);
    }

    /**
     * Validate datetime range
     */
    private void validateDateTimeRange(LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        if (fromDateTime == null || toDateTime == null) {
            throw new IllegalArgumentException("Both from datetime and to datetime are required");
        }

        if (fromDateTime.isAfter(toDateTime)) {
            throw new IllegalArgumentException("From datetime cannot be after to datetime");
        }

        log.debug("DateTime range validation passed: {} to {}", fromDateTime, toDateTime);
    }
}