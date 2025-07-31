package com.rosteroptimization.repository;

import com.rosteroptimization.entity.Squad;
import com.rosteroptimization.entity.SquadWorkingPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SquadRepository extends JpaRepository<Squad, Long> {

    // Find all active squads
    List<Squad> findByActiveTrue();

    // Find all squads (including inactive)
    List<Squad> findAllByOrderByNameAsc();

    // Find active squads ordered by name
    List<Squad> findByActiveTrueOrderByNameAsc();

    // Find by name (case insensitive)
    Optional<Squad> findByNameIgnoreCaseAndActiveTrue(String name);

    // Search squads by name containing (case insensitive)
    @Query("SELECT s FROM Squad s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) AND s.active = :active")
    List<Squad> searchByNameContaining(@Param("name") String name, @Param("active") Boolean active);

    // Find squads by working pattern
    List<Squad> findBySquadWorkingPatternAndActiveTrue(SquadWorkingPattern squadWorkingPattern);

    // Find squads by working pattern ID
    List<Squad> findBySquadWorkingPatternIdAndActiveTrue(Long squadWorkingPatternId);

    // Find squads by start date
    List<Squad> findByStartDateAndActiveTrue(LocalDate startDate);

    // Find squads by start date range
    @Query("SELECT s FROM Squad s WHERE s.startDate >= :fromDate AND s.startDate <= :toDate AND s.active = true")
    List<Squad> findByStartDateBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    // Find squads started after date
    List<Squad> findByStartDateAfterAndActiveTrue(LocalDate date);

    // Find squads started before date
    List<Squad> findByStartDateBeforeAndActiveTrue(LocalDate date);

    // Count active squads
    long countByActiveTrue();

    // Count squads by working pattern
    long countBySquadWorkingPatternIdAndActiveTrue(Long squadWorkingPatternId);

    // Count staff in squad
    @Query("SELECT COUNT(st) FROM Staff st WHERE st.squad.id = :squadId AND st.active = true")
    long countStaffInSquad(@Param("squadId") Long squadId);

    // Find squads with staff count
    @Query("SELECT s FROM Squad s LEFT JOIN s.staffList st WHERE s.active = true GROUP BY s HAVING COUNT(st) >= :minStaffCount")
    List<Squad> findSquadsWithMinimumStaff(@Param("minStaffCount") long minStaffCount);

    // Find squads without staff
    @Query("SELECT s FROM Squad s WHERE s.active = true AND s.id NOT IN (SELECT DISTINCT st.squad.id FROM Staff st WHERE st.active = true)")
    List<Squad> findSquadsWithoutStaff();
}