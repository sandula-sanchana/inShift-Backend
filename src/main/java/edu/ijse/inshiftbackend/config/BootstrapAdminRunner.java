package edu.ijse.inshiftbackend.config;

import edu.ijse.inshiftbackend.entity.Branch;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.enums.Role;
import edu.ijse.inshiftbackend.repository.BranchRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${inshift.bootstrap.enabled:false}")
    private boolean enabled;

    @Value("${inshift.bootstrap.adminEmail:admin@inshift.com}")
    private String adminEmail;

    @Value("${inshift.bootstrap.adminPassword:Admin@1234}")
    private String adminPassword;

    @Value("${inshift.bootstrap.adminEmpCode:ADM001}")
    private String adminEmpCode;

    @Value("${inshift.bootstrap.branchId:1}")
    private Long branchId;

    @Override
    public void run(String... args) {
        if (!enabled) return;

        boolean adminExists = employeeRepository.findByEmail(adminEmail).isPresent()
                || employeeRepository.findAll().stream().anyMatch(e -> e.getRole() == Role.ADMIN);

        if (adminExists) return;

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Bootstrap branch not found. Create branchId=" + branchId));

        Employee admin = Employee.builder()
                .empCode(adminEmpCode)
                .fullName("System Administrator")
                .email(adminEmail)
                .phone(null)
                .role(Role.ADMIN)
                .active(true)
                .branch(branch)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .build();

        employeeRepository.save(admin);

        System.out.println("Bootstrap ADMIN created: " + adminEmail);
        System.out.println("Password: " + adminPassword);
    }
}