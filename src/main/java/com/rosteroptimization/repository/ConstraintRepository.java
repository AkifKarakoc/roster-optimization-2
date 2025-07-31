package com.rosteroptimization.repository;

import com.rosteroptimization.entity.Constraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConstraintRepository extends JpaRepository<Constraint, Long> {

    // Find all active constraints
    List<Constraint> findByActiveTrue();

    // Find all constraints (including inactive)
    List<Constraint> findAllByOrderByNameAsc();

    // Find active constraints ordered by name
    List<Constraint> findByActiveTrueOrderByNameAsc();

    // Find by name (case insensitive)
    Optional<Constraint> findByNameIgnoreCaseAndActiveTrue(String name);

    // Find by name (exact match, case sensitive)
    Optional<Constraint> findByNameAndActiveTrue(String name);

    // Search constraints by name containing (case insensitive)
    @Query("SELECT c FROM Constraint c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) AND c.active = :active")
    List<Constraint> searchByNameContaining(@Param("name") String name, @Param("active") Boolean active);

    // Find by constraint type
    List<Constraint> findByTypeAndActiveTrueOrderByNameAsc(Constraint.ConstraintType type);

    // Find SOFT constraints
    @Query("SELECT c FROM Constraint c WHERE c.type = 'SOFT' AND c.active = true ORDER BY c.name ASC")
    List<Constraint> findSoftConstraints();

    // Find HARD constraints
    @Query("SELECT c FROM Constraint c WHERE c.type = 'HARD' AND c.active = true ORDER BY c.name ASC")
    List<Constraint> findHardConstraints();

    // Search constraints by description containing
    @Query("SELECT c FROM Constraint c WHERE LOWER(c.description) LIKE LOWER(CONCAT('%', :description, '%')) AND c.active = true")
    List<Constraint> searchByDescriptionContaining(@Param("description") String description);

    // Find constraints with overrides
    @Query("SELECT DISTINCT c FROM Constraint c WHERE SIZE(c.constraintOverrides) > 0 AND c.active = true")
    List<Constraint> findConstraintsWithOverrides();

    // Find constraints without overrides
    @Query("SELECT c FROM Constraint c WHERE SIZE(c.constraintOverrides) = 0 AND c.active = true")
    List<Constraint> findConstraintsWithoutOverrides();

    // Count active constraints
    long countByActiveTrue();

    // Count constraints by type
    long countByTypeAndActiveTrue(Constraint.ConstraintType type);

    // Count constraints with overrides
    @Query("SELECT COUNT(DISTINCT c) FROM Constraint c WHERE SIZE(c.constraintOverrides) > 0 AND c.active = true")
    long countConstraintsWithOverrides();

    // Find constraints by IDs (for bulk operations)
    List<Constraint> findByIdInAndActiveTrue(List<Long> ids);

    // Check if constraint name exists (for uniqueness validation)
    boolean existsByNameIgnoreCaseAndIdNotAndActiveTrue(String name, Long id);
}