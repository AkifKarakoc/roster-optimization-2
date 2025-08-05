package com.rosteroptimization.dto.excel;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
public class ResolutionContext {
    private Map<String, Long> departmentCache = new HashMap<>();
    private Map<String, Long> squadCache = new HashMap<>();
    private Map<String, Long> qualificationCache = new HashMap<>();
    private Map<String, Long> workingPeriodCache = new HashMap<>();
    private Map<String, Long> squadWorkingPatternCache = new HashMap<>();
    private Map<String, Long> staffCache = new HashMap<>();
    private Map<String, Long> constraintCache = new HashMap<>();

    private boolean enableCache = true;

    public void putDepartment(String name, Long id) {
        if (enableCache) {
            departmentCache.put(name.toLowerCase(), id);
        }
    }

    public Long getDepartment(String name) {
        return departmentCache.get(name.toLowerCase());
    }

    public void putSquad(String name, Long id) {
        if (enableCache) {
            squadCache.put(name.toLowerCase(), id);
        }
    }

    public Long getSquad(String name) {
        return squadCache.get(name.toLowerCase());
    }

    public void putQualification(String name, Long id) {
        if (enableCache) {
            qualificationCache.put(name.toLowerCase(), id);
        }
    }

    public Long getQualification(String name) {
        return qualificationCache.get(name.toLowerCase());
    }

    public void putWorkingPeriod(String name, Long id) {
        if (enableCache) {
            workingPeriodCache.put(name.toLowerCase(), id);
        }
    }

    public Long getWorkingPeriod(String name) {
        return workingPeriodCache.get(name.toLowerCase());
    }

    public void putSquadWorkingPattern(String name, Long id) {
        if (enableCache) {
            squadWorkingPatternCache.put(name.toLowerCase(), id);
        }
    }

    public Long getSquadWorkingPattern(String name) {
        return squadWorkingPatternCache.get(name.toLowerCase());
    }

    public void putStaff(String registrationCode, Long id) {
        if (enableCache) {
            staffCache.put(registrationCode.toLowerCase(), id);
        }
    }

    public Long getStaff(String registrationCode) {
        return staffCache.get(registrationCode.toLowerCase());
    }

    public void putConstraint(String name, Long id) {
        if (enableCache) {
            constraintCache.put(name.toLowerCase(), id);
        }
    }

    public Long getConstraint(String name) {
        return constraintCache.get(name.toLowerCase());
    }

    public void clearAll() {
        departmentCache.clear();
        squadCache.clear();
        qualificationCache.clear();
        workingPeriodCache.clear();
        squadWorkingPatternCache.clear();
        staffCache.clear();
        constraintCache.clear();
    }
}