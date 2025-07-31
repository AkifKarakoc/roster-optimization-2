package com.rosteroptimization.service;

import com.rosteroptimization.dto.DayOffRuleDTO;
import com.rosteroptimization.entity.DayOffRule;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.mapper.DayOffRuleMapper;
import com.rosteroptimization.repository.DayOffRuleRepository;
import com.rosteroptimization.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DayOffRuleService {

    private final DayOffRuleRepository dayOffRuleRepository;
    private final StaffRepository staffRepository;
    private final DayOffRuleMapper dayOffRuleMapper;

    /**
     * Create new day off rule
     */
    public DayOffRuleDTO create(DayOffRuleDTO dto) {
        log.info("Creating new day off rule for staff ID: {}", dto.getStaffId());

        // Validate day off rule data
        validateDayOffRuleData(dto);

        // Check if staff already has a day off rule
        if (dayOffRuleRepository.existsByStaffId(dto.getStaffId())) {
            throw new IllegalArgumentException("Staff already has a day off rule. Use update instead.");
        }

        DayOffRule entity = dayOffRuleMapper.toEntity(dto);
        DayOffRule saved = dayOffRuleRepository.save(entity);

        log.info("Day off rule created with ID: {} for staff: {}", saved.getId(), saved.getStaff().getRegistrationCode());
        return dayOffRuleMapper.toDto(saved);
    }

    /**
     * Update existing day off rule
     */
    public DayOffRuleDTO update(Long id, DayOffRuleDTO dto) {
        log.info("Updating day off rule with ID: {}", id);

        // Validate day off rule data
        validateDayOffRuleData(dto);

        DayOffRule existing = dayOffRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Day off rule not found with ID: " + id));

        // Check if trying to change staff and new staff already has a rule
        if (!existing.getStaff().getId().equals(dto.getStaffId())) {
            if (dayOffRuleRepository.existsByStaffId(dto.getStaffId())) {
                throw new IllegalArgumentException("Target staff already has a day off rule");
            }
        }

        dayOffRuleMapper.updateEntityFromDto(dto, existing);
        DayOffRule updated = dayOffRuleRepository.save(existing);

        log.info("Day off rule updated: {} for staff: {}", updated.getId(), updated.getStaff().getRegistrationCode());
        return dayOffRuleMapper.toDto(updated);
    }

    /**
     * Delete day off rule
     */
    public void delete(Long id) {
        log.info("Deleting day off rule with ID: {}", id);

        DayOffRule dayOffRule = dayOffRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Day off rule not found with ID: " + id));

        dayOffRuleRepository.delete(dayOffRule);

        log.info("Day off rule deleted: {} for staff: {}", id, dayOffRule.getStaff().getRegistrationCode());
    }

    /**
     * Find day off rule by ID
     */
    @Transactional(readOnly = true)
    public DayOffRuleDTO findById(Long id) {
        DayOffRule dayOffRule = dayOffRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Day off rule not found with ID: " + id));

        return dayOffRuleMapper.toDto(dayOffRule);
    }

    /**
     * Find all day off rules
     */
    @Transactional(readOnly = true)
    public List<DayOffRuleDTO> findAll() {
        List<DayOffRule> dayOffRules = dayOffRuleRepository.findAll();
        return dayOffRuleMapper.toDtoList(dayOffRules);
    }

    /**
     * Find day off rule by staff ID
     */
    @Transactional(readOnly = true)
    public DayOffRuleDTO findByStaffId(Long staffId) {
        DayOffRule dayOffRule = dayOffRuleRepository.findByStaffId(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Day off rule not found for staff ID: " + staffId));

        return dayOffRuleMapper.toDto(dayOffRule);
    }

    /**
     * Find day off rule by staff registration code
     */
    @Transactional(readOnly = true)
    public DayOffRuleDTO findByStaffRegistrationCode(String registrationCode) {
        DayOffRule dayOffRule = dayOffRuleRepository.findByStaffRegistrationCode(registrationCode)
                .orElseThrow(() -> new IllegalArgumentException("Day off rule not found for staff registration code: " + registrationCode));

        return dayOffRuleMapper.toDto(dayOffRule);
    }

    /**
     * Find day off rules by working days
     */
    @Transactional(readOnly = true)
    public List<DayOffRuleDTO> findByWorkingDays(Integer workingDays) {
        List<DayOffRule> dayOffRules = dayOffRuleRepository.findByWorkingDays(workingDays);
        return dayOffRuleMapper.toDtoList(dayOffRules);
    }

    /**
     * Find day off rules by off days
     */
    @Transactional(readOnly = true)
    public List<DayOffRuleDTO> findByOffDays(Integer offDays) {
        List<DayOffRule> dayOffRules = dayOffRuleRepository.findByOffDays(offDays);
        return dayOffRuleMapper.toDtoList(dayOffRules);
    }

    /**
     * Find day off rules with fixed off days
     */
    @Transactional(readOnly = true)
    public List<DayOffRuleDTO> findRulesWithFixedOffDays() {
        List<DayOffRule> dayOffRules = dayOffRuleRepository.findRulesWithFixedOffDays();
        return dayOffRuleMapper.toDtoList(dayOffRules);
    }

    /**
     * Find day off rules without fixed off days (flexible rules)
     */
    @Transactional(readOnly = true)
    public List<DayOffRuleDTO> findFlexibleRules() {
        List<DayOffRule> dayOffRules = dayOffRuleRepository.findRulesWithoutFixedOffDays();
        return dayOffRuleMapper.toDtoList(dayOffRules);
    }

    /**
     * Find day off rules by specific fixed off day
     */
    @Transactional(readOnly = true)
    public List<DayOffRuleDTO> findByFixedOffDay(String dayOfWeek) {
        validateDayOfWeek(dayOfWeek);
        List<DayOffRule> dayOffRules = dayOffRuleRepository.findByFixedOffDayContaining(dayOfWeek.toUpperCase());
        return dayOffRuleMapper.toDtoList(dayOffRules);
    }

    /**
     * Find day off rules by department
     */
    @Transactional(readOnly = true)
    public List<DayOffRuleDTO> findByDepartment(Long departmentId) {
        List<DayOffRule> dayOffRules = dayOffRuleRepository.findByStaffDepartmentId(departmentId);
        return dayOffRuleMapper.toDtoList(dayOffRules);
    }

    /**
     * Find day off rules by squad
     */
    @Transactional(readOnly = true)
    public List<DayOffRuleDTO> findBySquad(Long squadId) {
        List<DayOffRule> dayOffRules = dayOffRuleRepository.findByStaffSquadId(squadId);
        return dayOffRuleMapper.toDtoList(dayOffRules);
    }

    /**
     * Get total day off rule count
     */
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return dayOffRuleRepository.count();
    }

    /**
     * Get count of rules with fixed off days
     */
    @Transactional(readOnly = true)
    public long getFixedRulesCount() {
        return dayOffRuleRepository.countRulesWithFixedOffDays();
    }

    /**
     * Get count of flexible rules (without fixed off days)
     */
    @Transactional(readOnly = true)
    public long getFlexibleRulesCount() {
        return getTotalCount() - getFixedRulesCount();
    }

    /**
     * Check if staff has day off rule
     */
    @Transactional(readOnly = true)
    public boolean hasRule(Long staffId) {
        return dayOffRuleRepository.existsByStaffId(staffId);
    }

    /**
     * Get optional day off rule by staff ID
     */
    @Transactional(readOnly = true)
    public Optional<DayOffRuleDTO> findOptionalByStaffId(Long staffId) {
        Optional<DayOffRule> dayOffRule = dayOffRuleRepository.findByStaffId(staffId);
        return dayOffRule.map(dayOffRuleMapper::toDto);
    }

    /**
     * Validate day off rule data
     */
    private void validateDayOffRuleData(DayOffRuleDTO dto) {
        if (dto.getWorkingDays() == null || dto.getWorkingDays() < 1 || dto.getWorkingDays() > 14) {
            throw new IllegalArgumentException("Working days must be between 1 and 14");
        }

        if (dto.getOffDays() == null || dto.getOffDays() < 1 || dto.getOffDays() > 7) {
            throw new IllegalArgumentException("Off days must be between 1 and 7");
        }

        // Validate staff exists and is active
        if (dto.getStaffId() != null) {
            Staff staff = staffRepository.findById(dto.getStaffId())
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + dto.getStaffId()));

            if (!staff.getActive()) {
                throw new IllegalArgumentException("Cannot create day off rule for inactive staff");
            }
        }

        // Validate fixed off days format
        if (StringUtils.hasText(dto.getFixedOffDays())) {
            if (!dayOffRuleMapper.isValidFixedOffDays(dto.getFixedOffDays())) {
                throw new IllegalArgumentException("Invalid fixed off days format. Use comma-separated day names (e.g., MONDAY,TUESDAY)");
            }
        }

        log.debug("Day off rule validation passed for staff ID: {}", dto.getStaffId());
    }

    /**
     * Validate day of week
     */
    private void validateDayOfWeek(String dayOfWeek) {
        if (!StringUtils.hasText(dayOfWeek)) {
            throw new IllegalArgumentException("Day of week is required");
        }

        String[] validDays = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        String upperDay = dayOfWeek.toUpperCase();

        for (String validDay : validDays) {
            if (validDay.equals(upperDay)) {
                return;
            }
        }

        throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek + ". Valid values: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY");
    }
}