package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.StaffDTO;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Department;
import com.rosteroptimization.entity.Squad;
import com.rosteroptimization.entity.Qualification;
import com.rosteroptimization.repository.DepartmentRepository;
import com.rosteroptimization.repository.SquadRepository;
import com.rosteroptimization.repository.QualificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class StaffMapper {

    private final DepartmentRepository departmentRepository;
    private final SquadRepository squadRepository;
    private final QualificationRepository qualificationRepository;
    private final DepartmentMapper departmentMapper;
    private final SquadMapper squadMapper;
    private final QualificationMapper qualificationMapper;
    private final DayOffRuleMapper dayOffRuleMapper;
    private final ConstraintOverrideMapper constraintOverrideMapper;

    /**
     * Convert Entity to DTO
     */
    public StaffDTO toDto(Staff entity) {
        if (entity == null) {
            return null;
        }

        StaffDTO dto = new StaffDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSurname(entity.getSurname());
        dto.setRegistrationCode(entity.getRegistrationCode());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setActive(entity.getActive());

        // Set foreign key references
        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartment(departmentMapper.toDto(entity.getDepartment()));
        }

        if (entity.getSquad() != null) {
            dto.setSquadId(entity.getSquad().getId());
            dto.setSquad(squadMapper.toDto(entity.getSquad()));
        }

        // Set qualification references
        if (entity.getQualifications() != null) {
            dto.setQualificationIds(entity.getQualifications().stream()
                    .map(Qualification::getId)
                    .collect(Collectors.toSet()));
            dto.setQualifications(entity.getQualifications().stream()
                    .map(qualificationMapper::toDto)
                    .collect(Collectors.toSet()));
        }

        // Set one-to-one relationships
        if (entity.getDayOffRule() != null) {
            dto.setDayOffRule(dayOffRuleMapper.toDto(entity.getDayOffRule()));
        }

        // Set one-to-many relationships
        if (entity.getConstraintOverrides() != null) {
            dto.setConstraintOverrides(entity.getConstraintOverrides().stream()
                    .map(constraintOverrideMapper::toDto)
                    .collect(Collectors.toList()));
        }

        // Calculate computed fields
        calculateComputedFields(entity, dto);

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public Staff toEntity(StaffDTO dto) {
        if (dto == null) {
            return null;
        }

        Staff entity = new Staff();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setSurname(dto.getSurname());
        entity.setRegistrationCode(dto.getRegistrationCode());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Set foreign key relationships
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + dto.getDepartmentId()));
            entity.setDepartment(department);
        }

        if (dto.getSquadId() != null) {
            Squad squad = squadRepository.findById(dto.getSquadId())
                    .orElseThrow(() -> new IllegalArgumentException("Squad not found with ID: " + dto.getSquadId()));
            entity.setSquad(squad);
        }

        // Set qualification relationships
        if (dto.getQualificationIds() != null && !dto.getQualificationIds().isEmpty()) {
            Set<Qualification> qualifications = new HashSet<>();
            for (Long qualificationId : dto.getQualificationIds()) {
                Qualification qualification = qualificationRepository.findById(qualificationId)
                        .orElseThrow(() -> new IllegalArgumentException("Qualification not found with ID: " + qualificationId));
                qualifications.add(qualification);
            }
            entity.setQualifications(qualifications);
        }

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(StaffDTO dto, Staff entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setSurname(dto.getSurname());
        entity.setRegistrationCode(dto.getRegistrationCode());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Update foreign key relationships
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + dto.getDepartmentId()));
            entity.setDepartment(department);
        }

        if (dto.getSquadId() != null) {
            Squad squad = squadRepository.findById(dto.getSquadId())
                    .orElseThrow(() -> new IllegalArgumentException("Squad not found with ID: " + dto.getSquadId()));
            entity.setSquad(squad);
        }

        // Update qualification relationships
        if (dto.getQualificationIds() != null) {
            Set<Qualification> qualifications = new HashSet<>();
            for (Long qualificationId : dto.getQualificationIds()) {
                Qualification qualification = qualificationRepository.findById(qualificationId)
                        .orElseThrow(() -> new IllegalArgumentException("Qualification not found with ID: " + qualificationId));
                qualifications.add(qualification);
            }
            entity.setQualifications(qualifications);
        } else {
            entity.setQualifications(new HashSet<>());
        }
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<StaffDTO> toDtoList(List<Staff> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculate computed fields for staff
     */
    private void calculateComputedFields(Staff entity, StaffDTO dto) {
        // Full name
        if (StringUtils.hasText(entity.getName()) && StringUtils.hasText(entity.getSurname())) {
            dto.setFullName(entity.getName() + " " + entity.getSurname());
            dto.setDisplayName(entity.getSurname() + ", " + entity.getName());
        }

        // Qualification count
        if (entity.getQualifications() != null) {
            dto.setQualificationCount(entity.getQualifications().size());
        } else {
            dto.setQualificationCount(0);
        }

        // Boolean flags
        dto.setHasDayOffRule(entity.getDayOffRule() != null);
        dto.setHasConstraintOverrides(entity.getConstraintOverrides() != null && !entity.getConstraintOverrides().isEmpty());

        // Current cycle position from squad
        if (entity.getSquad() != null) {
            dto.setCurrentCyclePosition(squadMapper.getCurrentCyclePosition(entity.getSquad(), LocalDate.now()));
        }
    }

    /**
     * Convert qualification IDs to qualification entities
     */
    public Set<Qualification> mapQualificationIds(Set<Long> qualificationIds) {
        if (qualificationIds == null || qualificationIds.isEmpty()) {
            return new HashSet<>();
        }

        return qualificationIds.stream()
                .map(id -> qualificationRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Qualification not found with ID: " + id)))
                .collect(Collectors.toSet());
    }

    /**
     * Convert qualification entities to IDs
     */
    public Set<Long> mapQualificationEntities(Set<Qualification> qualifications) {
        if (qualifications == null || qualifications.isEmpty()) {
            return new HashSet<>();
        }

        return qualifications.stream()
                .map(Qualification::getId)
                .collect(Collectors.toSet());
    }
}