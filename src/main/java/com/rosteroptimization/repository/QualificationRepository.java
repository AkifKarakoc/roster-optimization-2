package com.rosteroptimization.repository;

import com.rosteroptimization.entity.Qualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QualificationRepository extends JpaRepository<Qualification, Long> {

    // Find all active qualifications
    List<Qualification> findByActiveTrue();

    // Find all qualifications (including inactive)
    List<Qualification> findAllByOrderByNameAsc();

    // Find active qualifications ordered by name
    List<Qualification> findByActiveTrueOrderByNameAsc();

    // Find by name (case insensitive)
    Optional<Qualification> findByNameIgnoreCaseAndActiveTrue(String name);

    // Search qualifications by name containing (case insensitive)
    @Query("SELECT q FROM Qualification q WHERE LOWER(q.name) LIKE LOWER(CONCAT('%', :name, '%')) AND q.active = :active")
    List<Qualification> searchByNameContaining(@Param("name") String name, @Param("active") Boolean active);

    // Count active qualifications
    long countByActiveTrue();

    // Find qualifications by IDs (for bulk operations)
    List<Qualification> findByIdInAndActiveTrue(List<Long> ids);
}