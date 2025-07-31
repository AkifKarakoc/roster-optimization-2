package com.rosteroptimization.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "squads")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Squad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate; // When the squad started following the working pattern

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_working_pattern_id", nullable = false)
    private SquadWorkingPattern squadWorkingPattern;

    @OneToMany(mappedBy = "squad", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Staff> staffList;
}