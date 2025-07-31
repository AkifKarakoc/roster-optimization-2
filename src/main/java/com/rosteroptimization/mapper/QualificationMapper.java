package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.QualificationDTO;
import com.rosteroptimization.entity.Qualification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class QualificationMapper {

    /**
     * Convert Entity to DTO
     */
    public QualificationDTO toDto(Qualification entity) {
        if (entity == null) {
            return null;
        }

        QualificationDTO dto = new QualificationDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());

        // Calculate statistics if relationships are loaded
        if (entity.getStaffSet() != null) {
            dto.setStaffCount((long) entity.getStaffSet().size());
        }
        if (entity.getTaskSet() != null) {
            dto.setTaskCount((long) entity.getTaskSet().size());
        }

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public Qualification toEntity(QualificationDTO dto) {
        if (dto == null) {
            return null;
        }

        Qualification entity = new Qualification();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(QualificationDTO dto, Qualification entity) {
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
    public List<QualificationDTO> toDtoList(List<Qualification> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}