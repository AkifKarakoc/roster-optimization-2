package com.rosteroptimization.repository;

import com.rosteroptimization.entity.DayOffRule;
import com.rosteroptimization.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DayOffRuleRepository extends JpaRepository<DayOffRule, Long> {

    // Find by staff
    Optional<DayOffRule> findByStaff(Staff staff);

    // Find by staff ID
    Optional<DayOffRule> findByStaffId(Long staffId);

    // Find by staff registration code
    @Query("SELECT d FROM DayOffRule d WHERE d.staff.registrationCode = :registrationCode AND d.staff.active = true")
    Optional<DayOffRule> findByStaffRegistrationCode(@Param("registrationCode") String registrationCode);

    // Find by working days
    List<DayOffRule> findByWorkingDays(Integer workingDays);

    // Find by off days
    List<DayOffRule> findByOffDays(Integer offDays);

    // Find by working days range
    @Query("SELECT d FROM DayOffRule d WHERE d.workingDays >= :minWorkingDays AND d.workingDays <= :maxWorkingDays")
    List<DayOffRule> findByWorkingDaysBetween(@Param("minWorkingDays") Integer minWorkingDays, @Param("maxWorkingDays") Integer maxWorkingDays);

    // Find by off days range
    @Query("SELECT d FROM DayOffRule d WHERE d.offDays >= :minOffDays AND d.offDays <= :maxOffDays")
    List<DayOffRule> findByOffDaysBetween(@Param("minOffDays") Integer minOffDays, @Param("maxOffDays") Integer maxOffDays);

    // Find rules with fixed off days
    @Query("SELECT d FROM DayOffRule d WHERE d.fixedOffDays IS NOT NULL AND d.fixedOffDays != ''")
    List<DayOffRule> findRulesWithFixedOffDays();

    // Find rules without fixed off days
    @Query("SELECT d FROM DayOffRule d WHERE d.fixedOffDays IS NULL OR d.fixedOffDays = ''")
    List<DayOffRule> findRulesWithoutFixedOffDays();

    // Find rules containing specific fixed off day
    @Query("SELECT d FROM DayOffRule d WHERE d.fixedOffDays LIKE CONCAT('%', :dayOfWeek, '%')")
    List<DayOffRule> findByFixedOffDayContaining(@Param("dayOfWeek") String dayOfWeek);

    // Find by department (through staff)
    @Query("SELECT d FROM DayOffRule d WHERE d.staff.department.id = :departmentId AND d.staff.active = true")
    List<DayOffRule> findByStaffDepartmentId(@Param("departmentId") Long departmentId);

    // Find by squad (through staff)
    @Query("SELECT d FROM DayOffRule d WHERE d.staff.squad.id = :squadId AND d.staff.active = true")
    List<DayOffRule> findByStaffSquadId(@Param("squadId") Long squadId);

    // Count all day off rules
    long count();

    // Count rules with fixed off days
    @Query("SELECT COUNT(d) FROM DayOffRule d WHERE d.fixedOffDays IS NOT NULL AND d.fixedOffDays != ''")
    long countRulesWithFixedOffDays();

    // Count rules by department
    @Query("SELECT COUNT(d) FROM DayOffRule d WHERE d.staff.department.id = :departmentId AND d.staff.active = true")
    long countByStaffDepartmentId(@Param("departmentId") Long departmentId);

    // Check if staff already has day off rule
    boolean existsByStaffId(Long staffId);
}