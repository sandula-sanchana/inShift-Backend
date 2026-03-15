package edu.ijse.inshiftbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "shift")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shiftId;

    @Column(nullable = false, length = 80)
    private String shiftName;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer graceMinutes;

    @Column(nullable = false)
    private Integer earlyCheckInMinutes;

    @Column(nullable = false)
    private Integer earlyLeaveGraceMinutes;

    @Column(nullable = false)
    private Integer overtimeAfterMinutes;

    @Column(nullable = false)
    private Integer breakMinutes;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean isDefault;
}