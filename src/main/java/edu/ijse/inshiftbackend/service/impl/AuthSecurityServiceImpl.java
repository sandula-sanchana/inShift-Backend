package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AuthSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthSecurityServiceImpl implements AuthSecurityService {

    private final EmployeeRepository employeeRepository;

    @Override
    public boolean hasRecentPasswordAuth(Long employeeId, long minutes) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        LocalDateTime last = employee.getLastPasswordAuthenticatedAt();
        if (last == null) return false;

        return last.isAfter(LocalDateTime.now().minusMinutes(minutes));
    }
}