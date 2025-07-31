package com.rosteroptimization.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "constraint_overrides",
        uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "constraint_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String overrideValue; // Staff-specific value for this constraint

    // Foreign Key Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constraint_id", nullable = false)
    private Constraint constraint;
}