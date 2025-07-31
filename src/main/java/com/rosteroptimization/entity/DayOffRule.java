package com.rosteroptimization.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "day_off_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayOffRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer workingDays; // Maximum consecutive working days

    @Column(nullable = false)
    private Integer offDays; // Minimum consecutive off days after working period

    @Column(length = 100)
    private String fixedOffDays; // Fixed off days in week (e.g., "MONDAY,TUESDAY")

    // One-to-One Relationship with Staff
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false, unique = true)
    private Staff staff;
}