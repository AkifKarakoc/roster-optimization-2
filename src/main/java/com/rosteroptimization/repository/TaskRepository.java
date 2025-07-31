package com.rosteroptimization.repository;

import com.rosteroptimization.entity.Task;
import com.rosteroptimization.entity.Department;
import com.rosteroptimization.entity.Qualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Find all active tasks
    List<Task> findByActiveTrue();

    // Find all tasks (including inactive)
    List<Task> findAllByOrderByStartTimeAsc();

    // Find active tasks ordered by start time
    List<Task> findByActiveTrueOrderByStartTimeAsc();

    // Find active tasks ordered by priority, then start time
    List<Task> findByActiveTrueOrderByPriorityAscStartTimeAsc();

    // Find by name (case insensitive)
    Optional<Task> findByNameIgnoreCaseAndActiveTrue(String name);

    // Search tasks by name containing (case insensitive)
    @Query("SELECT t FROM Task t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) AND t.active = :active")
    List<Task> searchByNameContaining(@Param("name") String name, @Param("active") Boolean active);

    // Find tasks by department
    List<Task> findByDepartmentAndActiveTrue(Department department);

    // Find tasks by department ID
    List<Task> findByDepartmentIdAndActiveTrue(Long departmentId);

    // Find tasks by priority
    List<Task> findByPriorityAndActiveTrueOrderByStartTimeAsc(Integer priority);

    // Find tasks by priority range
    @Query("SELECT t FROM Task t WHERE t.priority >= :minPriority AND t.priority <= :maxPriority AND t.active = true ORDER BY t.priority ASC, t.startTime ASC")
    List<Task> findByPriorityBetween(@Param("minPriority") Integer minPriority, @Param("maxPriority") Integer maxPriority);

    // Find high priority tasks (priority <= 2)
    @Query("SELECT t FROM Task t WHERE t.priority <= 2 AND t.active = true ORDER BY t.priority ASC, t.startTime ASC")
    List<Task> findHighPriorityTasks();

    // Find tasks by date range
    @Query("SELECT t FROM Task t WHERE t.startTime >= :fromDateTime AND t.endTime <= :toDateTime AND t.active = true ORDER BY t.startTime ASC")
    List<Task> findByDateTimeRange(@Param("fromDateTime") LocalDateTime fromDateTime, @Param("toDateTime") LocalDateTime toDateTime);

    // Find tasks by date (all tasks that occur on a specific date)
    @Query("SELECT t FROM Task t WHERE DATE(t.startTime) = :date AND t.active = true ORDER BY t.startTime ASC")
    List<Task> findByDate(@Param("date") LocalDate date);

    // Find tasks between dates
    @Query("SELECT t FROM Task t WHERE DATE(t.startTime) >= :fromDate AND DATE(t.endTime) <= :toDate AND t.active = true ORDER BY t.startTime ASC")
    List<Task> findByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    // Find overlapping tasks (tasks that overlap with given time range)
    @Query("SELECT t FROM Task t WHERE " +
            "((t.startTime <= :startDateTime AND t.endTime > :startDateTime) OR " +
            "(t.startTime < :endDateTime AND t.endTime >= :endDateTime) OR " +
            "(t.startTime >= :startDateTime AND t.endTime <= :endDateTime)) AND " +
            "t.active = true ORDER BY t.startTime ASC")
    List<Task> findOverlappingTasks(@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);

    // Find tasks by required qualification
    @Query("SELECT t FROM Task t JOIN t.requiredQualifications q WHERE q = :qualification AND t.active = true")
    List<Task> findByRequiredQualification(@Param("qualification") Qualification qualification);

    // Find tasks by required qualification ID
    @Query("SELECT t FROM Task t JOIN t.requiredQualifications q WHERE q.id = :qualificationId AND t.active = true")
    List<Task> findByRequiredQualificationId(@Param("qualificationId") Long qualificationId);

    // Find tasks requiring all specified qualifications
    @Query("SELECT t FROM Task t WHERE t.active = true AND " +
            "SIZE(t.requiredQualifications) >= :qualificationCount AND " +
            "EXISTS (SELECT 1 FROM Task t2 JOIN t2.requiredQualifications q WHERE t2 = t AND q.id IN :qualificationIds " +
            "GROUP BY t2 HAVING COUNT(DISTINCT q.id) = :qualificationCount)")
    List<Task> findByAllRequiredQualifications(@Param("qualificationIds") List<Long> qualificationIds,
                                               @Param("qualificationCount") long qualificationCount);

    // Find tasks requiring any of the specified qualifications
    @Query("SELECT DISTINCT t FROM Task t JOIN t.requiredQualifications q WHERE q.id IN :qualificationIds AND t.active = true")
    List<Task> findByAnyRequiredQualification(@Param("qualificationIds") List<Long> qualificationIds);

    // Find tasks without required qualifications
    @Query("SELECT t FROM Task t WHERE t.active = true AND (t.requiredQualifications IS EMPTY OR SIZE(t.requiredQualifications) = 0)")
    List<Task> findTasksWithoutRequiredQualifications();

    // Find upcoming tasks (from now)
    @Query("SELECT t FROM Task t WHERE t.startTime > CURRENT_TIMESTAMP AND t.active = true ORDER BY t.startTime ASC")
    List<Task> findUpcomingTasks();

    // Find ongoing tasks (currently active)
    @Query("SELECT t FROM Task t WHERE t.startTime <= CURRENT_TIMESTAMP AND t.endTime > CURRENT_TIMESTAMP AND t.active = true ORDER BY t.priority ASC")
    List<Task> findOngoingTasks();

    // Find completed tasks (ended)
    @Query("SELECT t FROM Task t WHERE t.endTime < CURRENT_TIMESTAMP AND t.active = true ORDER BY t.endTime DESC")
    List<Task> findCompletedTasks();

    // Count active tasks
    long countByActiveTrue();

    // Count tasks by department
    long countByDepartmentIdAndActiveTrue(Long departmentId);

    // Count tasks by priority
    long countByPriorityAndActiveTrue(Integer priority);

    // Count tasks requiring specific qualification
    @Query("SELECT COUNT(DISTINCT t) FROM Task t JOIN t.requiredQualifications q WHERE q.id = :qualificationId AND t.active = true")
    long countByRequiredQualificationId(@Param("qualificationId") Long qualificationId);

    // Find tasks by IDs (for bulk operations)
    List<Task> findByIdInAndActiveTrue(List<Long> ids);
}