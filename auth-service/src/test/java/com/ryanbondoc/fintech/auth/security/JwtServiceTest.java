package com.ryanbondoc.fintech.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.ryanbondoc.fintech.auth.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

class JwtServiceTest {

    private static final long EXPIRATION_SECONDS = 900;

    @TempDir
    Path tempDir;

    private JwtService jwtService;
    private KeyPair keyPair;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = generateRsaKeyPair();

        Path privateKeyPath = tempDir.resolve("jwt-private.pem");
        Files.writeString(
                privateKeyPath,
                toPem(keyPair.getPrivate())
        );

        jwtService = new JwtService(
                privateKeyPath.toString(),
                EXPIRATION_SECONDS
        );

        userId = UUID.randomUUID();

        user = mock(User.class);
        when(user.getId()).thenReturn(userId);
    }

    @Test
    void shouldContainUserIdAsSubject() {
        // Given
        String expectedUserId = userId.toString();

        // When
        String token = jwtService.generateAccessToken(user);

        // Then
        Claims claims = parseToken(token);

        assertThat(claims.getSubject())
                .isEqualTo(expectedUserId);
    }

    @Test
    void shouldContainUserRole() {
        // Given
        String token = jwtService.generateAccessToken(user);

        // When
        Claims claims = parseToken(token);

        // Then
        assertThat(claims.get("roles"))
                .isNotNull();

        assertThat(claims.get("roles").toString())
                .contains("USER");
    }

    @Test
    void shouldContainIssuedAtTimestamp() {
        // Given
        String token = jwtService.generateAccessToken(user);

        // When
        Claims claims = parseToken(token);

        // Then
        assertThat(claims.getIssuedAt())
                .isNotNull();
    }

    @Test
    void shouldExpireAfter900Seconds() {
        // Given
        String token = jwtService.generateAccessToken(user);

        // When
        Claims claims = parseToken(token);

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        // Then
        assertThat(expiration.getTime() - issuedAt.getTime())
                .isEqualTo(EXPIRATION_SECONDS * 1000);
    }

    @Test
    void shouldProduceValidSignedJwt() {
        // Given
        String token = jwtService.generateAccessToken(user);

        // When
        Claims claims = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Then
        assertThat(claims)
                .isNotNull();

        assertThat(claims.getSubject())
                .isEqualTo(userId.toString());
    }

    @Test
    void shouldRejectTamperedJwt() {
        // Given
        String originalToken = jwtService.generateAccessToken(user);

        String tamperedToken = tamperWithPayload(originalToken);

        // When / Then
        assertThatThrownBy(() ->
                Jwts.parser()
                        .verifyWith(keyPair.getPublic())
                        .build()
                        .parseSignedClaims(tamperedToken)
        )
                .isInstanceOf(Exception.class);
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator =
                KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    private static String toPem(PrivateKey privateKey) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(privateKey.getEncoded());

        return """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(encoded);
    }

    private static String tamperWithPayload(String token) {
        String[] parts = token.split("\\.");

        String payload = new String(
                Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
        );

        String tamperedPayload = payload.replace(
                "\"USER\"",
                "\"ADMIN\""
        );

        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        tamperedPayload.getBytes(StandardCharsets.UTF_8)
                );

        return parts[0] + "." + encodedPayload + "." + parts[2];
    }
}