package com.rosteroptimization.repository;

import com.rosteroptimization.entity.WorkingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkingPeriodRepository extends JpaRepository<WorkingPeriod, Long> {

    // Find all active working periods
    List<WorkingPeriod> findByActiveTrue();

    // Find all working periods (including inactive)
    List<WorkingPeriod> findAllByOrderByStartTimeAsc();

    // Find active working periods ordered by start time
    List<WorkingPeriod> findByActiveTrueOrderByStartTimeAsc();

    // Find by name (case insensitive)
    Optional<WorkingPeriod> findByNameIgnoreCaseAndActiveTrue(String name);

    // Search working periods by name containing (case insensitive)
    @Query("SELECT wp FROM WorkingPeriod wp WHERE LOWER(wp.name) LIKE LOWER(CONCAT('%', :name, '%')) AND wp.active = :active")
    List<WorkingPeriod> searchByNameContaining(@Param("name") String name, @Param("active") Boolean active);

    // Find working periods by time range
    @Query("SELECT wp FROM WorkingPeriod wp WHERE wp.startTime >= :startTime AND wp.endTime <= :endTime AND wp.active = true")
    List<WorkingPeriod> findByTimeRange(@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    // Find overlapping working periods
    @Query("SELECT wp FROM WorkingPeriod wp WHERE " +
            "((wp.startTime <= :startTime AND wp.endTime > :startTime) OR " +
            "(wp.startTime < :endTime AND wp.endTime >= :endTime) OR " +
            "(wp.startTime >= :startTime AND wp.endTime <= :endTime)) AND " +
            "wp.active = true")
    List<WorkingPeriod> findOverlappingPeriods(@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    // Count active working periods
    long countByActiveTrue();
}