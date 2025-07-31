package com.rosteroptimization.service;

import com.rosteroptimization.dto.QualificationDTO;
import com.rosteroptimization.entity.Qualification;
import com.rosteroptimization.mapper.QualificationMapper;
import com.rosteroptimization.repository.QualificationRepository;
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
public class QualificationService {

    private final QualificationRepository qualificationRepository;
    private final QualificationMapper qualificationMapper;

    /**
     * Create new qualification
     */
    public QualificationDTO create(QualificationDTO dto) {
        log.info("Creating new qualification: {}", dto.getName());

        // Check if qualification with same name already exists
        Optional<Qualification> existing = qualificationRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Qualification with name '" + dto.getName() + "' already exists");
        }

        Qualification entity = qualificationMapper.toEntity(dto);
        Qualification saved = qualificationRepository.save(entity);

        log.info("Qualification created with ID: {}", saved.getId());
        return qualificationMapper.toDto(saved);
    }

    /**
     * Update existing qualification
     */
    public QualificationDTO update(Long id, QualificationDTO dto) {
        log.info("Updating qualification with ID: {}", id);

        Qualification existing = qualificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Qualification not found with ID: " + id));

        // Check if another qualification with same name exists
        Optional<Qualification> duplicateName = qualificationRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (duplicateName.isPresent() && !duplicateName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Qualification with name '" + dto.getName() + "' already exists");
        }

        qualificationMapper.updateEntityFromDto(dto, existing);
        Qualification updated = qualificationRepository.save(existing);

        log.info("Qualification updated: {}", updated.getName());
        return qualificationMapper.toDto(updated);
    }

    /**
     * Soft delete qualification
     */
    public void delete(Long id) {
        log.info("Soft deleting qualification with ID: {}", id);

        Qualification qualification = qualificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Qualification not found with ID: " + id));

        qualification.setActive(false);
        qualificationRepository.save(qualification);

        log.info("Qualification soft deleted: {}", qualification.getName());
    }

    /**
     * Find qualification by ID
     */
    @Transactional(readOnly = true)
    public QualificationDTO findById(Long id) {
        Qualification qualification = qualificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Qualification not found with ID: " + id));

        return qualificationMapper.toDto(qualification);
    }

    /**
     * Find all active qualifications
     */
    @Transactional(readOnly = true)
    public List<QualificationDTO> findAll() {
        List<Qualification> qualifications = qualificationRepository.findByActiveTrueOrderByNameAsc();
        return qualificationMapper.toDtoList(qualifications);
    }

    /**
     * Find all qualifications (including inactive)
     */
    @Transactional(readOnly = true)
    public List<QualificationDTO> findAllIncludingInactive() {
        List<Qualification> qualifications = qualificationRepository.findAllByOrderByNameAsc();
        return qualificationMapper.toDtoList(qualifications);
    }

    /**
     * Search qualifications by name
     */
    @Transactional(readOnly = true)
    public List<QualificationDTO> searchByName(String name, boolean includeInactive) {
        List<Qualification> qualifications = qualificationRepository.searchByNameContaining(name, !includeInactive);
        return qualificationMapper.toDtoList(qualifications);
    }

    /**
     * Get active qualification count
     */
    @Transactional(readOnly = true)
    public long getActiveCount() {
        return qualificationRepository.countByActiveTrue();
    }

    /**
     * Find qualifications by IDs (for bulk operations)
     */
    @Transactional(readOnly = true)
    public List<QualificationDTO> findByIds(List<Long> ids) {
        List<Qualification> qualifications = qualificationRepository.findByIdInAndActiveTrue(ids);
        return qualificationMapper.toDtoList(qualifications);
    }
}