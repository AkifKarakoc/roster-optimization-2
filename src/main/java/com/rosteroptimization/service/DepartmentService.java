package com.rosteroptimization.service;

import com.rosteroptimization.dto.DepartmentDTO;
import com.rosteroptimization.entity.Department;
import com.rosteroptimization.mapper.DepartmentMapper;
import com.rosteroptimization.repository.DepartmentRepository;
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
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    /**
     * Create new department
     */
    public DepartmentDTO create(DepartmentDTO dto) {
        log.info("Creating new department: {}", dto.getName());

        // Check if department with same name already exists
        Optional<Department> existing = departmentRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Department with name '" + dto.getName() + "' already exists");
        }

        Department entity = departmentMapper.toEntity(dto);
        Department saved = departmentRepository.save(entity);

        log.info("Department created with ID: {}", saved.getId());
        return departmentMapper.toDto(saved);
    }

    /**
     * Update existing department
     */
    public DepartmentDTO update(Long id, DepartmentDTO dto) {
        log.info("Updating department with ID: {}", id);

        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + id));

        // Check if another department with same name exists
        Optional<Department> duplicateName = departmentRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (duplicateName.isPresent() && !duplicateName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Department with name '" + dto.getName() + "' already exists");
        }

        departmentMapper.updateEntityFromDto(dto, existing);
        Department updated = departmentRepository.save(existing);

        log.info("Department updated: {}", updated.getName());
        return departmentMapper.toDto(updated);
    }

    /**
     * Soft delete department
     */
    public void delete(Long id) {
        log.info("Soft deleting department with ID: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + id));

        department.setActive(false);
        departmentRepository.save(department);

        log.info("Department soft deleted: {}", department.getName());
    }

    /**
     * Find department by ID
     */
    @Transactional(readOnly = true)
    public DepartmentDTO findById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + id));

        return departmentMapper.toDto(department);
    }

    /**
     * Find all active departments
     */
    @Transactional(readOnly = true)
    public List<DepartmentDTO> findAll() {
        List<Department> departments = departmentRepository.findByActiveTrueOrderByNameAsc();
        return departmentMapper.toDtoList(departments);
    }

    /**
     * Find all departments (including inactive)
     */
    @Transactional(readOnly = true)
    public List<DepartmentDTO> findAllIncludingInactive() {
        List<Department> departments = departmentRepository.findAllByOrderByNameAsc();
        return departmentMapper.toDtoList(departments);
    }

    /**
     * Search departments by name
     */
    @Transactional(readOnly = true)
    public List<DepartmentDTO> searchByName(String name, boolean includeInactive) {
        List<Department> departments = departmentRepository.searchByNameContaining(name, !includeInactive);
        return departmentMapper.toDtoList(departments);
    }

    /**
     * Get active department count
     */
    @Transactional(readOnly = true)
    public long getActiveCount() {
        return departmentRepository.countByActiveTrue();
    }
}