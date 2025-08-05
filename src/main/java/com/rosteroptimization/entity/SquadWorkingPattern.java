package com.rosteroptimization.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Entity
@Table(name = "squad_working_patterns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "squads"})
public class SquadWorkingPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 1000)
    private String shiftPattern;  // Comma-separated: "Shift_1,DayOff,Shift_4,DayOff,DayOff,Shift_18"

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer cycleLength; // Number of days in the pattern cycle

    @Column(nullable = false)
    private Boolean active = true;

    // Relationships - OneToMany mappings
    @OneToMany(mappedBy = "squadWorkingPattern", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Squad> squadList;
}