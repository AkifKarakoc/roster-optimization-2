package com.rosteroptimization.repository;

import com.rosteroptimization.entity.ConstraintOverride;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Constraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConstraintOverrideRepository extends JpaRepository<ConstraintOverride, Long> {

    // Find by staff
    List<ConstraintOverride> findByStaff(Staff staff);

    // Find by staff ID
    List<ConstraintOverride> findByStaffId(Long staffId);

    // Find by staff IDs and active constraints
    @Query("SELECT co FROM ConstraintOverride co WHERE co.staff.id IN :staffIds AND co.constraint.active = true AND co.active = true")
    List<ConstraintOverride> findByStaffIdInAndConstraintActiveTrueAndActiveTrue(@Param("staffIds") List<Long> staffIds);

    // Find by constraint
    List<ConstraintOverride> findByConstraint(Constraint constraint);

    // Find by constraint ID
    List<ConstraintOverride> findByConstraintId(Long constraintId);

    // Find by staff and constraint
    Optional<ConstraintOverride> findByStaffAndConstraint(Staff staff, Constraint constraint);

    // Find by staff ID and constraint ID
    Optional<ConstraintOverride> findByStaffIdAndConstraintId(Long staffId, Long constraintId);

    // Find by staff registration code
    @Query("SELECT co FROM ConstraintOverride co WHERE co.staff.registrationCode = :registrationCode AND co.staff.active = true")
    List<ConstraintOverride> findByStaffRegistrationCode(@Param("registrationCode") String registrationCode);

    // Find by constraint name
    @Query("SELECT co FROM ConstraintOverride co WHERE co.constraint.name = :constraintName AND co.constraint.active = true")
    List<ConstraintOverride> findByConstraintName(@Param("constraintName") String constraintName);

    // Find by staff and constraint name
    @Query("SELECT co FROM ConstraintOverride co WHERE co.staff.id = :staffId AND co.constraint.name = :constraintName AND co.staff.active = true AND co.constraint.active = true")
    Optional<ConstraintOverride> findByStaffIdAndConstraintName(@Param("staffId") Long staffId, @Param("constraintName") String constraintName);

    // Find by override value
    List<ConstraintOverride> findByOverrideValue(String overrideValue);

    // Find by department (through staff)
    @Query("SELECT co FROM ConstraintOverride co WHERE co.staff.department.id = :departmentId AND co.staff.active = true")
    List<ConstraintOverride> findByStaffDepartmentId(@Param("departmentId") Long departmentId);

    // Find by squad (through staff)
    @Query("SELECT co FROM ConstraintOverride co WHERE co.staff.squad.id = :squadId AND co.staff.active = true")
    List<ConstraintOverride> findByStaffSquadId(@Param("squadId") Long squadId);

    // Find by constraint type
    @Query("SELECT co FROM ConstraintOverride co WHERE co.constraint.type = :constraintType AND co.constraint.active = true")
    List<ConstraintOverride> findByConstraintType(@Param("constraintType") Constraint.ConstraintType constraintType);

    // Find HARD constraint overrides
    @Query("SELECT co FROM ConstraintOverride co WHERE co.constraint.type = 'HARD' AND co.constraint.active = true")
    List<ConstraintOverride> findHardConstraintOverrides();

    // Find SOFT constraint overrides
    @Query("SELECT co FROM ConstraintOverride co WHERE co.constraint.type = 'SOFT' AND co.constraint.active = true")
    List<ConstraintOverride> findSoftConstraintOverrides();

    // Count all constraint overrides
    long count();

    // Count overrides by staff ID
    long countByStaffId(Long staffId);

    // Count overrides by constraint ID
    long countByConstraintId(Long constraintId);

    // Count overrides by constraint type
    @Query("SELECT COUNT(co) FROM ConstraintOverride co WHERE co.constraint.type = :constraintType AND co.constraint.active = true")
    long countByConstraintType(@Param("constraintType") Constraint.ConstraintType constraintType);

    // Count overrides by department
    @Query("SELECT COUNT(co) FROM ConstraintOverride co WHERE co.staff.department.id = :departmentId AND co.staff.active = true")
    long countByStaffDepartmentId(@Param("departmentId") Long departmentId);

    // Check if staff-constraint combination exists
    boolean existsByStaffIdAndConstraintId(Long staffId, Long constraintId);

    // Find overrides by IDs (for bulk operations)
    List<ConstraintOverride> findByIdIn(List<Long> ids);

    // Delete by staff and constraint
    void deleteByStaffIdAndConstraintId(Long staffId, Long constraintId);

    // Find staff with most overrides
    @Query("SELECT co.staff FROM ConstraintOverride co WHERE co.staff.active = true GROUP BY co.staff ORDER BY COUNT(co) DESC")
    List<Staff> findStaffWithMostOverrides();

    // Find most overridden constraints
    @Query("SELECT co.constraint FROM ConstraintOverride co WHERE co.constraint.active = true GROUP BY co.constraint ORDER BY COUNT(co) DESC")
    List<Constraint> findMostOverriddenConstraints();

    List<ConstraintOverride> findByStaffAndActiveTrue(Staff staff);
}