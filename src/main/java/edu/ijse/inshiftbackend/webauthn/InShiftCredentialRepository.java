package edu.ijse.inshiftbackend.webauthn;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PasskeyCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InShiftCredentialRepository implements CredentialRepository {

    private final EmployeeRepository employeeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return passkeyCredentialRepository.findAllByEmployeeEmailAndActiveTrue(username)
                .stream()
                .map(cred -> PublicKeyCredentialDescriptor.builder()
                        .id(safeBase64Url(cred.getCredentialId()))
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return employeeRepository.findByEmail(username)
                .map(emp -> new ByteArray(
                        emp.getEmployeeId().toString().getBytes(StandardCharsets.UTF_8)
                ));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        try {
            String employeeIdStr = new String(userHandle.getBytes(), StandardCharsets.UTF_8);
            return employeeRepository.findById(Long.valueOf(employeeIdStr))
                    .map(Employee::getEmail);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        try {
            String employeeIdStr = new String(userHandle.getBytes(), StandardCharsets.UTF_8);

            return passkeyCredentialRepository
                    .findByCredentialIdAndEmployeeEmployeeIdAndActiveTrue(
                            credentialId.getBase64Url(),
                            Long.valueOf(employeeIdStr)
                    )
                    .map(this::toRegisteredCredential);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return passkeyCredentialRepository
                .findAllByCredentialIdAndActiveTrue(credentialId.getBase64Url())
                .stream()
                .map(this::toRegisteredCredential)
                .collect(Collectors.toSet());
    }

    private RegisteredCredential toRegisteredCredential(PasskeyCredential cred) {
        return RegisteredCredential.builder()
                .credentialId(safeBase64Url(cred.getCredentialId()))
                .userHandle(new ByteArray(
                        cred.getEmployee().getEmployeeId().toString().getBytes(StandardCharsets.UTF_8)
                ))
                .publicKeyCose(safeBase64Url(cred.getPublicKey()))
                .signatureCount(cred.getSignCount() == null ? 0L : cred.getSignCount())
                .build();
    }

    private ByteArray safeBase64Url(String value) {
        try {
            return ByteArray.fromBase64Url(value);
        } catch (Exception e) {
            throw new IllegalStateException("Stored passkey data has invalid Base64Url format", e);
        }
    }
}