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

    // how many minutes after start is still considered on-time
    @Column(nullable = false)
    private Integer graceMinutes;

    // how early user can check in before shift start
    @Column(nullable = false)
    private Integer earlyCheckInMinutes;

    // overtime only counted after this many minutes past shift end
    @Column(nullable = false)
    private Integer overtimeAfterMinutes;

    @Column(nullable = false)
    private Boolean active;
}