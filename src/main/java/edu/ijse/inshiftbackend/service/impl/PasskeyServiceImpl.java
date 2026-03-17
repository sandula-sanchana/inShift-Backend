package edu.ijse.inshiftbackend.service.impl;

import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import edu.ijse.inshiftbackend.dto.PasskeyAssertionVerifyDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterStartDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterVerifyDTO;
import edu.ijse.inshiftbackend.entity.DeviceEnrollmentRequest;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import edu.ijse.inshiftbackend.entity.WebAuthnAssertionRequest;
import edu.ijse.inshiftbackend.entity.WebAuthnRegistrationRequest;
import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestStatus;
import edu.ijse.inshiftbackend.entity.enums.PasskeyCredentialStatus;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PasskeyCredentialRepository;
import edu.ijse.inshiftbackend.repository.WebAuthnAssertionRequestRepository;
import edu.ijse.inshiftbackend.repository.WebAuthnRegistrationRequestRepository;
import edu.ijse.inshiftbackend.service.AuthSecurityService;
import edu.ijse.inshiftbackend.service.DeviceEnrollmentRequestService;
import edu.ijse.inshiftbackend.service.DeviceRecognitionService;
import edu.ijse.inshiftbackend.service.PasskeyService;
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
    private final DeviceEnrollmentRequestService enrollmentRequestService;

    @Override
    public String getPasskeyRegisterResponse(
            WebAuthnChallengePurpose purpose,
            PasskeyRegisterStartDTO dto,
            String userAgent,
            String ipAddress
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (dto == null || dto.getDeviceName() == null || dto.getDeviceName().isBlank()) {
            throw new BadRequestException("Device name is required");
        }

        enrollmentRequestService.expireOldPendingRequests(employee);

        long activeCount = passkeyCredentialRepository.countByEmployeeAndActiveTrue(employee);

        // First passkey registration
        if (activeCount == 0) {
            ensureRecentPasswordAuth(employee, "Recent password authentication required to register first passkey");
            return createRegistrationRequest(employee, purpose);
        }

        // Same-device-like re-enrollment
        boolean sameDeviceLike = deviceRecognitionService.isSameDeviceLike(employee, dto.getDeviceName(), userAgent);
        if (sameDeviceLike) {
            ensureRecentPasswordAuth(employee, "Recent password authentication required for re-enrollment");
            return createRegistrationRequest(employee, purpose);
        }

        // New/unrecognized device => create pending request, do not allow immediate registration
        enrollmentRequestService.createPendingReplacementRequest(
                employee,
                dto.getDeviceName(),
                userAgent,
                ipAddress
        );

        throw new BadRequestException("New device registration request submitted for admin approval");
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

    @Override
    @Transactional
    public void verifyAndSavePasskey(PasskeyRegisterVerifyDTO dto, String userAgent) {
        if (dto == null || dto.getCredentialJson() == null || dto.getCredentialJson().isBlank()) {
            throw new BadRequestException("Credential JSON is required");
        }

        if (dto.getDeviceName() == null || dto.getDeviceName().isBlank()) {
            throw new BadRequestException("Device name is required");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        WebAuthnRegistrationRequest savedRequest = registrationRequestRepository
                .findTopByEmployeeAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        employee,
                        WebAuthnChallengePurpose.REGISTER,
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new BadRequestException("No valid registration request found"));

        long activeCount = passkeyCredentialRepository.countByEmployeeAndActiveTrue(employee);

        DeviceEnrollmentRequest approvedRequest = null;
        boolean sameDeviceReEnrollment = false;

        if (activeCount >= 1) {
            boolean sameDeviceLike = deviceRecognitionService.isSameDeviceLike(employee, dto.getDeviceName(), userAgent);

            if (sameDeviceLike) {
                sameDeviceReEnrollment = true;
                ensureRecentPasswordAuth(employee, "Recent password authentication required for re-enrollment");
            } else {
                approvedRequest = enrollmentRequestService.getApprovedValidRequest(employee);
                if (approvedRequest.getStatus() != DeviceEnrollmentRequestStatus.APPROVED) {
                    throw new BadRequestException("Approved enrollment request required");
                }
            }
        } else {
            ensureRecentPasswordAuth(employee, "Recent password authentication required");
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


            if (approvedRequest != null) {
                revokeOtherActiveCredentials(
                        employee,
                        newCredential.getCredentialId(),
                        "Replaced by approved new device enrollment"
                );

                approvedRequest.setStatus(DeviceEnrollmentRequestStatus.COMPLETED);
                approvedRequest.setCompletedAt(LocalDateTime.now());
            }

            // Same-device re-enrollment flow
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

    @Override
    public String getPasskeyAssertionResponse(WebAuthnChallengePurpose purpose) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        try {
            AssertionRequest request = relyingParty.startAssertion(
                    StartAssertionOptions.builder()
                            .username(employee.getEmail())
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
        if (dto == null || dto.getCredentialJson() == null || dto.getCredentialJson().isBlank()) {
            throw new BadRequestException("Credential JSON is required");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        WebAuthnAssertionRequest savedRequest = assertionRequestRepository
                .findTopByEmployeeAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        employee,
                        WebAuthnChallengePurpose.AUTHENTICATE,
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
}