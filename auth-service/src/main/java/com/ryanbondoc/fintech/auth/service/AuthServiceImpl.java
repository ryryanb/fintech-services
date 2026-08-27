package com.ryanbondoc.fintech.auth.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryanbondoc.fintech.auth.client.CustomerServiceClient;
import com.ryanbondoc.fintech.auth.dto.LoginRequest;
import com.ryanbondoc.fintech.auth.dto.LoginResponse;
import com.ryanbondoc.fintech.auth.dto.RegisterRequest;
import com.ryanbondoc.fintech.auth.dto.RegisterResponse;
import com.ryanbondoc.fintech.auth.entity.User;
import com.ryanbondoc.fintech.auth.enums.UserStatus;
import com.ryanbondoc.fintech.auth.exception.EmailAlreadyExistsException;
import com.ryanbondoc.fintech.auth.exception.InvalidCredentialsException;
import com.ryanbondoc.fintech.auth.repository.UserRepository;
import com.ryanbondoc.fintech.auth.security.JwtService;

@Service
public class AuthServiceImpl implements AuthService {

   private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;
private final JwtService jwtService;
private final CustomerServiceClient customerServiceClient;

    public AuthServiceImpl(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        CustomerServiceClient customerServiceClient) {

    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.customerServiceClient = customerServiceClient;
}

    @Override
@Transactional
public RegisterResponse register(RegisterRequest request) {

    String email = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

    if (userRepository.existsByEmailIgnoreCase(email)) {
        throw new EmailAlreadyExistsException(email);
    }

    String passwordHash =
            passwordEncoder.encode(request.password());

    User user = new User(
            email,
            passwordHash,
            UserStatus.ACTIVE,
            Instant.now()
    );

    User savedUser = userRepository.save(user);

    customerServiceClient.createCustomer(
            savedUser.getId(),
            request.firstName(),
            request.lastName(),
            savedUser.getEmail()
    );

    return new RegisterResponse(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getStatus(),
            savedUser.getCreatedAt()
    );
}

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        String accessToken =
                jwtService.generateAccessToken(user);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }
}