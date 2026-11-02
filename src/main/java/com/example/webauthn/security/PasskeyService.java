package com.example.webauthn.security;

import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Lists and removes the passkeys of a user. Registration and authentication are handled
 * by Spring Security's WebAuthn filters, called from the browser (see webauthn.js).
 */
@Service
public class PasskeyService {

    public record Passkey(String id, String label, Instant created, Instant lastUsed, long signatureCount) {
    }

    private final PublicKeyCredentialUserEntityRepository userEntities;
    private final UserCredentialRepository userCredentials;

    public PasskeyService(PublicKeyCredentialUserEntityRepository userEntities,
                          UserCredentialRepository userCredentials) {
        this.userEntities = userEntities;
        this.userCredentials = userCredentials;
    }

    public List<Passkey> findPasskeys(String username) {
        PublicKeyCredentialUserEntity userEntity = userEntities.findByUsername(username);
        if (userEntity == null) {
            return List.of();
        }
        return userCredentials.findByUserId(userEntity.getId()).stream()
                .map(PasskeyService::toPasskey)
                .sorted(Comparator.comparing(Passkey::created))
                .toList();
    }

    public void deletePasskey(String username, String credentialId) {
        PublicKeyCredentialUserEntity userEntity = userEntities.findByUsername(username);
        if (userEntity == null) {
            return;
        }
        Bytes id = Bytes.fromBase64(credentialId);
        CredentialRecord credential = userCredentials.findByCredentialId(id);
        // Only the owner may delete a passkey
        if (credential != null && credential.getUserEntityUserId().equals(userEntity.getId())) {
            userCredentials.delete(id);
        }
    }

    private static Passkey toPasskey(CredentialRecord credential) {
        return new Passkey(credential.getCredentialId().toBase64UrlString(), credential.getLabel(),
                credential.getCreated(), credential.getLastUsed(), credential.getSignatureCount());
    }
}
