package edu.ijse.inshiftbackend.service.impl;

import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import edu.ijse.inshiftbackend.dto.PasskeyAssertionVerifyDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterStartDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterVerifyDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import edu.ijse.inshiftbackend.entity.WebAuthnAssertionRequest;
import edu.ijse.inshiftbackend.entity.WebAuthnRegistrationRequest;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import edu.ijse.inshiftbackend.entity.enums.PasskeyCredentialStatus;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PasskeyCredentialRepository;
import edu.ijse.inshiftbackend.repository.WebAuthnAssertionRequestRepository;
import edu.ijse.inshiftbackend.repository.WebAuthnRegistrationRequestRepository;
import edu.ijse.inshiftbackend.service.AuthSecurityService;
import edu.ijse.inshiftbackend.service.DeviceRecognitionService;
import edu.ijse.inshiftbackend.service.PasskeyService;
import edu.ijse.inshiftbackend.service.TrustedDeviceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasskeyServiceImpl implements PasskeyService {

    private final EmployeeRepository employeeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final WebAuthnRegistrationRequestRepository registrationRequestRepository;
    private final WebAuthnAssertionRequestRepository assertionRequestRepository;
    private final RelyingParty relyingParty;

    private final AuthSecurityService authSecurityService;
    private final DeviceRecognitionService deviceRecognitionService;
    private final TrustedDeviceService trustedDeviceService;

    @Override
    public String getPasskeyRegisterResponse(
            WebAuthnChallengePurpose purpose,
            PasskeyRegisterStartDTO dto,
            String userAgent,
            String ipAddress
    ) {
        validateRegisterStartDTO(dto);

        Employee employee = getCurrentEmployee();
        long activeCount = passkeyCredentialRepository.countByEmployeeAndActiveTrue(employee);

        requireApprovedMobileDevice(employee, dto.getDeviceFingerprint());

        if (activeCount == 0) {
            ensureRecentPasswordAuth(
                    employee,
                    "Recent password authentication required to register first passkey"
            );
            return createRegistrationRequest(employee, purpose);
        }

        boolean sameDeviceLike = deviceRecognitionService.isSameDeviceLike(
                employee,
                dto.getDeviceName(),
                userAgent
        );

        if (sameDeviceLike) {
            ensureRecentPasswordAuth(
                    employee,
                    "Recent password authentication required for re-enrollment"
            );
            return createRegistrationRequest(employee, purpose);
        }

        ensureRecentPasswordAuth(
                employee,
                "Recent password authentication required to register passkey on a new approved device"
        );

        return createRegistrationRequest(employee, purpose);
    }

    @Override
    @Transactional
    public void verifyAndSavePasskey(PasskeyRegisterVerifyDTO dto, String userAgent) {
        validateRegisterVerifyDTO(dto);

        Employee employee = getCurrentEmployee();

        WebAuthnRegistrationRequest savedRequest = registrationRequestRepository
                .findTopByEmployeeAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        employee,
                        WebAuthnChallengePurpose.REGISTER,
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new BadRequestException("No valid registration request found"));

        long activeCount = passkeyCredentialRepository.countByEmployeeAndActiveTrue(employee);
        boolean sameDeviceReEnrollment = false;

        if (activeCount == 0) {
            requireApprovedMobileDevice(employee, dto.getDeviceFingerprint());
            ensureRecentPasswordAuth(employee, "Recent password authentication required");
        } else {
            boolean sameDeviceLike = deviceRecognitionService.isSameDeviceLike(
                    employee,
                    dto.getDeviceName(),
                    userAgent
            );

            if (sameDeviceLike) {
                sameDeviceReEnrollment = true;
                ensureRecentPasswordAuth(
                        employee,
                        "Recent password authentication required for re-enrollment"
                );
            } else {
                requireApprovedMobileDevice(employee, dto.getDeviceFingerprint());
                ensureRecentPasswordAuth(
                        employee,
                        "Recent password authentication required for new approved mobile device registration"
                );
            }
        }

        try {
            PublicKeyCredentialCreationOptions request =
                    PublicKeyCredentialCreationOptions.fromJson(savedRequest.getRequestJson());

            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                    PublicKeyCredential.parseRegistrationResponseJson(dto.getCredentialJson());

            RegistrationResult result = relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(request)
                            .response(pkc)
                            .build()
            );

            String credentialId = result.getKeyId().getId().getBase64Url();

            if (passkeyCredentialRepository.existsByCredentialId(credentialId)) {
                throw new BadRequestException("Passkey credential already exists");
            }

            PasskeyCredential newCredential = PasskeyCredential.builder()
                    .employee(employee)
                    .credentialId(credentialId)
                    .publicKey(result.getPublicKeyCose().getBase64Url())
                    .signCount(result.getSignatureCount())
                    .deviceName(dto.getDeviceName())
                    .userAgent(userAgent)
                    .status(PasskeyCredentialStatus.ACTIVE)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            passkeyCredentialRepository.save(newCredential);

            if (sameDeviceReEnrollment) {
                revokeOtherActiveCredentials(
                        employee,
                        newCredential.getCredentialId(),
                        "Same-device re-enrollment replaced older credential"
                );
            }

            savedRequest.setUsed(true);
            registrationRequestRepository.save(savedRequest);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Passkey registration verification failed");
        }
    }

    @Override
    public String getPasskeyAssertionResponse(WebAuthnChallengePurpose purpose) {
        Employee employee = getCurrentEmployee();

        try {
            AssertionRequest request = relyingParty.startAssertion(
                    StartAssertionOptions.builder()
                            .username(employee.getEmail())
                            .userVerification(UserVerificationRequirement.REQUIRED)
                            .build()
            );

            WebAuthnAssertionRequest savedRequest = WebAuthnAssertionRequest.builder()
                    .employee(employee)
                    .purpose(purpose)
                    .requestJson(request.toJson())
                    .used(false)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            assertionRequestRepository.save(savedRequest);

            return request.toCredentialsGetJson();

        } catch (Exception e) {
            throw new BadRequestException("Failed to create passkey assertion options");
        }
    }

    @Override
    @Transactional
    public void verifyPasskeyAssertion(PasskeyAssertionVerifyDTO dto) {
        verifyPasskeyAssertion(dto, WebAuthnChallengePurpose.AUTHENTICATE);
    }

    @Override
    @Transactional
    public void verifyPasskeyAssertion(
            PasskeyAssertionVerifyDTO dto,
            WebAuthnChallengePurpose purpose
    ) {
        if (dto == null || dto.getCredentialJson() == null || dto.getCredentialJson().isBlank()) {
            throw new BadRequestException("Credential JSON is required");
        }

        if (purpose == null) {
            throw new BadRequestException("Assertion purpose is required");
        }

        Employee employee = getCurrentEmployee();

        WebAuthnAssertionRequest savedRequest = assertionRequestRepository
                .findTopByEmployeeAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        employee,
                        purpose,
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new BadRequestException("No valid assertion request found"));

        try {
            AssertionRequest request = AssertionRequest.fromJson(savedRequest.getRequestJson());

            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                    PublicKeyCredential.parseAssertionResponseJson(dto.getCredentialJson());

            AssertionResult result = relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(request)
                            .response(pkc)
                            .build()
            );

            if (!result.isSuccess()) {
                throw new BadRequestException("Passkey assertion failed");
            }

            PasskeyCredential credential = passkeyCredentialRepository
                    .findByCredentialIdAndActiveTrue(result.getCredentialId().getBase64Url())
                    .orElseThrow(() -> new BadRequestException("Credential not found or inactive"));

            credential.setSignCount(result.getSignatureCount());
            credential.setLastUsedAt(LocalDateTime.now());
            passkeyCredentialRepository.save(credential);

            savedRequest.setUsed(true);
            assertionRequestRepository.save(savedRequest);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Passkey assertion verification failed");
        }
    }

    private Employee getCurrentEmployee() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private void validateRegisterStartDTO(PasskeyRegisterStartDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Registration request is required");
        }

        if (dto.getDeviceName() == null || dto.getDeviceName().isBlank()) {
            throw new BadRequestException("Device name is required");
        }

        if (dto.getDeviceFingerprint() == null || dto.getDeviceFingerprint().isBlank()) {
            throw new BadRequestException("Device fingerprint is required");
        }

        if (dto.getRequestedTrustType() == null) {
            throw new BadRequestException("Requested trust type is required");
        }
    }

    private void validateRegisterVerifyDTO(PasskeyRegisterVerifyDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Registration verification request is required");
        }

        if (dto.getCredentialJson() == null || dto.getCredentialJson().isBlank()) {
            throw new BadRequestException("Credential JSON is required");
        }

        if (dto.getDeviceName() == null || dto.getDeviceName().isBlank()) {
            throw new BadRequestException("Device name is required");
        }

        if (dto.getDeviceFingerprint() == null || dto.getDeviceFingerprint().isBlank()) {
            throw new BadRequestException("Device fingerprint is required");
        }
    }

    private EmployeeDevice requireApprovedMobileDevice(Employee employee, String deviceFingerprint) {
        EmployeeDevice approvedDevice = trustedDeviceService.requireApprovedDevice(employee, deviceFingerprint);

        if (approvedDevice.getApprovedTrustType() != DeviceTrustType.MOBILE) {
            throw new BadRequestException("Passkey registration is only allowed on approved mobile devices");
        }

        return approvedDevice;
    }

    private void ensureRecentPasswordAuth(Employee employee, String message) {
        if (!authSecurityService.hasRecentPasswordAuth(employee.getEmployeeId(), 5)) {
            throw new BadRequestException(message);
        }
    }

    private String createRegistrationRequest(Employee employee, WebAuthnChallengePurpose purpose) {
        UserIdentity userIdentity = UserIdentity.builder()
                .name(employee.getEmail())
                .displayName(employee.getFullName())
                .id(new ByteArray(
                        ByteBuffer.allocate(Long.BYTES)
                                .putLong(employee.getEmployeeId())
                                .array()
                ))
                .build();

        try {
            PublicKeyCredentialCreationOptions request = relyingParty.startRegistration(
                    StartRegistrationOptions.builder()
                            .user(userIdentity)
                            .build()
            );

            WebAuthnRegistrationRequest savedRequest = WebAuthnRegistrationRequest.builder()
                    .employee(employee)
                    .purpose(purpose)
                    .requestJson(request.toJson())
                    .used(false)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            registrationRequestRepository.save(savedRequest);

            return request.toCredentialsCreateJson();

        } catch (Exception e) {
            throw new BadRequestException("Failed to create passkey registration options");
        }
    }

    private void revokeOtherActiveCredentials(Employee employee, String keepCredentialId, String reason) {
        List<PasskeyCredential> activeCredentials = passkeyCredentialRepository.findByEmployeeAndActiveTrue(employee);

        for (PasskeyCredential credential : activeCredentials) {
            if (!credential.getCredentialId().equals(keepCredentialId)) {
                credential.setActive(false);
                credential.setStatus(PasskeyCredentialStatus.REVOKED);
                credential.setRevokedAt(LocalDateTime.now());
                credential.setRevokedReason(reason);
                passkeyCredentialRepository.save(credential);
            }
        }
    }
}