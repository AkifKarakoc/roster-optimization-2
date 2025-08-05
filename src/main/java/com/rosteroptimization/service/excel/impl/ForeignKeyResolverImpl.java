package com.rosteroptimization.service.excel.impl;

import com.rosteroptimization.service.DepartmentService;
import com.rosteroptimization.service.QualificationService;
import com.rosteroptimization.service.SquadService;
import com.rosteroptimization.service.StaffService;
import com.rosteroptimization.service.WorkingPeriodService;
import com.rosteroptimization.service.SquadWorkingPatternService;
import com.rosteroptimization.service.ConstraintService;
import com.rosteroptimization.service.excel.ForeignKeyResolver;
import com.rosteroptimization.dto.DepartmentDTO;
import com.rosteroptimization.dto.QualificationDTO;
import com.rosteroptimization.dto.SquadDTO;
import com.rosteroptimization.dto.StaffDTO;
import com.rosteroptimization.dto.WorkingPeriodDTO;
import com.rosteroptimization.dto.SquadWorkingPatternDTO;
import com.rosteroptimization.dto.ConstraintDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForeignKeyResolverImpl implements ForeignKeyResolver {

    private final DepartmentService departmentService;
    private final QualificationService qualificationService;
    private final SquadService squadService;
    private final StaffService staffService;
    private final WorkingPeriodService workingPeriodService;
    private final SquadWorkingPatternService squadWorkingPatternService;
    private final ConstraintService constraintService;

    // Global caches
    private final Map<String, Long> departmentCache = new ConcurrentHashMap<>();
    private final Map<String, Long> qualificationCache = new ConcurrentHashMap<>();
    private final Map<String, Long> squadCache = new ConcurrentHashMap<>();
    private final Map<String, Long> staffCache = new ConcurrentHashMap<>();
    private final Map<String, Long> workingPeriodCache = new ConcurrentHashMap<>();
    private final Map<String, Long> squadWorkingPatternCache = new ConcurrentHashMap<>();
    private final Map<String, Long> constraintCache = new ConcurrentHashMap<>();

    @Override
    public Long resolveDepartment(String departmentName) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            return null;
        }

        String key = departmentName.trim().toLowerCase();

        // Check cache first
        if (departmentCache.containsKey(key)) {
            return departmentCache.get(key);
        }

        try {
            List<DepartmentDTO> departments = departmentService.findAll();
            for (DepartmentDTO dept : departments) {
                if (dept.getName().toLowerCase().equals(key)) {
                    departmentCache.put(key, dept.getId());
                    return dept.getId();
                }
            }
        } catch (Exception e) {
            log.error("Error resolving department '{}': ", departmentName, e);
        }

        return null;
    }

    @Override
    public Long resolveSquad(String squadName) {
        if (squadName == null || squadName.trim().isEmpty()) {
            return null;
        }

        String key = squadName.trim().toLowerCase();

        if (squadCache.containsKey(key)) {
            return squadCache.get(key);
        }

        try {
            List<SquadDTO> squads = squadService.findAll();
            for (SquadDTO squad : squads) {
                if (squad.getName().toLowerCase().equals(key)) {
                    squadCache.put(key, squad.getId());
                    return squad.getId();
                }
            }
        } catch (Exception e) {
            log.error("Error resolving squad '{}': ", squadName, e);
        }

        return null;
    }

    @Override
    public Set<Long> resolveQualifications(String qualificationNames) {
        Set<Long> result = new HashSet<>();

        if (qualificationNames == null || qualificationNames.trim().isEmpty()) {
            return result;
        }

        String[] names = qualificationNames.split(",");
        for (String name : names) {
            Long id = resolveSingleQualification(name.trim());
            if (id != null) {
                result.add(id);
            }
        }

        return result;
    }

    private Long resolveSingleQualification(String qualificationName) {
        if (qualificationName == null || qualificationName.trim().isEmpty()) {
            return null;
        }

        String key = qualificationName.trim().toLowerCase();

        if (qualificationCache.containsKey(key)) {
            return qualificationCache.get(key);
        }

        try {
            List<QualificationDTO> qualifications = qualificationService.findAll();
            for (QualificationDTO qual : qualifications) {
                if (qual.getName().toLowerCase().equals(key)) {
                    qualificationCache.put(key, qual.getId());
                    return qual.getId();
                }
            }
        } catch (Exception e) {
            log.error("Error resolving qualification '{}': ", qualificationName, e);
        }

        return null;
    }

    @Override
    public Long resolveWorkingPeriod(String workingPeriodName) {
        if (workingPeriodName == null || workingPeriodName.trim().isEmpty()) {
            return null;
        }

        String key = workingPeriodName.trim().toLowerCase();

        if (workingPeriodCache.containsKey(key)) {
            return workingPeriodCache.get(key);
        }

        try {
            List<WorkingPeriodDTO> periods = workingPeriodService.findAll();
            for (WorkingPeriodDTO period : periods) {
                if (period.getName().toLowerCase().equals(key)) {
                    workingPeriodCache.put(key, period.getId());
                    return period.getId();
                }
            }
        } catch (Exception e) {
            log.error("Error resolving working period '{}': ", workingPeriodName, e);
        }

        return null;
    }

    @Override
    public Long resolveSquadWorkingPattern(String patternName) {
        if (patternName == null || patternName.trim().isEmpty()) {
            return null;
        }

        String key = patternName.trim().toLowerCase();

        if (squadWorkingPatternCache.containsKey(key)) {
            return squadWorkingPatternCache.get(key);
        }

        try {
            List<SquadWorkingPatternDTO> patterns = squadWorkingPatternService.findAll();
            for (SquadWorkingPatternDTO pattern : patterns) {
                if (pattern.getName().toLowerCase().equals(key)) {
                    squadWorkingPatternCache.put(key, pattern.getId());
                    return pattern.getId();
                }
            }
        } catch (Exception e) {
            log.error("Error resolving squad working pattern '{}': ", patternName, e);
        }

        return null;
    }

    @Override
    public Long resolveStaff(String registrationCode) {
        if (registrationCode == null || registrationCode.trim().isEmpty()) {
            return null;
        }

        String key = registrationCode.trim().toLowerCase();

        if (staffCache.containsKey(key)) {
            return staffCache.get(key);
        }

        try {
            List<StaffDTO> staffList = staffService.findAll();
            for (StaffDTO staff : staffList) {
                if (staff.getRegistrationCode().toLowerCase().equals(key)) {
                    staffCache.put(key, staff.getId());
                    return staff.getId();
                }
            }
        } catch (Exception e) {
            log.error("Error resolving staff '{}': ", registrationCode, e);
        }

        return null;
    }

    @Override
    public Long resolveConstraint(String constraintName) {
        if (constraintName == null || constraintName.trim().isEmpty()) {
            return null;
        }

        String key = constraintName.trim().toLowerCase();

        if (constraintCache.containsKey(key)) {
            return constraintCache.get(key);
        }

        try {
            List<ConstraintDTO> constraints = constraintService.findAll();
            for (ConstraintDTO constraint : constraints) {
                if (constraint.getName().toLowerCase().equals(key)) {
                    constraintCache.put(key, constraint.getId());
                    return constraint.getId();
                }
            }
        } catch (Exception e) {
            log.error("Error resolving constraint '{}': ", constraintName, e);
        }

        return null;
    }

    @Override
    public void clearCache() {
        departmentCache.clear();
        qualificationCache.clear();
        squadCache.clear();
        staffCache.clear();
        workingPeriodCache.clear();
        squadWorkingPatternCache.clear();
        constraintCache.clear();
        log.info("FK resolver caches cleared");
    }
}