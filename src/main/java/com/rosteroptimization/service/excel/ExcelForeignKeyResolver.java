package com.rosteroptimization.service.excel;

import com.rosteroptimization.service.excel.dto.EntityOperation;
import com.rosteroptimization.service.excel.dto.ExcelRowDTO;
import com.rosteroptimization.service.excel.dto.ParsedEntityId;
import com.rosteroptimization.service.excel.ExcelEntityType;
import com.rosteroptimization.dto.*;
import com.rosteroptimization.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Service for resolving foreign key references in Excel import
 * Handles both ID-based and name-based lookups with caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelForeignKeyResolver {

    private final DepartmentService departmentService;
    private final QualificationService qualificationService;
    private final WorkingPeriodService workingPeriodService;
    private final SquadWorkingPatternService squadWorkingPatternService;
    private final SquadService squadService;
    private final ShiftService shiftService;
    private final StaffService staffService;
    private final TaskService taskService;

    // Entity caches for performance
    private final Map<String, DepartmentDTO> departmentCache = new HashMap<>();
    private final Map<String, QualificationDTO> qualificationCache = new HashMap<>();
    private final Map<String, WorkingPeriodDTO> workingPeriodCache = new HashMap<>();
    private final Map<String, SquadWorkingPatternDTO> squadWorkingPatternCache = new HashMap<>();
    private final Map<String, SquadDTO> squadCache = new HashMap<>();
    private final Map<String, ShiftDTO> shiftCache = new HashMap<>();
    private final Map<String, StaffDTO> staffCache = new HashMap<>();
    private final Map<String, TaskDTO> taskCache = new HashMap<>();

    // Batch validation sırasında accumulate edilen entity'ler
    private Map<String, Set<String>> batchEntities = new HashMap<>();

    /**
     * Clear all caches
     */
    public void clearCache() {
        departmentCache.clear();
        qualificationCache.clear();
        workingPeriodCache.clear();
        squadWorkingPatternCache.clear();
        squadCache.clear();
        shiftCache.clear();
        staffCache.clear();
        taskCache.clear();
    }

    /**
     * Clear all caches (alternative method name for compatibility)
     */
    public void clearCaches() {
        clearCache();
    }

    /**
     * Warm up caches by preloading frequently accessed entities
     */
    public void warmUpCaches() {
        log.debug("Warming up caches for foreign key resolution");
        // This is a placeholder method - can be implemented later for performance optimization
    }

    /**
     * Batch validation başladığında cache'i temizle
     */
    public void startBatchValidation() {
        batchEntities.clear();
    }

    /**
     * Validation sırasında ADD operasyonu görüldüğünde cache'e ekle
     */
    public void addToBatch(ExcelEntityType entityType, ExcelRowDTO row) {
        EntityOperation operation = row.getOperation();
        if (operation == null) {
            operation = row.getOperationFromCell();
        }
        
        if (operation != EntityOperation.ADD) {
            return;
        }

        String entityKey = entityType.name();
        batchEntities.computeIfAbsent(entityKey, k -> new HashSet<>());

        // Ana entity ID'yi parse et ve ekle (Squad_1 -> 1)
        ParsedEntityId mainEntityId = row.parseMainEntityId();
        
        if (mainEntityId != null && mainEntityId.hasId()) {
            addIfPresent(entityKey, mainEntityId.getId().toString());
        }
        if (mainEntityId != null && mainEntityId.hasName()) {
            addIfPresent(entityKey, mainEntityId.getName());
        }

        // Entity type'a göre diğer identifier field'ları da ekle
        switch (entityType) {
            case DEPARTMENT:
                addIfPresent(entityKey, row.getCellValue("NAME"));
                // Department ID kolonu "Department_1" formatında olabilir, name kısmını da ekle
                String departmentIdValue = row.getCellValue("DEPARTMENT ID");
                if (StringUtils.hasText(departmentIdValue)) {
                    ParsedEntityId parsedDeptId = ParsedEntityId.parse(departmentIdValue);
                    if (parsedDeptId.hasName()) {
                        addIfPresent(entityKey, parsedDeptId.getName());
                    }
                    if (parsedDeptId.hasId()) {
                        addIfPresent(entityKey, parsedDeptId.getId().toString());
                    }
                }
                break;

            case STAFF:
                addIfPresent(entityKey, row.getCellValue("REGISTRATION CODE"));
                String firstName = row.getCellValue("FIRST NAME");
                String lastName = row.getCellValue("LAST NAME");
                if (StringUtils.hasText(firstName) && StringUtils.hasText(lastName)) {
                    addIfPresent(entityKey, firstName + " " + lastName);
                }
                // Staff ID kolonu da parse edilmeli
                String staffIdValue = row.getCellValue("STAFF ID");
                if (StringUtils.hasText(staffIdValue)) {
                    ParsedEntityId parsedStaffId = ParsedEntityId.parse(staffIdValue);
                    if (parsedStaffId.hasName()) {
                        addIfPresent(entityKey, parsedStaffId.getName());
                    }
                    if (parsedStaffId.hasId()) {
                        addIfPresent(entityKey, parsedStaffId.getId().toString());
                    }
                }
                break;

            case SQUAD:
                addIfPresent(entityKey, row.getCellValue("NAME"));
                // Squad ID kolonu da parse edilmeli
                String squadIdValue = row.getCellValue("SQUAD ID");
                if (StringUtils.hasText(squadIdValue)) {
                    ParsedEntityId parsedSquadId = ParsedEntityId.parse(squadIdValue);
                    if (parsedSquadId.hasName()) {
                        addIfPresent(entityKey, parsedSquadId.getName());
                    }
                    if (parsedSquadId.hasId()) {
                        addIfPresent(entityKey, parsedSquadId.getId().toString());
                    }
                }
                break;

            case QUALIFICATION:
                addIfPresent(entityKey, row.getCellValue("NAME"));
                // Qualification ID kolonu da parse edilmeli
                String qualIdValue = row.getCellValue("QUALIFICATION ID");
                if (StringUtils.hasText(qualIdValue)) {
                    ParsedEntityId parsedQualId = ParsedEntityId.parse(qualIdValue);
                    if (parsedQualId.hasName()) {
                        addIfPresent(entityKey, parsedQualId.getName());
                    }
                    if (parsedQualId.hasId()) {
                        addIfPresent(entityKey, parsedQualId.getId().toString());
                    }
                }
                break;

            case WORKING_PERIOD:
                addIfPresent(entityKey, row.getCellValue("NAME"));
                // Period ID kolonu da parse edilmeli
                String periodIdValue = row.getCellValue("PERIOD ID");
                if (StringUtils.hasText(periodIdValue)) {
                    ParsedEntityId parsedPeriodId = ParsedEntityId.parse(periodIdValue);
                    if (parsedPeriodId.hasName()) {
                        addIfPresent(entityKey, parsedPeriodId.getName());
                    }
                    if (parsedPeriodId.hasId()) {
                        addIfPresent(entityKey, parsedPeriodId.getId().toString());
                    }
                }
                break;

            case SHIFT:
                addIfPresent(entityKey, row.getCellValue("NAME"));
                // Shift ID kolonu da parse edilmeli
                String shiftIdValue = row.getCellValue("SHIFT ID");
                if (StringUtils.hasText(shiftIdValue)) {
                    ParsedEntityId parsedShiftId = ParsedEntityId.parse(shiftIdValue);
                    if (parsedShiftId.hasName()) {
                        addIfPresent(entityKey, parsedShiftId.getName());
                    }
                    if (parsedShiftId.hasId()) {
                        addIfPresent(entityKey, parsedShiftId.getId().toString());
                    }
                }
                break;

            case SQUAD_WORKING_PATTERN:
                addIfPresent(entityKey, row.getCellValue("NAME"));
                // Pattern ID kolonu da parse edilmeli
                String patternIdValue = row.getCellValue("PATTERN ID");
                if (StringUtils.hasText(patternIdValue)) {
                    ParsedEntityId parsedPatternId = ParsedEntityId.parse(patternIdValue);
                    if (parsedPatternId.hasName()) {
                        addIfPresent(entityKey, parsedPatternId.getName());
                    }
                    if (parsedPatternId.hasId()) {
                        addIfPresent(entityKey, parsedPatternId.getId().toString());
                    }
                }
                break;

            case TASK:
                addIfPresent(entityKey, row.getCellValue("NAME"));
                // Task ID kolonu da parse edilmeli
                String taskIdValue = row.getCellValue("TASK ID");
                if (StringUtils.hasText(taskIdValue)) {
                    ParsedEntityId parsedTaskId = ParsedEntityId.parse(taskIdValue);
                    if (parsedTaskId.hasName()) {
                        addIfPresent(entityKey, parsedTaskId.getName());
                    }
                    if (parsedTaskId.hasId()) {
                        addIfPresent(entityKey, parsedTaskId.getId().toString());
                    }
                }
                break;

            case CONSTRAINT_OVERRIDE:
                // No additional fields to cache for constraint overrides
                break;

            case DAY_OFF_RULE:
                // No additional fields to cache for day off rules
                break;
        }
    }

    private void addIfPresent(String entityKey, String value) {
        if (StringUtils.hasText(value)) {
            String normalizedValue = value.trim().toLowerCase();
            batchEntities.get(entityKey).add(normalizedValue);
        }
    }

    /**
     * Batch'te var mı kontrol et
     */
    private boolean isInBatch(String entityType, String value) {
        if (!StringUtils.hasText(value)) return false;

        Set<String> entities = batchEntities.get(entityType);
        if (entities == null) return false;
        
        String searchValue = value.trim().toLowerCase();
        
        // Direct match
        if (entities.contains(searchValue)) {
            return true;
        }
        
        // ID-based match - sadece sayısal değerler için
        if (searchValue.matches("\\d+")) {
            for (String entity : entities) {
                if (entity.contains("_") && entity.endsWith("_" + searchValue)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Resolve Department ID from value (ID or name)
     */
    public Long resolveDepartmentId(String value) {
        if (!StringUtils.hasText(value)) return null;

        ParsedEntityId parsed = ParsedEntityId.parse(value);

        // Eğer name_id formatı varsa (örneğin "Department_1"), önce name kontrolü yap
        if (parsed.hasName()) {
            // Batch'te aynı isimde department var mı kontrol et
            if (isInBatch("DEPARTMENT", parsed.getName())) {
                return -1L; // Geçici ID - batch'te eklenecek
            }

            // Name-based lookup with cache
            String key = parsed.getName().toLowerCase();
            DepartmentDTO dept = departmentCache.get(key);

            if (dept == null) {
                dept = departmentService.searchByName(parsed.getName(), false)
                        .stream()
                        .filter(d -> d.getName().equalsIgnoreCase(parsed.getName()))
                        .findFirst()
                        .orElse(null);
                if (dept != null) {
                    departmentCache.put(key, dept);
                }
            }

            // Eğer department ismiyle bulundu ve aktifse ID'sini döndür
            if (dept != null) {
                if (!dept.getActive()) {
                    throw new IllegalArgumentException("Department is inactive: " + parsed.getName());
                }
                return dept.getId();
            }

            // Department ismiyle bulunamadı, eğer ID varsa da kontrol et
            if (parsed.hasId()) {
                // Batch'te ID var mı kontrol et
                if (isInBatch("DEPARTMENT", parsed.getId().toString())) {
                    return -1L; // Geçici ID
                }

                // Direct ID lookup - ama önce name'e göre bulunamadığı için muhtemelen bu da olmaz
                try {
                    DepartmentDTO deptById = departmentService.findById(parsed.getId());
                    if (!deptById.getActive()) {
                        throw new IllegalArgumentException("Department is inactive: " + value);
                    }
                    return deptById.getId();
                } catch (Exception e) {
                    // Ne name ne de ID ile bulunamadı - bu batch'te eklenecek olabilir
                    // Eğer batch'te varsa sorun yok, yoksa hata ver
                    if (isInBatch("DEPARTMENT", parsed.getName()) || 
                        (parsed.hasId() && isInBatch("DEPARTMENT", parsed.getId().toString()))) {
                        return -1L; // Geçici ID - bu batch'te eklenecek
                    }
                    throw new IllegalArgumentException("Department not found: " + parsed.getName() + ". Make sure it exists in the database or is being created in this import.");
                }
            } else {
                // Sadece name var ama bulunamadı - bu batch'te eklenecek olabilir
                // Eğer batch'te varsa sorun yok, yoksa hata ver
                if (isInBatch("DEPARTMENT", parsed.getName())) {
                    return -1L; // Geçici ID - bu batch'te eklenecek
                }
                throw new IllegalArgumentException("Department not found: " + parsed.getName() + ". Make sure it exists in the database or is being created in this import.");
            }
        } else if (parsed.hasId()) {
            // Sadece ID var
            if (isInBatch("DEPARTMENT", parsed.getId().toString())) {
                return -1L; // Geçici ID
            }

            // Direct ID lookup
            try {
                DepartmentDTO dept = departmentService.findById(parsed.getId());
                if (!dept.getActive()) {
                    throw new IllegalArgumentException("Department is inactive: " + value);
                }
                return dept.getId();
            } catch (Exception e) {
                throw new IllegalArgumentException("Department not found with ID: " + parsed.getId() + ". Make sure it exists in the database or is being created in this import.");
            }
        }

        throw new IllegalArgumentException("Invalid department reference: " + value);
    }

    /**
     * Resolve Qualification ID from value (ID or name)
     */
    public Long resolveQualificationId(String value) {
        if (!StringUtils.hasText(value)) return null;

        ParsedEntityId parsed = ParsedEntityId.parse(value);

        if (parsed.hasId()) {
            // Batch'te var mı kontrol et
            if (isInBatch("QUALIFICATION", parsed.getId().toString())) {
                return -1L; // Geçici ID
            }

            try {
                QualificationDTO qual = qualificationService.findById(parsed.getId());
                if (!qual.getActive()) {
                    throw new IllegalArgumentException("Qualification is inactive: " + value);
                }
                return qual.getId();
            } catch (Exception e) {
                throw new IllegalArgumentException("Qualification not found with ID: " + parsed.getId() + ". Make sure it exists in the database or is being created in this import.");
            }
        } else if (parsed.hasName()) {
            // Batch'te var mı kontrol et
            if (isInBatch("QUALIFICATION", parsed.getName())) {
                return -1L; // Geçici ID
            }

            String key = parsed.getName().toLowerCase();
            QualificationDTO qual = qualificationCache.get(key);

            if (qual == null) {
                qual = qualificationService.searchByName(parsed.getName(), false)
                        .stream()
                        .filter(q -> q.getName().equalsIgnoreCase(parsed.getName()))
                        .findFirst()
                        .orElse(null);
                if (qual != null) {
                    qualificationCache.put(key, qual);
                } else {
                    throw new IllegalArgumentException("Qualification not found: " + parsed.getName());
                }
            }

            if (!qual.getActive()) {
                throw new IllegalArgumentException("Qualification is inactive: " + parsed.getName());
            }
            return qual.getId();
        }

        throw new IllegalArgumentException("Invalid qualification reference: " + value);
    }

    /**
     * Resolve Working Period ID from value (ID or name)
     */
    public Long resolveWorkingPeriodId(String value) {
        if (!StringUtils.hasText(value)) return null;

        ParsedEntityId parsed = ParsedEntityId.parse(value);

        if (parsed.hasId()) {
            // Batch'te var mı kontrol et
            if (isInBatch("WORKING_PERIOD", parsed.getId().toString())) {
                return -1L; // Geçici ID
            }

            try {
                WorkingPeriodDTO period = workingPeriodService.findById(parsed.getId());
                if (!period.getActive()) {
                    throw new IllegalArgumentException("Working period is inactive: " + value);
                }
                return period.getId();
            } catch (Exception e) {
                throw new IllegalArgumentException("Working period not found with ID: " + parsed.getId() + ". Make sure it exists in the database or is being created in this import.");
            }
        } else if (parsed.hasName()) {
            // Batch'te var mı kontrol et
            if (isInBatch("WORKING_PERIOD", parsed.getName())) {
                return -1L; // Geçici ID
            }

            String key = parsed.getName().toLowerCase();
            WorkingPeriodDTO period = workingPeriodCache.get(key);

            if (period == null) {
                period = workingPeriodService.searchByName(parsed.getName(), false)
                        .stream()
                        .filter(p -> p.getName().equalsIgnoreCase(parsed.getName()))
                        .findFirst()
                        .orElse(null);
                if (period != null) {
                    workingPeriodCache.put(key, period);
                } else {
                    throw new IllegalArgumentException("Working period not found: " + parsed.getName());
                }
            }

            if (!period.getActive()) {
                throw new IllegalArgumentException("Working period is inactive: " + parsed.getName());
            }
            return period.getId();
        }

        throw new IllegalArgumentException("Invalid working period reference: " + value);
    }

    /**
     * Resolve Squad Working Pattern ID from value (ID or name)
     */
    public Long resolveSquadWorkingPatternId(String value) {
        if (!StringUtils.hasText(value)) return null;

        ParsedEntityId parsed = ParsedEntityId.parse(value);

        if (parsed.hasId()) {
            // Batch'te var mı kontrol et
            if (isInBatch("SQUAD_WORKING_PATTERN", parsed.getId().toString())) {
                return -1L; // Geçici ID
            }

            try {
                SquadWorkingPatternDTO pattern = squadWorkingPatternService.findById(parsed.getId());
                if (!pattern.getActive()) {
                    throw new IllegalArgumentException("Squad working pattern is inactive: " + value);
                }
                return pattern.getId();
            } catch (Exception e) {
                throw new IllegalArgumentException("Squad working pattern not found with ID: " + parsed.getId() + ". Make sure it exists in the database or is being created in this import.");
            }
        } else if (parsed.hasName()) {
            // Batch'te var mı kontrol et
            if (isInBatch("SQUAD_WORKING_PATTERN", parsed.getName())) {
                return -1L; // Geçici ID
            }

            String key = parsed.getName().toLowerCase();
            SquadWorkingPatternDTO pattern = squadWorkingPatternCache.get(key);

            if (pattern == null) {
                pattern = squadWorkingPatternService.searchByName(parsed.getName(), false)
                        .stream()
                        .filter(p -> p.getName().equalsIgnoreCase(parsed.getName()))
                        .findFirst()
                        .orElse(null);
                if (pattern != null) {
                    squadWorkingPatternCache.put(key, pattern);
                } else {
                    throw new IllegalArgumentException("Squad working pattern not found: " + parsed.getName());
                }
            }

            if (!pattern.getActive()) {
                throw new IllegalArgumentException("Squad working pattern is inactive: " + parsed.getName());
            }
            return pattern.getId();
        }

        throw new IllegalArgumentException("Invalid squad working pattern reference: " + value);
    }

    /**
     * Resolve Squad ID from value (ID or name)
     */
    public Long resolveSquadId(String value) {
        if (!StringUtils.hasText(value)) return null;

        ParsedEntityId parsed = ParsedEntityId.parse(value);

        if (parsed.hasId()) {
            // Batch'te var mı kontrol et
            if (isInBatch("SQUAD", parsed.getId().toString())) {
                return -1L; // Geçici ID
            }

            try {
                SquadDTO squad = squadService.findById(parsed.getId());
                if (!squad.getActive()) {
                    throw new IllegalArgumentException("Squad is inactive: " + value);
                }
                return squad.getId();
            } catch (Exception e) {
                throw new IllegalArgumentException("Squad not found with ID: " + parsed.getId() + ". Make sure it exists in the database or is being created in this import.");
            }
        } else if (parsed.hasName()) {
            // Batch'te var mı kontrol et
            if (isInBatch("SQUAD", parsed.getName())) {
                return -1L; // Geçici ID
            }

            String key = parsed.getName().toLowerCase();
            SquadDTO squad = squadCache.get(key);

            if (squad == null) {
                squad = squadService.searchByName(parsed.getName(), false)
                        .stream()
                        .filter(s -> s.getName().equalsIgnoreCase(parsed.getName()))
                        .findFirst()
                        .orElse(null);
                if (squad != null) {
                    squadCache.put(key, squad);
                } else {
                    throw new IllegalArgumentException("Squad not found: " + parsed.getName());
                }
            }

            if (!squad.getActive()) {
                throw new IllegalArgumentException("Squad is inactive: " + parsed.getName());
            }
            return squad.getId();
        }

        throw new IllegalArgumentException("Invalid squad reference: " + value);
    }

    /**
     * Resolve Shift ID from value (ID or name)
     */
    public Long resolveShiftId(String value) {
        if (!StringUtils.hasText(value)) return null;

        ParsedEntityId parsed = ParsedEntityId.parse(value);

        if (parsed.hasId()) {
            // Batch'te var mı kontrol et
            if (isInBatch("SHIFT", parsed.getId().toString())) {
                return -1L; // Geçici ID
            }

            try {
                ShiftDTO shift = shiftService.findById(parsed.getId());
                if (!shift.getActive()) {
                    throw new IllegalArgumentException("Shift is inactive: " + value);
                }
                return shift.getId();
            } catch (Exception e) {
                throw new IllegalArgumentException("Shift not found with ID: " + parsed.getId() + ". Make sure it exists in the database or is being created in this import.");
            }
        } else if (parsed.hasName()) {
            // Batch'te var mı kontrol et
            if (isInBatch("SHIFT", parsed.getName())) {
                return -1L; // Geçici ID
            }

            String key = parsed.getName().toLowerCase();
            ShiftDTO shift = shiftCache.get(key);

            if (shift == null) {
                shift = shiftService.searchByName(parsed.getName(), false)
                        .stream()
                        .filter(s -> s.getName().equalsIgnoreCase(parsed.getName()))
                        .findFirst()
                        .orElse(null);
                if (shift != null) {
                    shiftCache.put(key, shift);
                } else {
                    throw new IllegalArgumentException("Shift not found: " + parsed.getName());
                }
            }

            if (!shift.getActive()) {
                throw new IllegalArgumentException("Shift is inactive: " + parsed.getName());
            }
            return shift.getId();
        }

        throw new IllegalArgumentException("Invalid shift reference: " + value);
    }

    /**
     * Resolve Staff ID from value (ID or name)
     */
    public Long resolveStaffId(String value) {
        if (!StringUtils.hasText(value)) return null;

        ParsedEntityId parsed = ParsedEntityId.parse(value);

        if (parsed.hasId()) {
            // Batch'te var mı kontrol et
            if (isInBatch("STAFF", parsed.getId().toString())) {
                return -1L; // Geçici ID
            }

            try {
                StaffDTO staff = staffService.findById(parsed.getId());
                if (!staff.getActive()) {
                    throw new IllegalArgumentException("Staff is inactive: " + value);
                }
                return staff.getId();
            } catch (Exception e) {
                throw new IllegalArgumentException("Staff not found with ID: " + parsed.getId() + ". Make sure it exists in the database or is being created in this import.");
            }
        } else if (parsed.hasName()) {
            // Batch'te var mı kontrol et
            if (isInBatch("STAFF", parsed.getName())) {
                return -1L; // Geçici ID
            }

            String key = parsed.getName().toLowerCase();
            StaffDTO staff = staffCache.get(key);

            if (staff == null) {
                // Try to find by full name using search
                staff = staffService.searchByName(parsed.getName(), false)
                        .stream()
                        .filter(s -> (s.getName() + " " + s.getSurname()).equalsIgnoreCase(parsed.getName()))
                        .findFirst()
                        .orElse(null);

                if (staff != null) {
                    staffCache.put(key, staff);
                } else {
                    throw new IllegalArgumentException("Staff not found: " + parsed.getName());
                }
            }

            if (!staff.getActive()) {
                throw new IllegalArgumentException("Staff is inactive: " + parsed.getName());
            }
            return staff.getId();
        }

        throw new IllegalArgumentException("Invalid staff reference: " + value);
    }

    /**
     * Resolve Staff ID by registration code
     */
    public Long resolveStaffIdByRegistrationCode(String registrationCode) {
        if (!StringUtils.hasText(registrationCode)) {
            throw new IllegalArgumentException("Registration code is required");
        }

        // Batch'te var mı kontrol et
        if (isInBatch("STAFF", registrationCode)) {
            return -1L; // Geçici ID
        }

        String key = registrationCode.toLowerCase();
        StaffDTO staff = staffCache.get(key);

        if (staff == null) {
            staff = staffService.findByRegistrationCode(registrationCode);
            if (staff != null) {
                staffCache.put(key, staff);
            } else {
                throw new IllegalArgumentException("Staff not found with registration code: " + registrationCode);
            }
        }

        if (!staff.getActive()) {
            throw new IllegalArgumentException("Staff is inactive: " + registrationCode);
        }
        return staff.getId();
    }

    /**
     * Resolve Task ID from value (ID or name)
     */
    public Long resolveTaskId(String value) {
        if (!StringUtils.hasText(value)) return null;

        ParsedEntityId parsed = ParsedEntityId.parse(value);

        if (parsed.hasId()) {
            // Batch'te var mı kontrol et
            if (isInBatch("TASK", parsed.getId().toString())) {
                return -1L; // Geçici ID
            }

            try {
                TaskDTO task = taskService.findById(parsed.getId());
                if (!task.getActive()) {
                    throw new IllegalArgumentException("Task is inactive: " + value);
                }
                return task.getId();
            } catch (Exception e) {
                throw new IllegalArgumentException("Task not found with ID: " + parsed.getId() + ". Make sure it exists in the database or is being created in this import.");
            }
        } else if (parsed.hasName()) {
            // Batch'te var mı kontrol et
            if (isInBatch("TASK", parsed.getName())) {
                return -1L; // Geçici ID
            }

            String key = parsed.getName().toLowerCase();
            TaskDTO task = taskCache.get(key);

            if (task == null) {
                task = taskService.searchByName(parsed.getName(), false)
                        .stream()
                        .filter(t -> t.getName().equalsIgnoreCase(parsed.getName()))
                        .findFirst()
                        .orElse(null);
                if (task != null) {
                    taskCache.put(key, task);
                } else {
                    throw new IllegalArgumentException("Task not found: " + parsed.getName());
                }
            }

            if (!task.getActive()) {
                throw new IllegalArgumentException("Task is inactive: " + parsed.getName());
            }
            return task.getId();
        }

        throw new IllegalArgumentException("Invalid task reference: " + value);
    }

    /**
     * Resolve multiple qualification IDs from comma-separated values
     */
    public List<Long> resolveQualificationIds(String commaSeparatedValues) {
        if (!StringUtils.hasText(commaSeparatedValues)) {
            return new ArrayList<>();
        }

        List<Long> qualificationIds = new ArrayList<>();
        String[] values = commaSeparatedValues.split(",");

        for (String value : values) {
            String trimmedValue = value.trim();
            if (StringUtils.hasText(trimmedValue)) {
                try {
                    Long qualId = resolveQualificationId(trimmedValue);
                    if (qualId != null) {
                        qualificationIds.add(qualId);
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error resolving qualification '" + trimmedValue + "': " + e.getMessage());
                }
            }
        }

        return qualificationIds;
    }

    /**
     * Resolve multiple staff IDs from comma-separated registration codes
     */
    public List<Long> resolveStaffIdsByRegistrationCodes(String commaSeparatedCodes) {
        if (!StringUtils.hasText(commaSeparatedCodes)) {
            return new ArrayList<>();
        }

        List<Long> staffIds = new ArrayList<>();
        String[] codes = commaSeparatedCodes.split(",");

        for (String code : codes) {
            String trimmedCode = code.trim();
            if (StringUtils.hasText(trimmedCode)) {
                try {
                    Long staffId = resolveStaffIdByRegistrationCode(trimmedCode);
                    if (staffId != null) {
                        staffIds.add(staffId);
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error resolving staff '" + trimmedCode + "': " + e.getMessage());
                }
            }
        }

        return staffIds;
    }

    /**
     * Clear batch context
     */
    public void clearBatchContext() {
        batchEntities.clear();
    }

    /**
     * Validate if entity exists (in database or batch)
     */
    public void validateEntityExists(String entityType, String value) {
        if (!StringUtils.hasText(value)) return;

        try {
            switch (entityType.toLowerCase()) {
                case "department":
                    resolveDepartmentId(value);
                    break;
                case "qualification":
                    resolveQualificationId(value);
                    break;
                case "workingperiod":
                    resolveWorkingPeriodId(value);
                    break;
                case "squadworkingpattern":
                    resolveSquadWorkingPatternId(value);
                    break;
                case "shift":
                    resolveShiftId(value);
                    break;
                case "squad":
                    resolveSquadId(value);
                    break;
                case "staff":
                    resolveStaffId(value);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown entity type: " + entityType);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Entity not found: " + entityType + " with value: " + value);
        }
    }

    /**
     * Validate staff by registration code
     */
    public void validateStaffByRegistrationCode(String registrationCode) {
        if (!StringUtils.hasText(registrationCode)) return;
        
        Long staffId = resolveStaffIdByRegistrationCode(registrationCode);
        if (staffId == null) {
            throw new IllegalArgumentException("Staff not found with registration code: " + registrationCode);
        }
    }

    /**
     * Resolve constraint ID by name (stub implementation)
     */
    public Long resolveConstraintIdByName(String constraintName) {
        // TODO: Implement constraint service integration when available
        if (!StringUtils.hasText(constraintName)) return null;
        
        // For now, return a dummy ID to prevent compilation errors
        log.debug("Resolving constraint by name: {} (stub implementation)", constraintName);
        return -1L;
    }

    /**
     * Validate constraint by name
     */
    public void validateConstraintByName(String constraintName) {
        if (!StringUtils.hasText(constraintName)) return;
        
        Long constraintId = resolveConstraintIdByName(constraintName);
        if (constraintId == null) {
            throw new IllegalArgumentException("Constraint not found with name: " + constraintName);
        }
    }
}