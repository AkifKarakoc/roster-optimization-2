package com.rosteroptimization.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "working_periods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    // Relationships - OneToMany mappings
    @OneToMany(mappedBy = "workingPeriod", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Shift> shiftList;
}