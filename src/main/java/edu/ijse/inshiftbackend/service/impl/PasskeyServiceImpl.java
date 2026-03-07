package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.PasskeyRegisterVerifyDTO;
import edu.ijse.inshiftbackend.dto.response.PasskeyRegisterOptionsDTO;
import edu.ijse.inshiftbackend.dto.response.PubKeyCredParamDTO;
import edu.ijse.inshiftbackend.dto.response.RpDTO;
import edu.ijse.inshiftbackend.dto.response.UserDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import edu.ijse.inshiftbackend.entity.WebAuthnChallenge;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PasskeyCredentialRepository;
import edu.ijse.inshiftbackend.service.PasskeyService;
import edu.ijse.inshiftbackend.service.WebAuthnChallengeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasskeyServiceImpl implements PasskeyService {

    private final WebAuthnChallengeService webAuthnChallengeService;
    private final EmployeeRepository employeeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;

    @Override
    public PasskeyRegisterOptionsDTO getPasskeyRegisterResponse(WebAuthnChallengePurpose purpose) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        String challenge = webAuthnChallengeService.createChallenge(purpose, employee);

        if (challenge == null || challenge.isBlank()) {
            throw new BadRequestException("Challenge is null or empty");
        }

        String userIdBase64Url = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        employee.getEmployeeId().toString().getBytes(StandardCharsets.UTF_8)
                );

        return PasskeyRegisterOptionsDTO.builder()
                .rp(RpDTO.builder()
                        .id("provocatively-televisional-wei.ngrok-free.dev")
                        .name("InShift")
                        .build())
                .user(UserDTO.builder()
                        .id(userIdBase64Url)
                        .name(employee.getEmail())
                        .displayName(employee.getFullName())
                        .build())
                .challenge(challenge)
                .pubKeyCredParams(List.of(
                        PubKeyCredParamDTO.builder()
                                .type("public-key")
                                .alg(-7)
                                .build(),
                        PubKeyCredParamDTO.builder()
                                .type("public-key")
                                .alg(-257)
                                .build()
                ))
                .timeout(60000)
                .attestation("none")
                .authenticatorAttachment("platform")
                .residentKey("preferred")
                .userVerification("required")
                .build();
    }

    @Override
    @Transactional
    public void verifyAndSavePasskey(PasskeyRegisterVerifyDTO dto) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        WebAuthnChallenge challenge =
                webAuthnChallengeService.getValidChallenge(WebAuthnChallengePurpose.REGISTER);

        if (dto == null) {
            throw new BadRequestException("Passkey register verify payload is null");
        }

        if (passkeyCredentialRepository.existsByCredentialId(dto.getCredentialId())) {
            throw new BadRequestException("Passkey credential already exists");
        }

        PasskeyCredential credential = PasskeyCredential.builder()
                .employee(employee)
                .credentialId(dto.getCredentialId())
                .publicKey(dto.getPublicKey())
                .signCount(dto.getSignCount() == null ? 0L : dto.getSignCount())
                .deviceName(dto.getDeviceName())
                .active(true)
                .build();

        passkeyCredentialRepository.save(credential);

        webAuthnChallengeService.markChallengeAsUsed(challenge);
    }
}