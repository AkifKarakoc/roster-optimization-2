package com.rosteroptimization.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Table(name = "constraints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Constraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // Unique constraint name (e.g., "MaxWorkingHoursPerWeek")

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ConstraintType type; // HARD or SOFT

    @Column(nullable = false, length = 100)
    private String defaultValue; // Default value for this constraint

    @Column(length = 500)
    private String description; // Human-readable description

    @Column(nullable = false)
    private Boolean active = true;

    // One-to-Many Relationships
    @OneToMany(mappedBy = "constraint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConstraintOverride> constraintOverrides;

    // Enum for constraint types
    public enum ConstraintType {
        HARD,  // Must be satisfied (violations reject the solution)
        SOFT   // Preferred to be satisfied (violations add penalty to score)
    }
}