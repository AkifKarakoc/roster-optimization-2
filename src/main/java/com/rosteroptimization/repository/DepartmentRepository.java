package com.rosteroptimization.repository;

import com.rosteroptimization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // Find all active departments
    List<Department> findByActiveTrue();

    // Find all departments (including inactive)
    List<Department> findAllByOrderByNameAsc();

    // Find active departments ordered by name
    List<Department> findByActiveTrueOrderByNameAsc();

    // Find by name (case insensitive)
    Optional<Department> findByNameIgnoreCaseAndActiveTrue(String name);

    // Search departments by name containing (case insensitive)
    @Query("SELECT d FROM Department d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) AND d.active = :active")
    List<Department> searchByNameContaining(@Param("name") String name, @Param("active") Boolean active);

    // Count active departments
    long countByActiveTrue();
}