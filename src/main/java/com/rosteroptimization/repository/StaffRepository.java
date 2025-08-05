package com.rosteroptimization.repository;

import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Department;
import com.rosteroptimization.entity.Squad;
import com.rosteroptimization.entity.Qualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    // Find all active staff
    List<Staff> findByActiveTrue();

    // Find all staff (including inactive)
    List<Staff> findAllByOrderBySurnameAscNameAsc();

    // Find active staff ordered by surname, name
    List<Staff> findByActiveTrueOrderBySurnameAscNameAsc();

    // Find by registration code
    Optional<Staff> findByRegistrationCodeAndActiveTrue(String registrationCode);

    // Find by email
    Optional<Staff> findByEmailIgnoreCaseAndActiveTrue(String email);

    // Search staff by name or surname containing (case insensitive)
    @Query("SELECT s FROM Staff s WHERE (LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND s.active = :active")
    List<Staff> searchByNameOrSurnameContaining(@Param("searchTerm") String searchTerm, @Param("active") Boolean active);

    // Find staff by department
    List<Staff> findByDepartmentAndActiveTrue(Department department);

    // Find staff by department ID
    @Query("SELECT DISTINCT s FROM Staff s " +
           "LEFT JOIN FETCH s.constraintOverrides " +
           "LEFT JOIN FETCH s.squad sq " +
           "LEFT JOIN FETCH sq.squadWorkingPattern " +
           "WHERE s.department.id = :departmentId AND s.active = true")
    List<Staff> findByDepartmentIdAndActiveTrue(@Param("departmentId") Long departmentId);

    // Find staff by squad
    List<Staff> findBySquadAndActiveTrue(Squad squad);

    // Find staff by squad ID
    List<Staff> findBySquadIdAndActiveTrue(Long squadId);

    // Find staff by qualification
    @Query("SELECT s FROM Staff s JOIN s.qualifications q WHERE q = :qualification AND s.active = true")
    List<Staff> findByQualification(@Param("qualification") Qualification qualification);

    // Find staff by qualification ID
    @Query("SELECT s FROM Staff s JOIN s.qualifications q WHERE q.id = :qualificationId AND s.active = true")
    List<Staff> findByQualificationId(@Param("qualificationId") Long qualificationId);

    // Find staff with specific qualifications (all must match)
    @Query("SELECT s FROM Staff s WHERE s.active = true AND " +
            "SIZE(s.qualifications) >= :qualificationCount AND " +
            "EXISTS (SELECT 1 FROM Staff s2 JOIN s2.qualifications q WHERE s2 = s AND q.id IN :qualificationIds " +
            "GROUP BY s2 HAVING COUNT(DISTINCT q.id) = :qualificationCount)")
    List<Staff> findByAllQualifications(@Param("qualificationIds") List<Long> qualificationIds,
                                        @Param("qualificationCount") long qualificationCount);

    // Find staff with any of the specified qualifications
    @Query("SELECT DISTINCT s FROM Staff s JOIN s.qualifications q WHERE q.id IN :qualificationIds AND s.active = true")
    List<Staff> findByAnyQualification(@Param("qualificationIds") List<Long> qualificationIds);

    // Find staff without qualifications
    @Query("SELECT s FROM Staff s WHERE s.active = true AND (s.qualifications IS EMPTY OR SIZE(s.qualifications) = 0)")
    List<Staff> findStaffWithoutQualifications();

    // Find staff with day off rules
    @Query("SELECT s FROM Staff s WHERE s.dayOffRule IS NOT NULL AND s.active = true")
    List<Staff> findStaffWithDayOffRules();

    // Find staff without day off rules
    @Query("SELECT s FROM Staff s WHERE s.dayOffRule IS NULL AND s.active = true")
    List<Staff> findStaffWithoutDayOffRules();

    // Find staff with constraint overrides
    @Query("SELECT DISTINCT s FROM Staff s WHERE SIZE(s.constraintOverrides) > 0 AND s.active = true")
    List<Staff> findStaffWithConstraintOverrides();

    // Count active staff
    long countByActiveTrue();

    // Count staff by department
    long countByDepartmentIdAndActiveTrue(Long departmentId);

    // Count staff by squad
    long countBySquadIdAndActiveTrue(Long squadId);

    // Count staff with specific qualification
    @Query("SELECT COUNT(DISTINCT s) FROM Staff s JOIN s.qualifications q WHERE q.id = :qualificationId AND s.active = true")
    long countByQualificationId(@Param("qualificationId") Long qualificationId);

    // Find staff by IDs (for bulk operations)
    List<Staff> findByIdInAndActiveTrue(List<Long> ids);

    // Check if registration code exists (for uniqueness validation)
    boolean existsByRegistrationCodeAndIdNotAndActiveTrue(String registrationCode, Long id);

    // Check if email exists (for uniqueness validation)
    boolean existsByEmailIgnoreCaseAndIdNotAndActiveTrue(String email, Long id);
}