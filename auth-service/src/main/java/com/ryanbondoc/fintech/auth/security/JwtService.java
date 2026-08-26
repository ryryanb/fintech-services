package com.ryanbondoc.fintech.auth.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ryanbondoc.fintech.auth.entity.User;

import io.jsonwebtoken.Jwts;

@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${security.jwt.private-key-path}") String privateKeyPath,
            @Value("${security.jwt.expiration-seconds:900}") long expirationSeconds
    ) {
        this.privateKey = loadPrivateKey(privateKeyPath);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateAccessToken(User user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("roles", List.of("USER"))
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plusSeconds(expirationSeconds)
                        )
                )
                .signWith(privateKey)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private PrivateKey loadPrivateKey(String path) {

        try {
            String pem = Files.readString(Path.of(path));

            String privateKeyContent = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes =
                    Base64.getDecoder().decode(privateKeyContent);

            PKCS8EncodedKeySpec keySpec =
                    new PKCS8EncodedKeySpec(keyBytes);

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to load JWT private key",
                    e
            );
        }
    }
}