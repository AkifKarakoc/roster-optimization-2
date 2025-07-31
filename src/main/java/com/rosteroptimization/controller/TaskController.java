package com.rosteroptimization.controller;

import com.rosteroptimization.dto.TaskDTO;
import com.rosteroptimization.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaskController {

    private final TaskService taskService;

    /**
     * Create new task
     */
    @PostMapping
    public ResponseEntity<TaskDTO> create(@Valid @RequestBody TaskDTO dto) {
        log.info("POST /api/tasks - Creating task: {}", dto.getName());

        try {
            TaskDTO created = taskService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating task: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing task
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> update(@PathVariable Long id, @Valid @RequestBody TaskDTO dto) {
        log.info("PUT /api/tasks/{} - Updating task", id);

        try {
            TaskDTO updated = taskService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating task: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete task
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/tasks/{} - Deleting task", id);

        try {
            taskService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting task: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get task by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> findById(@PathVariable Long id) {
        log.info("GET /api/tasks/{} - Finding task by ID", id);

        try {
            TaskDTO task = taskService.findById(id);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            log.error("Task not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active tasks
     */
    @GetMapping
    public ResponseEntity<List<TaskDTO>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "false") boolean orderByPriority) {
        log.info("GET /api/tasks - Finding all tasks (includeInactive: {}, orderByPriority: {})", includeInactive, orderByPriority);

        List<TaskDTO> tasks;
        if (includeInactive) {
            tasks = taskService.findAllIncludingInactive();
        } else if (orderByPriority) {
            tasks = taskService.findAllByPriority();
        } else {
            tasks = taskService.findAll();
        }

        return ResponseEntity.ok(tasks);
    }

    /**
     * Search tasks by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<TaskDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/tasks/search - Searching tasks by name: {}", name);

        List<TaskDTO> tasks = taskService.searchByName(name, includeInactive);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find tasks by department
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<TaskDTO>> findByDepartment(@PathVariable Long departmentId) {
        log.info("GET /api/tasks/department/{} - Finding tasks by department", departmentId);

        List<TaskDTO> tasks = taskService.findByDepartment(departmentId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find tasks by priority
     */
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TaskDTO>> findByPriority(@PathVariable Integer priority) {
        log.info("GET /api/tasks/priority/{} - Finding tasks by priority", priority);

        List<TaskDTO> tasks = taskService.findByPriority(priority);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find high priority tasks
     */
    @GetMapping("/high-priority")
    public ResponseEntity<List<TaskDTO>> findHighPriorityTasks() {
        log.info("GET /api/tasks/high-priority - Finding high priority tasks");

        List<TaskDTO> tasks = taskService.findHighPriorityTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find tasks by date
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<TaskDTO>> findByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        log.info("GET /api/tasks/date/{} - Finding tasks by date", date);

        List<TaskDTO> tasks = taskService.findByDate(date);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find tasks by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<TaskDTO>> findByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate) {
        log.info("GET /api/tasks/date-range - Finding tasks by date range: {} to {}", fromDate, toDate);

        try {
            List<TaskDTO> tasks = taskService.findByDateRange(fromDate, toDate);
            return ResponseEntity.ok(tasks);
        } catch (IllegalArgumentException e) {
            log.error("Error finding tasks by date range: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find tasks by datetime range
     */
    @GetMapping("/datetime-range")
    public ResponseEntity<List<TaskDTO>> findByDateTimeRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime fromDateTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime toDateTime) {
        log.info("GET /api/tasks/datetime-range - Finding tasks by datetime range: {} to {}", fromDateTime, toDateTime);

        try {
            List<TaskDTO> tasks = taskService.findByDateTimeRange(fromDateTime, toDateTime);
            return ResponseEntity.ok(tasks);
        } catch (IllegalArgumentException e) {
            log.error("Error finding tasks by datetime range: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find overlapping tasks
     */
    @GetMapping("/overlapping")
    public ResponseEntity<List<TaskDTO>> findOverlapping(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDateTime) {
        log.info("GET /api/tasks/overlapping - Finding overlapping tasks: {} to {}", startDateTime, endDateTime);

        try {
            List<TaskDTO> tasks = taskService.findOverlappingTasks(startDateTime, endDateTime);
            return ResponseEntity.ok(tasks);
        } catch (IllegalArgumentException e) {
            log.error("Error finding overlapping tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find tasks by required qualification
     */
    @GetMapping("/qualification/{qualificationId}")
    public ResponseEntity<List<TaskDTO>> findByRequiredQualification(@PathVariable Long qualificationId) {
        log.info("GET /api/tasks/qualification/{} - Finding tasks by required qualification", qualificationId);

        List<TaskDTO> tasks = taskService.findByRequiredQualification(qualificationId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find tasks requiring all specified qualifications
     */
    @PostMapping("/qualifications/all")
    public ResponseEntity<List<TaskDTO>> findByAllRequiredQualifications(@RequestBody List<Long> qualificationIds) {
        log.info("POST /api/tasks/qualifications/all - Finding tasks with all required qualifications: {}", qualificationIds);

        List<TaskDTO> tasks = taskService.findByAllRequiredQualifications(qualificationIds);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find tasks requiring any of the specified qualifications
     */
    @PostMapping("/qualifications/any")
    public ResponseEntity<List<TaskDTO>> findByAnyRequiredQualification(@RequestBody List<Long> qualificationIds) {
        log.info("POST /api/tasks/qualifications/any - Finding tasks with any required qualifications: {}", qualificationIds);

        List<TaskDTO> tasks = taskService.findByAnyRequiredQualification(qualificationIds);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find tasks without required qualifications
     */
    @GetMapping("/without-qualifications")
    public ResponseEntity<List<TaskDTO>> findTasksWithoutRequiredQualifications() {
        log.info("GET /api/tasks/without-qualifications - Finding tasks without required qualifications");

        List<TaskDTO> tasks = taskService.findTasksWithoutRequiredQualifications();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find upcoming tasks
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<TaskDTO>> findUpcomingTasks() {
        log.info("GET /api/tasks/upcoming - Finding upcoming tasks");

        List<TaskDTO> tasks = taskService.findUpcomingTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find ongoing tasks
     */
    @GetMapping("/ongoing")
    public ResponseEntity<List<TaskDTO>> findOngoingTasks() {
        log.info("GET /api/tasks/ongoing - Finding ongoing tasks");

        List<TaskDTO> tasks = taskService.findOngoingTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Find completed tasks
     */
    @GetMapping("/completed")
    public ResponseEntity<List<TaskDTO>> findCompletedTasks() {
        log.info("GET /api/tasks/completed - Finding completed tasks");

        List<TaskDTO> tasks = taskService.findCompletedTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get active task count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        log.info("GET /api/tasks/count - Getting active task count");

        long count = taskService.getActiveCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get task count by department
     */
    @GetMapping("/count/department/{departmentId}")
    public ResponseEntity<Long> getCountByDepartment(@PathVariable Long departmentId) {
        log.info("GET /api/tasks/count/department/{} - Getting task count by department", departmentId);

        long count = taskService.getCountByDepartment(departmentId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get task count by priority
     */
    @GetMapping("/count/priority/{priority}")
    public ResponseEntity<Long> getCountByPriority(@PathVariable Integer priority) {
        log.info("GET /api/tasks/count/priority/{} - Getting task count by priority", priority);

        long count = taskService.getCountByPriority(priority);
        return ResponseEntity.ok(count);
    }

    /**
     * Get tasks by IDs (for bulk operations)
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<TaskDTO>> findByIds(@RequestBody List<Long> ids) {
        log.info("POST /api/tasks/bulk - Finding tasks by IDs: {}", ids);

        List<TaskDTO> tasks = taskService.findByIds(ids);
        return ResponseEntity.ok(tasks);
    }
}