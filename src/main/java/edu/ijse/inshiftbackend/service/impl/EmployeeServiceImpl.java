package edu.ijse.inshiftbackend.service.impl;

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

        String empCode = dto.getEmpCode().trim();

        if (employeeRepository.existsByEmpCode(empCode)) {
            throw new BadRequestException("Employee code already exists");
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String email = dto.getEmail().trim();
            if (employeeRepository.existsByEmail(email)) {
                throw new BadRequestException("Email already exists");
            }
        }

        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        // auto-generate temp password
        String tempPassword = generateTempPassword();

        Employee emp = modelMapper.map(dto, Employee.class);
        emp.setEmployeeId(null);
        emp.setEmpCode(empCode);
        emp.setFullName(dto.getFullName().trim());
        emp.setEmail(dto.getEmail() == null || dto.getEmail().isBlank() ? null : dto.getEmail().trim());
        emp.setPhone(dto.getPhone() == null || dto.getPhone().isBlank() ? null : dto.getPhone().trim());
        emp.setRole(dto.getRole());
        emp.setActive(dto.getActive());
        emp.setBranch(branch);

        // store only hash
        emp.setPasswordHash(passwordEncoder.encode(tempPassword));

        employeeRepository.save(emp);

        // return temp password ONCE (admin will show it)
        return tempPassword;
    }

    @Override
    public void updateEmployee(Long id, EmployeeDTO dto) {

        if (dto == null) throw new BadRequestException("employeeDTO is null");

        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        String newCode = dto.getEmpCode().trim();
        if (!existing.getEmpCode().equals(newCode) && employeeRepository.existsByEmpCode(newCode)) {
            throw new BadRequestException("Employee code already exists");
        }

        String newEmail = dto.getEmail() == null ? null : dto.getEmail().trim();
        if (newEmail != null && !newEmail.isBlank()) {
            boolean emailChanged = existing.getEmail() == null || !existing.getEmail().equals(newEmail);
            if (emailChanged && employeeRepository.existsByEmail(newEmail)) {
                throw new BadRequestException("Email already exists");
            }
        }

        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        existing.setEmpCode(newCode);
        existing.setFullName(dto.getFullName().trim());
        existing.setEmail(newEmail == null || newEmail.isBlank() ? null : newEmail);
        existing.setPhone(dto.getPhone() == null || dto.getPhone().isBlank() ? null : dto.getPhone().trim());
        existing.setRole(dto.getRole());
        existing.setActive(dto.getActive());
        existing.setBranch(branch);

        //We do NOT update password here (admin reset can be separate endpoint later)
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
                .active(emp.getActive())
                .build();


        dto.setPassword(null);
        return dto;
    }

    private String generateTempPassword() {
        //  Temp@ + 4 digits + 1 letter
        int n = 1000 + RAND.nextInt(9000);
        char ch = (char) ('A' + RAND.nextInt(26));
        return "Temp@" + n + ch;
    }
}