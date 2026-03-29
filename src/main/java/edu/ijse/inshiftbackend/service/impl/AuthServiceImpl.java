package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AuthDTO;
import edu.ijse.inshiftbackend.dto.RefreshTokenRequestDTO;
import edu.ijse.inshiftbackend.dto.response.AuthResponseDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AuthService;
import edu.ijse.inshiftbackend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
                .orElseThrow(() -> new BadCredentialsException("Incorrect email or password"));

        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(authDTO.getPassword(), employee.getPasswordHash())) {
            throw new BadCredentialsException("Incorrect email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(employee.getEmail(), employee.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(employee.getEmail());

        System.out.println("access "+ accessToken);
        System.out.println("refresh"+refreshToken);

        employee.setLastPasswordAuthenticatedAt(LocalDateTime.now());
        employeeRepository.save(employee);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(employee.getRole().name())
                .passwordMustChange(employee.getMustChangePassword())
                .build();
    }

    @Override
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        if (refreshTokenRequestDTO == null || refreshTokenRequestDTO.getRefreshToken() == null || refreshTokenRequestDTO.getRefreshToken().isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        String refreshToken = refreshTokenRequestDTO.getRefreshToken().trim();

        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String email = jwtUtil.extractUsername(refreshToken);

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Employee not found"));

        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new BadCredentialsException("Account is disabled");
        }

        String newAccessToken = jwtUtil.generateAccessToken(employee.getEmail(), employee.getRole().name());

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .role(employee.getRole().name())
                .passwordMustChange(employee.getMustChangePassword())
                .build();
    }
}