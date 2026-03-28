package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.PasskeyAssertionVerifyDTO;
import edu.ijse.inshiftbackend.dto.PresenceCheckBiometricVerifyDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceCheckBiometricProofDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.PresenceCheckBiometricProof;
import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PresenceCheckBiometricProofRepository;
import edu.ijse.inshiftbackend.repository.PresenceCheckRepository;
import edu.ijse.inshiftbackend.service.PasskeyService;
import edu.ijse.inshiftbackend.service.PresenceCheckBiometricService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PresenceCheckBiometricServiceImpl implements PresenceCheckBiometricService {

    private final EmployeeRepository employeeRepository;
    private final PresenceCheckRepository presenceCheckRepository;
    private final EmployeeDeviceRepository employeeDeviceRepository;
    private final PresenceCheckBiometricProofRepository proofRepository;
    private final PasskeyService passkeyService;

    @Override
    public String getPresenceAssertionOptions(
            Long presenceCheckId,
            String deviceFingerprint,
            String employeeEmail
    ) {
        if (presenceCheckId == null) {
            throw new BadRequestException("Presence check ID is required");
        }

        if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
            throw new BadRequestException("Device fingerprint is required");
        }

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        PresenceCheck check = presenceCheckRepository.findById(presenceCheckId)
                .orElseThrow(() -> new ResourceNotFoundException("Presence check not found"));

        if (!check.getEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
            throw new BadRequestException("You cannot verify another employee's presence check");
        }

        if (check.getStatus() != PresenceCheckStatus.PENDING) {
            throw new BadRequestException("Presence check is not pending");
        }

        EmployeeDevice device = employeeDeviceRepository
                .findByEmployeeAndDeviceFingerprintAndApprovalStatusAndActiveTrue(
                        employee,
                        deviceFingerprint,
                        DeviceApprovalStatus.APPROVED
                )
                .orElseThrow(() -> new BadRequestException("Approved mobile device required"));

        if (device.getApprovedTrustType() != DeviceTrustType.MOBILE) {
            throw new BadRequestException("Biometric presence confirmation requires an approved mobile device");
        }

        return passkeyService.getPasskeyAssertionResponse(WebAuthnChallengePurpose.PRESENCE_CHECK);
    }

    @Override
    @Transactional
    public PresenceCheckBiometricProofDTO verifyPresenceAssertion(
            PresenceCheckBiometricVerifyDTO dto,
            String employeeEmail
    ) {
        if (dto == null) {
            throw new BadRequestException("Presence biometric verification request is required");
        }

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        PresenceCheck check = presenceCheckRepository.findById(dto.getPresenceCheckId())
                .orElseThrow(() -> new ResourceNotFoundException("Presence check not found"));

        if (!check.getEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
            throw new BadRequestException("You cannot verify another employee's presence check");
        }

        if (check.getStatus() != PresenceCheckStatus.PENDING) {
            throw new BadRequestException("Presence check is not pending");
        }

        EmployeeDevice device = employeeDeviceRepository
                .findByEmployeeAndDeviceFingerprintAndApprovalStatusAndActiveTrue(
                        employee,
                        dto.getDeviceFingerprint(),
                        DeviceApprovalStatus.APPROVED
                )
                .orElseThrow(() -> new BadRequestException("Approved mobile device required"));

        if (device.getApprovedTrustType() != DeviceTrustType.MOBILE) {
            throw new BadRequestException("Biometric presence confirmation requires an approved mobile device");
        }

        passkeyService.verifyPasskeyAssertion(
                PasskeyAssertionVerifyDTO.builder()
                        .credentialJson(dto.getCredentialJson())
                        .build(),
                WebAuthnChallengePurpose.PRESENCE_CHECK
        );

        String token = UUID.randomUUID().toString();

        PresenceCheckBiometricProof proof = PresenceCheckBiometricProof.builder()
                .proofToken(token)
                .presenceCheck(check)
                .employee(employee)
                .deviceFingerprint(dto.getDeviceFingerprint())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(90))
                .used(false)
                .usedAt(null)
                .build();

        proofRepository.save(proof);

        return PresenceCheckBiometricProofDTO.builder()
                .proofToken(token)
                .expiresAt(proof.getExpiresAt())
                .build();
    }
}