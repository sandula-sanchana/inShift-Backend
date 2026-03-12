package edu.ijse.inshiftbackend.config;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import edu.ijse.inshiftbackend.webauthn.InShiftCredentialRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class WebAuthnConfig {

    @Bean
    public RelyingPartyIdentity relyingPartyIdentity() {
        return RelyingPartyIdentity.builder()
                .id("provocatively-televisional-wei.ngrok-free.dev")
                .name("InShift")
                .build();
    }

    @Bean
    public RelyingParty relyingParty(
            RelyingPartyIdentity relyingPartyIdentity,
            InShiftCredentialRepository credentialRepository
    ) {
        return RelyingParty.builder()
                .identity(relyingPartyIdentity)
                .credentialRepository(credentialRepository)
                .origins(Set.of(
                        "https://provocatively-televisional-wei.ngrok-free.dev"
                ))
                .build();
    }
}