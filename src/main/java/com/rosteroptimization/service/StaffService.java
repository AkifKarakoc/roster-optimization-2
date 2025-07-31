package com.rosteroptimization.service;

import com.rosteroptimization.dto.StaffDTO;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Department;
import com.rosteroptimization.entity.Squad;
import com.rosteroptimization.mapper.StaffMapper;
import com.rosteroptimization.repository.StaffRepository;
import com.rosteroptimization.repository.DepartmentRepository;
import com.rosteroptimization.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StaffService {

    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;
    private final SquadRepository squadRepository;
    private final StaffMapper staffMapper;

    /**
     * Create new staff
     */
    public StaffDTO create(StaffDTO dto) {
        log.info("Creating new staff: {} {}", dto.getName(), dto.getSurname());

        // Validate staff data
        validateStaffData(dto, null);

        Staff entity = staffMapper.toEntity(dto);
        Staff saved = staffRepository.save(entity);

        log.info("Staff created with ID: {} ({})", saved.getId(), saved.getRegistrationCode());
        return staffMapper.toDto(saved);
    }

    /**
     * Update existing staff
     */
    public StaffDTO update(Long id, StaffDTO dto) {
        log.info("Updating staff with ID: {}", id);

        Staff existing = staffRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + id));

        // Validate staff data with current ID
        validateStaffData(dto, id);

        staffMapper.updateEntityFromDto(dto, existing);
        Staff updated = staffRepository.save(existing);

        log.info("Staff updated: {} {} ({})", updated.getName(), updated.getSurname(), updated.getRegistrationCode());
        return staffMapper.toDto(updated);
    }

    /**
     * Soft delete staff
     */
    public void delete(Long id) {
        log.info("Soft deleting staff with ID: {}", id);

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + id));

        staff.setActive(false);
        staffRepository.save(staff);

        log.info("Staff soft deleted: {} {} ({})", staff.getName(), staff.getSurname(), staff.getRegistrationCode());
    }

    /**
     * Find staff by ID
     */
    @Transactional(readOnly = true)
    public StaffDTO findById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + id));

        return staffMapper.toDto(staff);
    }

    /**
     * Find all active staff
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findAll() {
        List<Staff> staffList = staffRepository.findByActiveTrueOrderBySurnameAscNameAsc();
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find all staff (including inactive)
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findAllIncludingInactive() {
        List<Staff> staffList = staffRepository.findAllByOrderBySurnameAscNameAsc();
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Search staff by name or surname
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> searchByName(String searchTerm, boolean includeInactive) {
        List<Staff> staffList = staffRepository.searchByNameOrSurnameContaining(searchTerm, !includeInactive);
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find staff by department
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findByDepartment(Long departmentId) {
        List<Staff> staffList = staffRepository.findByDepartmentIdAndActiveTrue(departmentId);
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find staff by squad
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findBySquad(Long squadId) {
        List<Staff> staffList = staffRepository.findBySquadIdAndActiveTrue(squadId);
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find staff by qualification
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findByQualification(Long qualificationId) {
        List<Staff> staffList = staffRepository.findByQualificationId(qualificationId);
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find staff with all specified qualifications
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findByAllQualifications(List<Long> qualificationIds) {
        if (qualificationIds == null || qualificationIds.isEmpty()) {
            return List.of();
        }
        List<Staff> staffList = staffRepository.findByAllQualifications(qualificationIds, qualificationIds.size());
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find staff with any of the specified qualifications
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findByAnyQualification(List<Long> qualificationIds) {
        if (qualificationIds == null || qualificationIds.isEmpty()) {
            return List.of();
        }
        List<Staff> staffList = staffRepository.findByAnyQualification(qualificationIds);
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find staff without qualifications
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findStaffWithoutQualifications() {
        List<Staff> staffList = staffRepository.findStaffWithoutQualifications();
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find staff with day off rules
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findStaffWithDayOffRules() {
        List<Staff> staffList = staffRepository.findStaffWithDayOffRules();
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find staff with constraint overrides
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findStaffWithConstraintOverrides() {
        List<Staff> staffList = staffRepository.findStaffWithConstraintOverrides();
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Find staff by registration code
     */
    @Transactional(readOnly = true)
    public StaffDTO findByRegistrationCode(String registrationCode) {
        Staff staff = staffRepository.findByRegistrationCodeAndActiveTrue(registrationCode)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found with registration code: " + registrationCode));

        return staffMapper.toDto(staff);
    }

    /**
     * Find staff by email
     */
    @Transactional(readOnly = true)
    public StaffDTO findByEmail(String email) {
        Staff staff = staffRepository.findByEmailIgnoreCaseAndActiveTrue(email)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found with email: " + email));

        return staffMapper.toDto(staff);
    }

    /**
     * Get active staff count
     */
    @Transactional(readOnly = true)
    public long getActiveCount() {
        return staffRepository.countByActiveTrue();
    }

    /**
     * Get staff count by department
     */
    @Transactional(readOnly = true)
    public long getCountByDepartment(Long departmentId) {
        return staffRepository.countByDepartmentIdAndActiveTrue(departmentId);
    }

    /**
     * Get staff count by squad
     */
    @Transactional(readOnly = true)
    public long getCountBySquad(Long squadId) {
        return staffRepository.countBySquadIdAndActiveTrue(squadId);
    }

    /**
     * Find staff by IDs (for bulk operations)
     */
    @Transactional(readOnly = true)
    public List<StaffDTO> findByIds(List<Long> ids) {
        List<Staff> staffList = staffRepository.findByIdInAndActiveTrue(ids);
        return staffMapper.toDtoList(staffList);
    }

    /**
     * Validate staff data
     */
    private void validateStaffData(StaffDTO dto, Long existingId) {
        // Check registration code uniqueness
        if (StringUtils.hasText(dto.getRegistrationCode())) {
            if (existingId == null) {
                // Creating new staff
                Optional<Staff> existingByRegCode = staffRepository.findByRegistrationCodeAndActiveTrue(dto.getRegistrationCode());
                if (existingByRegCode.isPresent()) {
                    throw new IllegalArgumentException("Registration code '" + dto.getRegistrationCode() + "' already exists");
                }
            } else {
                // Updating existing staff
                boolean regCodeExists = staffRepository.existsByRegistrationCodeAndIdNotAndActiveTrue(dto.getRegistrationCode(), existingId);
                if (regCodeExists) {
                    throw new IllegalArgumentException("Registration code '" + dto.getRegistrationCode() + "' already exists");
                }
            }
        }

        // Check email uniqueness (if provided)
        if (StringUtils.hasText(dto.getEmail())) {
            if (existingId == null) {
                // Creating new staff
                Optional<Staff> existingByEmail = staffRepository.findByEmailIgnoreCaseAndActiveTrue(dto.getEmail());
                if (existingByEmail.isPresent()) {
                    throw new IllegalArgumentException("Email '" + dto.getEmail() + "' already exists");
                }
            } else {
                // Updating existing staff
                boolean emailExists = staffRepository.existsByEmailIgnoreCaseAndIdNotAndActiveTrue(dto.getEmail(), existingId);
                if (emailExists) {
                    throw new IllegalArgumentException("Email '" + dto.getEmail() + "' already exists");
                }
            }
        }

        // Validate department exists and is active
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + dto.getDepartmentId()));

            if (!department.getActive()) {
                throw new IllegalArgumentException("Cannot assign staff to inactive department");
            }
        }

        // Validate squad exists and is active
        if (dto.getSquadId() != null) {
            Squad squad = squadRepository.findById(dto.getSquadId())
                    .orElseThrow(() -> new IllegalArgumentException("Squad not found with ID: " + dto.getSquadId()));

            if (!squad.getActive()) {
                throw new IllegalArgumentException("Cannot assign staff to inactive squad");
            }
        }

        // Validate qualifications exist and are active (if provided)
        if (dto.getQualificationIds() != null && !dto.getQualificationIds().isEmpty()) {
            for (Long qualificationId : dto.getQualificationIds()) {
                // This validation will be done in the mapper when converting to entity
                // We could add explicit validation here if needed
            }
        }

        log.debug("Staff validation passed for: {} {} ({})", dto.getName(), dto.getSurname(), dto.getRegistrationCode());
    }
}