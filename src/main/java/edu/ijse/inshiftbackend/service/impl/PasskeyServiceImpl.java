package edu.ijse.inshiftbackend.service.impl;

import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.UserIdentity;
import edu.ijse.inshiftbackend.dto.PasskeyAssertionVerifyDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterVerifyDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import edu.ijse.inshiftbackend.entity.WebAuthnRegistrationRequest;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PasskeyCredentialRepository;
import edu.ijse.inshiftbackend.repository.WebAuthnAssertionRequestRepository;
import edu.ijse.inshiftbackend.repository.WebAuthnRegistrationRequestRepository;
import edu.ijse.inshiftbackend.service.PasskeyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasskeyServiceImpl implements PasskeyService {

    private final EmployeeRepository employeeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final WebAuthnRegistrationRequestRepository registrationRequestRepository;
    private final RelyingParty relyingParty;
    private final WebAuthnAssertionRequestRepository assertionRequestRepository;

    @Override
    public String getPasskeyRegisterResponse(WebAuthnChallengePurpose purpose) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        UserIdentity userIdentity = UserIdentity.builder()
                .name(employee.getEmail())
                .displayName(employee.getFullName())
                .id(new ByteArray(
                        java.nio.ByteBuffer.allocate(Long.BYTES)
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
    public void verifyAndSavePasskey(PasskeyRegisterVerifyDTO dto) {
        if (dto == null || dto.getCredentialJson() == null || dto.getCredentialJson().isBlank()) {
            throw new BadRequestException("Credential JSON is required");
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

            PasskeyCredential credential = PasskeyCredential.builder()
                    .employee(employee)
                    .credentialId(credentialId)
                    .publicKey(result.getPublicKeyCose().getBase64Url())
                    .signCount(result.getSignatureCount())
                    .deviceName(dto.getDeviceName())
                    .active(true)
                    .build();

            passkeyCredentialRepository.save(credential);

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
        return "";
    }

    @Override
    public void verifyPasskeyAssertion(PasskeyAssertionVerifyDTO dto) {

    }
}