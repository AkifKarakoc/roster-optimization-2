package com.rosteroptimization;

import com.rosteroptimization.entity.Constraint;
import com.rosteroptimization.entity.Department;
import com.rosteroptimization.entity.Qualification;
import com.rosteroptimization.entity.WorkingPeriod;
import com.rosteroptimization.repository.ConstraintRepository;
import com.rosteroptimization.repository.DepartmentRepository;
import com.rosteroptimization.repository.QualificationRepository;
import com.rosteroptimization.repository.WorkingPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
@Slf4j
@RequiredArgsConstructor
public class RosterOptimizationApplication implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final QualificationRepository qualificationRepository;
    private final WorkingPeriodRepository workingPeriodRepository;
    private final ConstraintRepository constraintRepository;

    public static void main(String[] args) {
        log.info("Starting Roster Optimization Application...");
        SpringApplication.run(RosterOptimizationApplication.class, args);
        log.info("Roster Optimization Application started successfully!");
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing application data...");

        initializeBasicData();
        initializeConstraints();

        log.info("Application data initialization completed!");
        logApplicationInfo();
    }

    /**
     * Initialize basic master data
     */
    private void initializeBasicData() {
        log.info("Creating basic master data...");

        // Create Departments
        if (departmentRepository.count() == 0) {
            List<Department> departments = Arrays.asList(
                    createDepartment("IT Department", "Information Technology Department"),
                    createDepartment("HR Department", "Human Resources Department"),
                    createDepartment("Operations", "Operations Department"),
                    createDepartment("Security", "Security Department"),
                    createDepartment("Maintenance", "Maintenance Department")
            );
            departmentRepository.saveAll(departments);
            log.info("Created {} departments", departments.size());
        }

        // Create Qualifications
        if (qualificationRepository.count() == 0) {
            List<Qualification> qualifications = Arrays.asList(
                    createQualification("Basic Computer Skills", "Basic computer and software knowledge"),
                    createQualification("Advanced IT Skills", "Advanced technical and programming skills"),
                    createQualification("Team Leadership", "Team management and leadership abilities"),
                    createQualification("Safety Certification", "Workplace safety and emergency procedures"),
                    createQualification("Customer Service", "Customer interaction and service skills"),
                    createQualification("Technical Support", "Hardware and software troubleshooting"),
                    createQualification("Project Management", "Project planning and execution skills"),
                    createQualification("Security Clearance", "Security protocols and access authorization"),
                    createQualification("Equipment Operation", "Specialized equipment operation skills"),
                    createQualification("Quality Assurance", "Quality control and testing procedures")
            );
            qualificationRepository.saveAll(qualifications);
            log.info("Created {} qualifications", qualifications.size());
        }

        // Create Working Periods
        if (workingPeriodRepository.count() == 0) {
            List<WorkingPeriod> workingPeriods = Arrays.asList(
                    createWorkingPeriod("Day Shift", "07:00", "19:00", "Standard day shift period"),
                    createWorkingPeriod("Night Shift", "19:00", "07:00", "Standard night shift period"),
                    createWorkingPeriod("Morning", "06:00", "14:00", "Morning working period"),
                    createWorkingPeriod("Evening", "14:00", "22:00", "Evening working period"),
                    createWorkingPeriod("Full Day", "00:00", "23:59", "24-hour coverage period")
            );
            workingPeriodRepository.saveAll(workingPeriods);
            log.info("Created {} working periods", workingPeriods.size());
        }
    }

    /**
     * Initialize default system constraints
     */
    private void initializeConstraints() {
        log.info("Creating default system constraints...");

        if (constraintRepository.count() == 0) {
            List<Constraint> constraints = Arrays.asList(
                    // HARD Constraints
                    createConstraint("MaxWorkingHoursPerDay", Constraint.ConstraintType.HARD, "12",
                            "Maximum working hours per day for any employee"),
                    createConstraint("MaxWorkingHoursPerWeek", Constraint.ConstraintType.HARD, "40",
                            "Maximum working hours per week for any employee"),
                    createConstraint("MaxWorkingHoursPerMonth", Constraint.ConstraintType.HARD, "160",
                            "Maximum working hours per month for any employee"),
                    createConstraint("MinimumDayOff", Constraint.ConstraintType.HARD, "2",
                            "Minimum number of days off per week"),
                    createConstraint("TimeBetweenShifts", Constraint.ConstraintType.HARD, "8",
                            "Minimum hours between consecutive shifts"),
                    createConstraint("NightShiftsAllowed", Constraint.ConstraintType.HARD, "true",
                            "Whether night shifts are allowed for employees"),
                    createConstraint("OverlappingAllowed", Constraint.ConstraintType.HARD, "false",
                            "Whether overlapping task assignments are allowed"),
                    createConstraint("SplitShiftsAllowed", Constraint.ConstraintType.HARD, "false",
                            "Whether multiple shifts on same day are allowed"),
                    createConstraint("TaskCoverageEnforcement", Constraint.ConstraintType.HARD, "true",
                            "Ensures all tasks are assigned to qualified staff"),

                    // SOFT Constraints
                    createConstraint("WorkingPatternCompliancy", Constraint.ConstraintType.SOFT, "enabled",
                            "Prefer employees to work according to their squad patterns"),
                    createConstraint("PreferredAvailability", Constraint.ConstraintType.SOFT, "enabled",
                            "Consider employee preferred time windows"),
                    createConstraint("CoverageDistribution", Constraint.ConstraintType.SOFT, "enabled",
                            "Balanced coverage distribution across time periods"),
                    createConstraint("ShiftPriority", Constraint.ConstraintType.SOFT, "enabled",
                            "Use higher priority shifts when possible"),
                    createConstraint("FairnessTarget", Constraint.ConstraintType.SOFT, "4",
                            "Maximum deviation of assigned hours among employees"),
                    createConstraint("MaxBalance", Constraint.ConstraintType.SOFT, "300",
                            "Maximum working hours in balance period"),
                    createConstraint("MaxBalancePeriod", Constraint.ConstraintType.SOFT, "4",
                            "Balance period duration in weeks"),
                    createConstraint("QualificationPreference", Constraint.ConstraintType.SOFT, "enabled",
                            "Prefer assigning tasks to most qualified staff"),
                    createConstraint("ConsecutiveWorkDaysLimit", Constraint.ConstraintType.SOFT, "7",
                            "Preferred maximum consecutive working days"),
                    createConstraint("WeekendWorkFrequency", Constraint.ConstraintType.SOFT, "2",
                            "Maximum weekend work assignments per month")
            );
            constraintRepository.saveAll(constraints);
            log.info("Created {} system constraints", constraints.size());
        }
    }

    /**
     * Helper method to create Department
     */
    private Department createDepartment(String name, String description) {
        Department department = new Department();
        department.setName(name);
        department.setDescription(description);
        department.setActive(true);
        return department;
    }

    /**
     * Helper method to create Qualification
     */
    private Qualification createQualification(String name, String description) {
        Qualification qualification = new Qualification();
        qualification.setName(name);
        qualification.setDescription(description);
        qualification.setActive(true);
        return qualification;
    }

    /**
     * Helper method to create Working Period
     */
    private WorkingPeriod createWorkingPeriod(String name, String startTime, String endTime, String description) {
        WorkingPeriod workingPeriod = new WorkingPeriod();
        workingPeriod.setName(name);
        workingPeriod.setStartTime(LocalTime.parse(startTime));
        workingPeriod.setEndTime(LocalTime.parse(endTime));
        workingPeriod.setDescription(description);
        workingPeriod.setActive(true);
        return workingPeriod;
    }

    /**
     * Helper method to create Constraint
     */
    private Constraint createConstraint(String name, Constraint.ConstraintType type, String defaultValue, String description) {
        Constraint constraint = new Constraint();
        constraint.setName(name);
        constraint.setType(type);
        constraint.setDefaultValue(defaultValue);
        constraint.setDescription(description);
        constraint.setActive(true);
        return constraint;
    }

    /**
     * Log application startup information
     */
    private void logApplicationInfo() {
        log.info("=".repeat(60));
        log.info("🚀 ROSTER OPTIMIZATION APPLICATION READY");
        log.info("=".repeat(60));
        log.info("📊 Application URLs:");
        log.info("   • Main Application: http://localhost:8080");
        log.info("   • H2 Database Console: http://localhost:8080/h2-console");
        log.info("   • Swagger UI: http://localhost:8080/swagger-ui.html");
        log.info("   • API Documentation: http://localhost:8080/api-docs");
        log.info("   • Health Check: http://localhost:8080/actuator/health");
        log.info("");
        log.info("🗄️ Database Information:");
        log.info("   • JDBC URL: jdbc:h2:mem:rosterdb");
        log.info("   • Username: sa");
        log.info("   • Password: (empty)");
        log.info("");
        log.info("📈 Initial Data Created:");
        log.info("   • Departments: {}", departmentRepository.count());
        log.info("   • Qualifications: {}", qualificationRepository.count());
        log.info("   • Working Periods: {}", workingPeriodRepository.count());
        log.info("   • System Constraints: {}", constraintRepository.count());
        log.info("");
        log.info("🔐 Default Login:");
        log.info("   • Username: admin");
        log.info("   • Password: admin123");
        log.info("");
        log.info("📋 Available REST API Endpoints:");
        log.info("   • /api/departments - Department management");
        log.info("   • /api/qualifications - Qualification management");
        log.info("   • /api/working-periods - Working period management");
        log.info("   • /api/shifts - Shift management");
        log.info("   • /api/squad-working-patterns - Pattern management");
        log.info("   • /api/squads - Squad management");
        log.info("   • /api/staff - Staff management");
        log.info("   • /api/tasks - Task management");
        log.info("   • /api/day-off-rules - Day-off rule management");
        log.info("   • /api/constraints - Constraint management");
        log.info("   • /api/constraint-overrides - Override management");
        log.info("=".repeat(60));
        log.info("✅ Application is ready to accept requests!");
        log.info("=".repeat(60));
    }
}