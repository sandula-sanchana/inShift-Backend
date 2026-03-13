package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "attendance_record",
        indexes = {
                @Index(name = "idx_att_emp_time", columnList = "employee_id,event_time"),
                @Index(name = "idx_att_branch_time", columnList = "branch_id,event_time"),
                @Index(name = "idx_att_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AttendanceType type; // IN / OUT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AttendanceSource source; // MOBILE / WEB

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AttendanceStatus status; // VALID / PENDING / REJECTED

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    private Double lat;
    private Double lng;

    @Column(length = 255)
    private String locationText;

    @Column(length = 500)
    private String reason; // For WEB manual

    @Column(nullable = false)
    private boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private VerificationMethod verificationMethod;

    @Column(length = 500)
    private String decisionNote;

    @Column(nullable = false, length = 20)
    private String createdBy; // EMPLOYEE / ADMIN / SYSTEM

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AttendanceMark attendanceMark;

    @Column
    private Integer lateMinutes;

    @Column
    private Integer earlyLeaveMinutes;

    @Column
    private Integer overtimeMinutes;


    @OneToMany(mappedBy = "attendance", cascade = CascadeType.ALL)
    private List<AttendanceAudit> audits;

    @PrePersist
    public void prePersist() {
        if (eventTime == null) eventTime = LocalDateTime.now();
        if (status == null) status = AttendanceStatus.VALID;
        if (verificationMethod == null) verificationMethod = VerificationMethod.NONE;
        if (attendanceMark == null) attendanceMark = AttendanceMark.NORMAL;
        if (lateMinutes == null) lateMinutes = 0;
        if (earlyLeaveMinutes == null) earlyLeaveMinutes = 0;
        if (overtimeMinutes == null) overtimeMinutes = 0;
    }
}