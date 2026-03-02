package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AuthDTO;
import edu.ijse.inshiftbackend.dto.response.AuthResponseDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AuthService;
import edu.ijse.inshiftbackend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponseDTO login(AuthDTO authDTO) {

        if (authDTO == null || authDTO.getEmail() == null || authDTO.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        String email = authDTO.getEmail().trim();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "Incorrect email or password"
                ));

        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(authDTO.getPassword(), employee.getPasswordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Incorrect email or password");
        }

        String accessToken = jwtUtil.generateToken(employee.getEmail(), employee.getRole().name());

        System.out.println(accessToken);

        return new AuthResponseDTO(accessToken, employee.getRole().name());
    }
}
