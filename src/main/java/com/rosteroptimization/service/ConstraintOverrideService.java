package com.rosteroptimization.service;

import com.rosteroptimization.dto.ConstraintOverrideDTO;
import com.rosteroptimization.entity.ConstraintOverride;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Constraint;
import com.rosteroptimization.mapper.ConstraintOverrideMapper;
import com.rosteroptimization.repository.ConstraintOverrideRepository;
import com.rosteroptimization.repository.StaffRepository;
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
public class ConstraintOverrideService {

    private final ConstraintOverrideRepository constraintOverrideRepository;
    private final StaffRepository staffRepository;
    private final ConstraintRepository constraintRepository;
    private final ConstraintOverrideMapper constraintOverrideMapper;

    /**
     * Create new constraint override
     */
    public ConstraintOverrideDTO create(ConstraintOverrideDTO dto) {
        log.info("Creating constraint override for staff ID: {} and constraint ID: {}", dto.getStaffId(), dto.getConstraintId());

        // Validate constraint override data
        validateConstraintOverrideData(dto);

        // Check if staff-constraint combination already exists
        if (constraintOverrideRepository.existsByStaffIdAndConstraintId(dto.getStaffId(), dto.getConstraintId())) {
            throw new IllegalArgumentException("Constraint override already exists for this staff and constraint combination. Use update instead.");
        }

        ConstraintOverride entity = constraintOverrideMapper.toEntity(dto);
        ConstraintOverride saved = constraintOverrideRepository.save(entity);

        log.info("Constraint override created with ID: {}", saved.getId());
        return constraintOverrideMapper.toDto(saved);
    }

    /**
     * Update existing constraint override
     */
    public ConstraintOverrideDTO update(Long id, ConstraintOverrideDTO dto) {
        log.info("Updating constraint override with ID: {}", id);

        // Validate constraint override data
        validateConstraintOverrideData(dto);

        ConstraintOverride existing = constraintOverrideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constraint override not found with ID: " + id));

        // Check if trying to change staff-constraint combination and new combination already exists
        if (!existing.getStaff().getId().equals(dto.getStaffId()) ||
                !existing.getConstraint().getId().equals(dto.getConstraintId())) {
            if (constraintOverrideRepository.existsByStaffIdAndConstraintId(dto.getStaffId(), dto.getConstraintId())) {
                throw new IllegalArgumentException("Constraint override already exists for the target staff and constraint combination");
            }
        }

        constraintOverrideMapper.updateEntityFromDto(dto, existing);
        ConstraintOverride updated = constraintOverrideRepository.save(existing);

        log.info("Constraint override updated: {}", updated.getId());
        return constraintOverrideMapper.toDto(updated);
    }

    /**
     * Delete constraint override
     */
    public void delete(Long id) {
        log.info("Deleting constraint override with ID: {}", id);

        ConstraintOverride constraintOverride = constraintOverrideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constraint override not found with ID: " + id));

        constraintOverrideRepository.delete(constraintOverride);

        log.info("Constraint override deleted: {}", id);
    }

    /**
     * Delete constraint override by staff and constraint
     */
    public void deleteByStaffAndConstraint(Long staffId, Long constraintId) {
        log.info("Deleting constraint override for staff ID: {} and constraint ID: {}", staffId, constraintId);

        constraintOverrideRepository.deleteByStaffIdAndConstraintId(staffId, constraintId);

        log.info("Constraint override deleted for staff ID: {} and constraint ID: {}", staffId, constraintId);
    }

    /**
     * Find constraint override by ID
     */
    @Transactional(readOnly = true)
    public ConstraintOverrideDTO findById(Long id) {
        ConstraintOverride constraintOverride = constraintOverrideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constraint override not found with ID: " + id));

        return constraintOverrideMapper.toDto(constraintOverride);
    }

    /**
     * Find all constraint overrides
     */
    @Transactional(readOnly = true)
    public List<ConstraintOverrideDTO> findAll() {
        List<ConstraintOverride> constraintOverrides = constraintOverrideRepository.findAll();
        return constraintOverrideMapper.toDtoList(constraintOverrides);
    }

    /**
     * Find constraint overrides by staff ID
     */
    @Transactional(readOnly = true)
    public List<ConstraintOverrideDTO> findByStaffId(Long staffId) {
        List<ConstraintOverride> constraintOverrides = constraintOverrideRepository.findByStaffId(staffId);
        return constraintOverrideMapper.toDtoList(constraintOverrides);
    }

    /**
     * Find constraint overrides by constraint ID
     */
    @Transactional(readOnly = true)
    public List<ConstraintOverrideDTO> findByConstraintId(Long constraintId) {
        List<ConstraintOverride> constraintOverrides = constraintOverrideRepository.findByConstraintId(constraintId);
        return constraintOverrideMapper.toDtoList(constraintOverrides);
    }

    /**
     * Find constraint override by staff and constraint
     */
    @Transactional(readOnly = true)
    public ConstraintOverrideDTO findByStaffAndConstraint(Long staffId, Long constraintId) {
        ConstraintOverride constraintOverride = constraintOverrideRepository.findByStaffIdAndConstraintId(staffId, constraintId)
                .orElseThrow(() -> new IllegalArgumentException("Constraint override not found for staff ID: " + staffId + " and constraint ID: " + constraintId));

        return constraintOverrideMapper.toDto(constraintOverride);
    }

    /**
     * Find optional constraint override by staff and constraint
     */
    @Transactional(readOnly = true)
    public Optional<ConstraintOverrideDTO> findOptionalByStaffAndConstraint(Long staffId, Long constraintId) {
        Optional<ConstraintOverride> constraintOverride = constraintOverrideRepository.findByStaffIdAndConstraintId(staffId, constraintId);
        return constraintOverride.map(constraintOverrideMapper::toDto);
    }

    /**
     * Find constraint overrides by staff registration code
     */
    @Transactional(readOnly = true)
    public List<ConstraintOverrideDTO> findByStaffRegistrationCode(String registrationCode) {
        List<ConstraintOverride> constraintOverrides = constraintOverrideRepository.findByStaffRegistrationCode(registrationCode);
        return constraintOverrideMapper.toDtoList(constraintOverrides);
    }

    /**
     * Find constraint overrides by constraint name
     */
    @Transactional(readOnly = true)
    public List<ConstraintOverrideDTO> findByConstraintName(String constraintName) {
        List<ConstraintOverride> constraintOverrides = constraintOverrideRepository.findByConstraintName(constraintName);
        return constraintOverrideMapper.toDtoList(constraintOverrides);
    }

    /**
     * Find constraint overrides by department
     */
    @Transactional(readOnly = true)
    public List<ConstraintOverrideDTO> findByDepartment(Long departmentId) {
        List<ConstraintOverride> constraintOverrides = constraintOverrideRepository.findByStaffDepartmentId(departmentId);
        return constraintOverrideMapper.toDtoList(constraintOverrides);
    }

    /**
     * Find constraint overrides by squad
     */
    @Transactional(readOnly = true)
    public List<ConstraintOverrideDTO> findBySquad(Long squadId) {
        List<ConstraintOverride> constraintOverrides = constraintOverrideRepository.findByStaffSquadId(squadId);
        return constraintOverrideMapper.toDtoList(constraintOverrides);
    }

    /**
     * Find HARD constraint overrides
     */
    @Transactional(readOnly = true)
    public List<ConstraintOverrideDTO> findHardConstraintOverrides() {
        List<ConstraintOverride> constraintOverrides = constraintOverrideRepository.findHardConstraintOverrides();
        return constraintOverrideMapper.toDtoList(constraintOverrides);
    }

    /**
     * Find SOFT constraint overrides
     */
    @Transactional(readOnly = true)
    public List<ConstraintOverrideDTO> findSoftConstraintOverrides() {
        List<ConstraintOverride> constraintOverrides = constraintOverrideRepository.findSoftConstraintOverrides();
        return constraintOverrideMapper.toDtoList(constraintOverrides);
    }

    /**
     * Get total constraint override count
     */
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return constraintOverrideRepository.count();
    }

    /**
     * Get constraint override count by staff
     */
    @Transactional(readOnly = true)
    public long getCountByStaff(Long staffId) {
        return constraintOverrideRepository.countByStaffId(staffId);
    }

    /**
     * Get constraint override count by constraint
     */
    @Transactional(readOnly = true)
    public long getCountByConstraint(Long constraintId) {
        return constraintOverrideRepository.countByConstraintId(constraintId);
    }

    /**
     * Check if constraint override exists
     */
    @Transactional(readOnly = true)
    public boolean exists(Long staffId, Long constraintId) {
        return constraintOverrideRepository.existsByStaffIdAndConstraintId(staffId, constraintId);
    }

    /**
     * Get effective constraint value for staff
     * Returns override value if exists, otherwise returns constraint default value
     */
    @Transactional(readOnly = true)
    public String getEffectiveValue(Long staffId, String constraintName) {
        Optional<ConstraintOverride> override = constraintOverrideRepository.findByStaffIdAndConstraintName(staffId, constraintName);

        if (override.isPresent()) {
            return override.get().getOverrideValue();
        }

        // Return default value from constraint
        Constraint constraint = constraintRepository.findByNameAndActiveTrue(constraintName)
                .orElseThrow(() -> new IllegalArgumentException("Constraint not found: " + constraintName));

        return constraint.getDefaultValue();
    }

    /**
     * Bulk create or update constraint overrides for staff
     */
    public List<ConstraintOverrideDTO> bulkCreateOrUpdateForStaff(Long staffId, List<ConstraintOverrideDTO> overrides) {
        log.info("Bulk creating/updating {} constraint overrides for staff ID: {}", overrides.size(), staffId);

        List<ConstraintOverrideDTO> results = overrides.stream()
                .peek(override -> override.setStaffId(staffId))
                .map(override -> {
                    if (exists(override.getStaffId(), override.getConstraintId())) {
                        // Find existing and update
                        ConstraintOverride existing = constraintOverrideRepository
                                .findByStaffIdAndConstraintId(override.getStaffId(), override.getConstraintId())
                                .orElseThrow();
                        return update(existing.getId(), override);
                    } else {
                        // Create new
                        return create(override);
                    }
                })
                .toList();

        log.info("Bulk operation completed for staff ID: {}", staffId);
        return results;
    }

    /**
     * Validate constraint override data
     */
    private void validateConstraintOverrideData(ConstraintOverrideDTO dto) {
        if (dto.getStaffId() == null) {
            throw new IllegalArgumentException("Staff ID is required");
        }

        if (dto.getConstraintId() == null) {
            throw new IllegalArgumentException("Constraint ID is required");
        }

        if (dto.getOverrideValue() == null || dto.getOverrideValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Override value is required");
        }

        // Validate staff exists and is active
        Staff staff = staffRepository.findById(dto.getStaffId())
                .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + dto.getStaffId()));

        if (!staff.getActive()) {
            throw new IllegalArgumentException("Cannot create constraint override for inactive staff");
        }

        // Validate constraint exists and is active
        Constraint constraint = constraintRepository.findById(dto.getConstraintId())
                .orElseThrow(() -> new IllegalArgumentException("Constraint not found with ID: " + dto.getConstraintId()));

        if (!constraint.getActive()) {
            throw new IllegalArgumentException("Cannot create override for inactive constraint");
        }

        log.debug("Constraint override validation passed for staff ID: {} and constraint ID: {}", dto.getStaffId(), dto.getConstraintId());
    }
}