package com.rosteroptimization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@ToString(exclude = {"requiredQualifications", "department"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer priority; // Lower number = higher priority

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    // Foreign Key Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // Many-to-Many Relationships - Required qualifications for this task
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_required_qualifications",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "qualification_id")
    )
    private Set<Qualification> requiredQualifications;
}