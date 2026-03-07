package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.WebAuthnChallenge;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.WebAuthnChallengeRepository;
import edu.ijse.inshiftbackend.service.WebAuthnChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class WebAuthnChallengeServiceImpl implements WebAuthnChallengeService {

    private final EmployeeRepository employeeRepository;
    private final SecureRandom secureRandom;
    private final WebAuthnChallengeRepository webAuthnChallengeRepository;

    @Override
    public String createChallenge(WebAuthnChallengePurpose purpose,Employee employee) {


        byte[] challengeBytes = new byte[32];
        secureRandom.nextBytes(challengeBytes);

        String challenge = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(challengeBytes);

        WebAuthnChallenge webAuthnChallenge = WebAuthnChallenge.builder()
                .employee(employee)
                .challenge(challenge)
                .purpose(purpose)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        webAuthnChallengeRepository.save(webAuthnChallenge);

        return challenge;
    }

    @Override
    public WebAuthnChallenge getValidChallenge(WebAuthnChallengePurpose purpose) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        WebAuthnChallenge challenge = webAuthnChallengeRepository
                .findTopByEmployeeAndPurposeAndUsedFalseOrderByCreatedAtDesc(employee, purpose)
                .orElseThrow(() -> new BadRequestException("No valid WebAuthn challenge found"));

        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("WebAuthn challenge has expired");
        }

        //TODO: i need to remove or clean expired one too

        return challenge;
    }

    @Override
    public void markChallengeAsUsed(WebAuthnChallenge challenge) {
        if (challenge == null) {
            throw new BadRequestException("Challenge cannot be null");
        }

        challenge.setUsed(true);
        webAuthnChallengeRepository.save(challenge);
    }
}