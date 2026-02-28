package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.EmployeeDTO;

import java.util.List;

public interface EmployeeService {

    // i return temp password for admin
    String saveEmployee(EmployeeDTO employeeDTO);

    void updateEmployee(Long id, EmployeeDTO employeeDTO);

    void deleteEmployee(Long id);

    List<EmployeeDTO> getAllEmployees();

    EmployeeDTO getEmployeeById(Long id);

    EmployeeDTO getMe();
}