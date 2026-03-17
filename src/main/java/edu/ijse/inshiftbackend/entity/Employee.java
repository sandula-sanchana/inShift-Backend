package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    @Column(nullable = false, unique = true, length = 20)
    private String empCode;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Role role; // EMPLOYEE, SUPERVISOR, HR, ADMIN

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean mustChangePassword;

    @Column(nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "branch_id", referencedColumnName = "branchId")
    private Branch branch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(name = "last_password_authenticated_at")
    private LocalDateTime lastPasswordAuthenticatedAt;
}