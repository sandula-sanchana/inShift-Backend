package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.ChangePasswordDTO;
import edu.ijse.inshiftbackend.dto.EmployeeDTO;
import edu.ijse.inshiftbackend.entity.Branch;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.BranchRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RAND = new SecureRandom();

    @Override
    public String saveEmployee(EmployeeDTO dto) {
        if (dto == null) throw new BadRequestException("employeeDTO is null");

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String email = dto.getEmail().trim();
            if (employeeRepository.existsByEmail(email)) {
                throw new BadRequestException("Email already exists");
            }
        }

        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        String tempPassword = generateTempPassword();
        String generatedEmpCode = generateNextEmpCode();

        Employee emp = modelMapper.map(dto, Employee.class);
        emp.setEmployeeId(null);
        emp.setEmpCode(generatedEmpCode);
        emp.setFullName(dto.getFullName().trim());
        emp.setEmail(dto.getEmail() == null || dto.getEmail().isBlank() ? null : dto.getEmail().trim());
        emp.setPhone(dto.getPhone() == null || dto.getPhone().isBlank() ? null : dto.getPhone().trim());
        emp.setRole(dto.getRole());
        emp.setActive(dto.getActive() != null ? dto.getActive() : true);
        emp.setMustChangePassword(true);
        emp.setBranch(branch);
        emp.setPasswordHash(passwordEncoder.encode(tempPassword));

        employeeRepository.save(emp);

        return tempPassword;
    }

    @Override
    public void updateEmployee(Long id, EmployeeDTO dto) {
        if (dto == null) throw new BadRequestException("employeeDTO is null");

        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        String newEmail = dto.getEmail() == null ? null : dto.getEmail().trim();
        if (newEmail != null && !newEmail.isBlank()) {
            boolean emailChanged = existing.getEmail() == null || !existing.getEmail().equals(newEmail);
            if (emailChanged && employeeRepository.existsByEmail(newEmail)) {
                throw new BadRequestException("Email already exists");
            }
        }

        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        // empCode is NOT editable
        existing.setFullName(dto.getFullName().trim());
        existing.setEmail(newEmail == null || newEmail.isBlank() ? null : newEmail);
        existing.setPhone(dto.getPhone() == null || dto.getPhone().isBlank() ? null : dto.getPhone().trim());
        existing.setRole(dto.getRole());
        existing.setActive(dto.getActive());
        existing.setBranch(branch);

        employeeRepository.save(existing);
    }

    @Override
    public EmployeeDTO getMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new BadRequestException("Unauthenticated");
        }

        String email = auth.getName();

        Employee emp = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        return toDto(emp);
    }

    @Override
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found");
        }
        employeeRepository.deleteById(id);
    }

    @Override
    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public EmployeeDTO getEmployeeById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return toDto(emp);
    }

    private EmployeeDTO toDto(Employee emp) {
        EmployeeDTO dto = EmployeeDTO.builder()
                .employeeId(emp.getEmployeeId())
                .empCode(emp.getEmpCode())
                .fullName(emp.getFullName())
                .email(emp.getEmail())
                .phone(emp.getPhone())
                .role(emp.getRole())
                .branchId(emp.getBranch().getBranchId())
                .branchName(emp.getBranch().getBranchName())
                .branchCode(emp.getBranch().getBranchCode())
                .mustChangePassword(emp.getMustChangePassword())
                .active(emp.getActive())
                .build();

        dto.setPassword(null);
        return dto;
    }

    @Override
    public void changeMyPassword(ChangePasswordDTO dto) {
        if (dto == null) throw new BadRequestException("Request body is null");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new BadRequestException("Unauthenticated");
        }

        String email = auth.getName();

        Employee emp = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), emp.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), emp.getPasswordHash())) {
            throw new BadRequestException("New password must be different");
        }

        emp.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        emp.setMustChangePassword(false);
        employeeRepository.save(emp);
    }

    private String generateTempPassword() {
        int n = 1000 + RAND.nextInt(9000);
        char ch = (char) ('A' + RAND.nextInt(26));
        return "Temp@" + n + ch;
    }

    private String generateNextEmpCode() {
        Employee lastEmployee = employeeRepository.findTopByOrderByEmployeeIdDesc().orElse(null);

        if (lastEmployee == null || lastEmployee.getEmpCode() == null || lastEmployee.getEmpCode().isBlank()) {
            return "EMP-001";
        }

        String lastCode = lastEmployee.getEmpCode().trim().toUpperCase();

        try {
            String numericPart = lastCode.replace("EMP-", "").trim();
            int nextNumber = Integer.parseInt(numericPart) + 1;
            return String.format("EMP-%03d", nextNumber);
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate next employee code");
        }
    }
}