package com.rosteroptimization.repository;

import com.rosteroptimization.entity.Shift;
import com.rosteroptimization.entity.WorkingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    // Find all active shifts
    List<Shift> findByActiveTrue();

    // Find all shifts (including inactive)
    List<Shift> findAllByOrderByStartTimeAsc();

    // Find active shifts ordered by start time
    List<Shift> findByActiveTrueOrderByStartTimeAsc();

    // Find by name (case insensitive)
    Optional<Shift> findByNameIgnoreCaseAndActiveTrue(String name);

    // Search shifts by name containing (case insensitive)
    @Query("SELECT s FROM Shift s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) AND s.active = :active")
    List<Shift> searchByNameContaining(@Param("name") String name, @Param("active") Boolean active);

    // Find shifts by working period
    List<Shift> findByWorkingPeriodAndActiveTrue(WorkingPeriod workingPeriod);

    // Find shifts by working period ID
    List<Shift> findByWorkingPeriodIdAndActiveTrue(Long workingPeriodId);

    // Find night shifts
    List<Shift> findByIsNightShiftTrueAndActiveTrue();

    // Find day shifts
    List<Shift> findByIsNightShiftFalseAndActiveTrue();

    // Find fixed shifts
    List<Shift> findByFixedTrueAndActiveTrue();

    // Find flexible shifts
    List<Shift> findByFixedFalseAndActiveTrue();

    // Find shifts by time range
    @Query("SELECT s FROM Shift s WHERE s.startTime >= :startTime AND s.endTime <= :endTime AND s.active = true")
    List<Shift> findByTimeRange(@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    // Find overlapping shifts
    @Query("SELECT s FROM Shift s WHERE " +
            "((s.startTime <= :startTime AND s.endTime > :startTime) OR " +
            "(s.startTime < :endTime AND s.endTime >= :endTime) OR " +
            "(s.startTime >= :startTime AND s.endTime <= :endTime)) AND " +
            "s.active = true")
    List<Shift> findOverlappingShifts(@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    // Count active shifts
    long countByActiveTrue();

    // Count shifts by working period
    long countByWorkingPeriodIdAndActiveTrue(Long workingPeriodId);

    // Find shifts by IDs (for bulk operations)
    List<Shift> findByIdInAndActiveTrue(List<Long> ids);
}