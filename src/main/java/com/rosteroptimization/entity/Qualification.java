package com.rosteroptimization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "qualifications")
@Getter
@Setter
@ToString(exclude = {"staffSet", "taskSet"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Qualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    // Relationships - ManyToMany mappings
    @ManyToMany(mappedBy = "qualifications", fetch = FetchType.LAZY)
    private Set<Staff> staffSet;

    @ManyToMany(mappedBy = "requiredQualifications", fetch = FetchType.LAZY)
    private Set<Task> taskSet;
}