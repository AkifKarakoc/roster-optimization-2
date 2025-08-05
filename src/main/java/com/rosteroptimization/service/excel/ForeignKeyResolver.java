package com.rosteroptimization.service.excel;

import java.util.Set;

public interface ForeignKeyResolver {

    /**
     * Resolve department by name, return ID
     */
    Long resolveDepartment(String departmentName);

    /**
     * Resolve squad by name, return ID
     */
    Long resolveSquad(String squadName);

    /**
     * Resolve qualifications by comma-separated names, return IDs
     */
    Set<Long> resolveQualifications(String qualificationNames);

    /**
     * Resolve working period by name, return ID
     */
    Long resolveWorkingPeriod(String workingPeriodName);

    /**
     * Resolve squad working pattern by name, return ID
     */
    Long resolveSquadWorkingPattern(String patternName);

    /**
     * Resolve staff by registration code, return ID
     */
    Long resolveStaff(String registrationCode);

    /**
     * Resolve constraint by name, return ID
     */
    Long resolveConstraint(String constraintName);

    /**
     * Clear cached resolutions
     */
    void clearCache();
}