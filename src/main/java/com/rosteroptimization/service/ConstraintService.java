package com.rosteroptimization.service;

import com.rosteroptimization.dto.ConstraintDTO;
import com.rosteroptimization.entity.Constraint;
import com.rosteroptimization.mapper.ConstraintMapper;
import com.rosteroptimization.repository.ConstraintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConstraintService {

    private final ConstraintRepository constraintRepository;
    private final ConstraintMapper constraintMapper;

    /**
     * Create new constraint
     */
    public ConstraintDTO create(ConstraintDTO dto) {
        log.info("Creating new constraint: {}", dto.getName());

        // Validate constraint data
        validateConstraintData(dto, null);

        // Check if constraint with same name already exists
        Optional<Constraint> existing = constraintRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Constraint with name '" + dto.getName() + "' already exists");
        }

        Constraint entity = constraintMapper.toEntity(dto);
        Constraint saved = constraintRepository.save(entity);

        log.info("Constraint created with ID: {}", saved.getId());
        return constraintMapper.toDto(saved);
    }

    /**
     * Update existing constraint
     */
    public ConstraintDTO update(Long id, ConstraintDTO dto) {
        log.info("Updating constraint with ID: {}", id);

        // Validate constraint data
        validateConstraintData(dto, id);

        Constraint existing = constraintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constraint not found with ID: " + id));

        // Check if another constraint with same name exists
        if (constraintRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue(dto.getName(), id)) {
            throw new IllegalArgumentException("Constraint with name '" + dto.getName() + "' already exists");
        }

        constraintMapper.updateEntityFromDto(dto, existing);
        Constraint updated = constraintRepository.save(existing);

        log.info("Constraint updated: {}", updated.getName());
        return constraintMapper.toDto(updated);
    }

    /**
     * Soft delete constraint
     */
    public void delete(Long id) {
        log.info("Soft deleting constraint with ID: {}", id);

        Constraint constraint = constraintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constraint not found with ID: " + id));

        // Check if constraint has active overrides
        long overrideCount = constraint.getConstraintOverrides() != null ? constraint.getConstraintOverrides().size() : 0;
        if (overrideCount > 0) {
            log.warn("Deleting constraint that has {} active overrides", overrideCount);
        }

        constraint.setActive(false);
        constraintRepository.save(constraint);

        log.info("Constraint soft deleted: {}", constraint.getName());
    }

    /**
     * Find constraint by ID
     */
    @Transactional(readOnly = true)
    public ConstraintDTO findById(Long id) {
        Constraint constraint = constraintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constraint not found with ID: " + id));

        return constraintMapper.toDto(constraint);
    }

    /**
     * Find all active constraints
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> findAll() {
        List<Constraint> constraints = constraintRepository.findByActiveTrueOrderByNameAsc();
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Find all constraints (including inactive)
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> findAllIncludingInactive() {
        List<Constraint> constraints = constraintRepository.findAllByOrderByNameAsc();
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Search constraints by name
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> searchByName(String name, boolean includeInactive) {
        List<Constraint> constraints = constraintRepository.searchByNameContaining(name, !includeInactive);
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Find constraint by name
     */
    @Transactional(readOnly = true)
    public ConstraintDTO findByName(String name) {
        Constraint constraint = constraintRepository.findByNameAndActiveTrue(name)
                .orElseThrow(() -> new IllegalArgumentException("Constraint not found with name: " + name));

        return constraintMapper.toDto(constraint);
    }

    /**
     * Find constraints by type
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> findByType(Constraint.ConstraintType type) {
        List<Constraint> constraints = constraintRepository.findByTypeAndActiveTrueOrderByNameAsc(type);
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Find HARD constraints
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> findHardConstraints() {
        List<Constraint> constraints = constraintRepository.findHardConstraints();
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Find SOFT constraints
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> findSoftConstraints() {
        List<Constraint> constraints = constraintRepository.findSoftConstraints();
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Search constraints by description
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> searchByDescription(String description) {
        List<Constraint> constraints = constraintRepository.searchByDescriptionContaining(description);
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Find constraints with overrides
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> findConstraintsWithOverrides() {
        List<Constraint> constraints = constraintRepository.findConstraintsWithOverrides();
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Find constraints without overrides
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> findConstraintsWithoutOverrides() {
        List<Constraint> constraints = constraintRepository.findConstraintsWithoutOverrides();
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Get active constraint count
     */
    @Transactional(readOnly = true)
    public long getActiveCount() {
        return constraintRepository.countByActiveTrue();
    }

    /**
     * Get constraint count by type
     */
    @Transactional(readOnly = true)
    public long getCountByType(Constraint.ConstraintType type) {
        return constraintRepository.countByTypeAndActiveTrue(type);
    }

    /**
     * Get count of constraints with overrides
     */
    @Transactional(readOnly = true)
    public long getConstraintsWithOverridesCount() {
        return constraintRepository.countConstraintsWithOverrides();
    }

    /**
     * Find constraints by IDs (for bulk operations)
     */
    @Transactional(readOnly = true)
    public List<ConstraintDTO> findByIds(List<Long> ids) {
        List<Constraint> constraints = constraintRepository.findByIdInAndActiveTrue(ids);
        return constraintMapper.toDtoList(constraints);
    }

    /**
     * Get effective constraint value for staff
     * Returns staff-specific override if exists, otherwise returns default value
     */
    @Transactional(readOnly = true)
    public String getEffectiveValueForStaff(String constraintName, Long staffId) {
        // This method would work with ConstraintOverrideService
        // For now, just return the default value
        ConstraintDTO constraint = findByName(constraintName);
        return constraint.getDefaultValue();
    }

    /**
     * Bulk update constraints
     */
    public List<ConstraintDTO> bulkUpdate(List<ConstraintDTO> constraints) {
        log.info("Bulk updating {} constraints", constraints.size());

        List<ConstraintDTO> updated = constraints.stream()
                .map(constraint -> {
                    if (constraint.getId() != null) {
                        return update(constraint.getId(), constraint);
                    } else {
                        return create(constraint);
                    }
                })
                .toList();

        log.info("Bulk update completed for {} constraints", updated.size());
        return updated;
    }

    /**
     * Validate constraint data
     */
    private void validateConstraintData(ConstraintDTO dto, Long existingId) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Constraint name is required");
        }

        if (dto.getType() == null) {
            throw new IllegalArgumentException("Constraint type is required");
        }

        if (dto.getDefaultValue() == null || dto.getDefaultValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Default value is required");
        }

        // Validate constraint name format (no spaces, alphanumeric + underscore)
        if (!dto.getName().matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Constraint name must start with a letter and contain only letters, numbers, and underscores");
        }

        log.debug("Constraint validation passed for: {}", dto.getName());
    }
}