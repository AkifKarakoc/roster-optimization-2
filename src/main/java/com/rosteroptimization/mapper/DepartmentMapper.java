package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.DepartmentDTO;
import com.rosteroptimization.entity.Department;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DepartmentMapper {

    /**
     * Convert Entity to DTO
     */
    public DepartmentDTO toDto(Department entity) {
        if (entity == null) {
            return null;
        }

        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());

        // Calculate statistics if relationships are loaded
        if (entity.getStaffList() != null) {
            dto.setStaffCount((long) entity.getStaffList().size());
        }
        if (entity.getTaskList() != null) {
            dto.setTaskCount((long) entity.getTaskList().size());
        }

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public Department toEntity(DepartmentDTO dto) {
        if (dto == null) {
            return null;
        }

        Department entity = new Department();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(DepartmentDTO dto, Department entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<DepartmentDTO> toDtoList(List<Department> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}