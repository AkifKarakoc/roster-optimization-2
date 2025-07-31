package com.rosteroptimization.repository;

import com.rosteroptimization.entity.SquadWorkingPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SquadWorkingPatternRepository extends JpaRepository<SquadWorkingPattern, Long> {

    // Find all active squad working patterns
    List<SquadWorkingPattern> findByActiveTrue();

    // Find all squad working patterns (including inactive)
    List<SquadWorkingPattern> findAllByOrderByNameAsc();

    // Find active squad working patterns ordered by name
    List<SquadWorkingPattern> findByActiveTrueOrderByNameAsc();

    // Find by name (case insensitive)
    Optional<SquadWorkingPattern> findByNameIgnoreCaseAndActiveTrue(String name);

    // Search squad working patterns by name containing (case insensitive)
    @Query("SELECT swp FROM SquadWorkingPattern swp WHERE LOWER(swp.name) LIKE LOWER(CONCAT('%', :name, '%')) AND swp.active = :active")
    List<SquadWorkingPattern> searchByNameContaining(@Param("name") String name, @Param("active") Boolean active);

    // Find by cycle length
    List<SquadWorkingPattern> findByCycleLengthAndActiveTrue(Integer cycleLength);

    // Find patterns containing specific shift
    @Query("SELECT swp FROM SquadWorkingPattern swp WHERE swp.shiftPattern LIKE CONCAT('%', :shiftName, '%') AND swp.active = true")
    List<SquadWorkingPattern> findByShiftPatternContaining(@Param("shiftName") String shiftName);

    // Find patterns by cycle length range
    @Query("SELECT swp FROM SquadWorkingPattern swp WHERE swp.cycleLength >= :minLength AND swp.cycleLength <= :maxLength AND swp.active = true")
    List<SquadWorkingPattern> findByCycleLengthBetween(@Param("minLength") Integer minLength, @Param("maxLength") Integer maxLength);

    // Count active squad working patterns
    long countByActiveTrue();

    // Count squads using this pattern
    @Query("SELECT COUNT(s) FROM Squad s WHERE s.squadWorkingPattern.id = :patternId AND s.active = true")
    long countSquadsUsingPattern(@Param("patternId") Long patternId);
}