package com.ryanbondoc.fintech.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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

class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
private CustomerServiceClient customerServiceClient;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        authService = new AuthServiceImpl(
        userRepository,
        passwordEncoder,
        jwtService,
        customerServiceClient
);
    }

    // ============================================================
    // AUTH-001 - REGISTER USER
    // ============================================================

    @Test
void register_shouldCreateUser() {

    // Given
    RegisterRequest request = new RegisterRequest("ryan@example.com", "SecurePassword123",
            "Ryan",
            "Bondoc"
            
            
    );

    UUID generatedId = UUID.randomUUID();

    when(userRepository.existsByEmailIgnoreCase("ryan@example.com"))
            .thenReturn(false);

    when(passwordEncoder.encode("SecurePassword123"))
            .thenReturn("$2a$10$hashedPassword");

    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);

                // Simulate JPA-generated ID.
                setId(user, generatedId);

                return user;
            });

    // When
    RegisterResponse response =
            authService.register(request);

    // Then
    assertThat(response).isNotNull();

    assertThat(response.id())
            .isEqualTo(generatedId);

    assertThat(response.email())
            .isEqualTo("ryan@example.com");

    assertThat(response.status())
            .isEqualTo(UserStatus.ACTIVE);

    assertThat(response.createdAt())
            .isNotNull();

    verify(userRepository)
            .save(any(User.class));

    verify(customerServiceClient)
            .createCustomer(
                    generatedId,
                    "Ryan",
                    "Bondoc",
                    "ryan@example.com"
            );
}

@Test
void register_shouldCreateCustomerUsingGeneratedUserId() {

    // Given
    

    RegisterRequest request = new RegisterRequest("ryan@example.com", "SecurePassword123",
            "Ryan",
            "Bondoc"
            
            
    );

    UUID userId = UUID.randomUUID();

    when(userRepository.existsByEmailIgnoreCase("ryan@example.com"))
            .thenReturn(false);

    when(passwordEncoder.encode("SecurePassword123"))
            .thenReturn("$2a$10$hashedPassword");

    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                setId(user, userId);
                return user;
            });

    // When
    authService.register(request);

    // Then
    verify(customerServiceClient)
            .createCustomer(
                    eq(userId),
                    eq("Ryan"),
                    eq("Bondoc"),
                    eq("ryan@example.com")
            );
}

@Test
void register_shouldNotCreateCustomerWhenEmailAlreadyExists() {

    // Given
    RegisterRequest request = new RegisterRequest("ryan@example.com", "SecurePassword123",
            "Ryan",
            "Bondoc"
            
            
    );

    when(userRepository.existsByEmailIgnoreCase("ryan@example.com"))
            .thenReturn(true);

    // When / Then
    assertThatThrownBy(
            () -> authService.register(request)
    )
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessage(
                    "Email already registered: ryan@example.com"
            );

    verify(userRepository, never())
            .save(any(User.class));

    verify(passwordEncoder, never())
            .encode(any(String.class));

    verify(customerServiceClient, never())
            .createCustomer(
                    any(UUID.class),
                    any(String.class),
                    any(String.class),
                    any(String.class)
            );
}

    @Test
    void register_shouldGenerateUserId() {

        // Given
        

        RegisterRequest request = new RegisterRequest("ryan@example.com", "SecurePassword123",
            "Ryan",
            "Bondoc"
            
            
    );

        UUID generatedId = UUID.randomUUID();

        when(userRepository.existsByEmailIgnoreCase("ryan@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("SecurePassword123"))
                .thenReturn("$2a$10$hashedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);

                    setId(user, generatedId);

                    return user;
                });

        // When
        RegisterResponse response =
                authService.register(request);

        // Then
        assertThat(response.id())
                .isNotNull();

        assertThat(response.id())
                .isEqualTo(generatedId);
    }

    @Test
    void register_shouldHashPasswordBeforeSaving() {

        // Given
        String plaintextPassword = "SecurePassword123";
        String passwordHash = "$2a$10$hashedPassword";



        RegisterRequest request = new RegisterRequest("ryan@example.com", plaintextPassword,
            "Ryan",
            "Bondoc"
            
            
    );

        when(userRepository.existsByEmailIgnoreCase("ryan@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(plaintextPassword))
                .thenReturn(passwordHash);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        // When
        authService.register(request);

        // Then
        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPasswordHash())
                .isEqualTo(passwordHash);

        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo(plaintextPassword);

        verify(passwordEncoder)
                .encode(plaintextPassword);
    }

    @Test
    void register_shouldNormalizeEmail() {

        // Given
        

        RegisterRequest request = new RegisterRequest("  RYAN@EXAMPLE.COM  ", "SecurePassword123",
            "Ryan",
            "Bondoc"
            
            
    );

        when(userRepository.existsByEmailIgnoreCase("ryan@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("SecurePassword123"))
                .thenReturn("$2a$10$hashedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        // When
        authService.register(request);

        // Then
        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail())
                .isEqualTo("ryan@example.com");
    }

    @Test
    void register_shouldRejectDuplicateEmail() {

        // Given
        

        RegisterRequest request = new RegisterRequest("  RYAN@EXAMPLE.COM  ", "SecurePassword123",
            "Ryan",
            "Bondoc"
            
            
    );

        when(userRepository.existsByEmailIgnoreCase("ryan@example.com"))
                .thenReturn(true);

        // When / Then
        assertThatThrownBy(
                () -> authService.register(request)
        )
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage(
                        "Email already registered: ryan@example.com"
                );

        verify(userRepository, never())
                .save(any(User.class));

        verify(passwordEncoder, never())
                .encode(any(String.class));
    }

    @Test
    void register_shouldCreateUserWithActiveStatus() {

        // Given


        RegisterRequest request = new RegisterRequest("ryan@example.com", "SecurePassword123",
            "Ryan",
            "Bondoc"
            
            
    );

        when(userRepository.existsByEmailIgnoreCase("ryan@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("SecurePassword123"))
                .thenReturn("$2a$10$hashedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        // When
        authService.register(request);

        // Then
        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void register_shouldStorePasswordAsHash() {

        // Given
        PasswordEncoder realPasswordEncoder =
                new BCryptPasswordEncoder();

        AuthServiceImpl service = new AuthServiceImpl(
        userRepository,
        realPasswordEncoder,
        jwtService,
        customerServiceClient
);



                RegisterRequest request = new RegisterRequest("ryan@example.com", "SecurePassword123",
            "Ryan",
            "Bondoc"
            
            
    );

        when(userRepository.existsByEmailIgnoreCase(
                "ryan@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        // When
        service.register(request);

        // Then
        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(userCaptor.capture());

        User savedUser =
                userCaptor.getValue();

        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo("SecurePassword123");

        assertThat(
                realPasswordEncoder.matches(
                        "SecurePassword123",
                        savedUser.getPasswordHash()
                )
        ).isTrue();
    }

    // ============================================================
    // AUTH-002 - LOGIN
    // ============================================================

    @Test
    void login_shouldAuthenticateUserAndReturnAccessToken() {

        // Given
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        User user = new User(
                "ryan@example.com",
                "$2a$10$hashedPassword",
                UserStatus.ACTIVE,
                createdAt
        );

        setId(user, userId);

        LoginRequest request =
                new LoginRequest(
                        "ryan@example.com",
                        "SecurePassword123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "ryan@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePassword123",
                "$2a$10$hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("test-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(900L);

        // When
        LoginResponse response =
                authService.login(request);

        // Then
        assertThat(response).isNotNull();

        assertThat(response.accessToken())
                .isEqualTo("test-token");

        assertThat(response.tokenType())
                .isEqualTo("Bearer");

        assertThat(response.expiresIn())
                .isEqualTo(900);

        verify(userRepository)
                .findByEmailIgnoreCase("ryan@example.com");

        verify(passwordEncoder)
                .matches(
                        "SecurePassword123",
                        "$2a$10$hashedPassword"
                );

        verify(jwtService)
                .generateAccessToken(user);
    }

    @Test
    void login_shouldAuthenticateUsingUserId() {

        // Given
        UUID userId = UUID.randomUUID();

        User user = new User(
                "ryan@example.com",
                "$2a$10$hashedPassword",
                UserStatus.ACTIVE,
                Instant.now()
        );

        setId(user, userId);

        LoginRequest request =
                new LoginRequest(
                        "ryan@example.com",
                        "SecurePassword123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "ryan@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePassword123",
                "$2a$10$hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("test-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(900L);

        // When
        authService.login(request);

        // Then
        verify(jwtService)
                .generateAccessToken(
                        eq(user)
                );

        assertThat(user.getId())
                .isEqualTo(userId);
    }

    @Test
    void login_shouldVerifyPasswordUsingStoredHash() {

        // Given
        User user = new User(
                "ryan@example.com",
                "$2a$10$storedPasswordHash",
                UserStatus.ACTIVE,
                Instant.now()
        );

        LoginRequest request =
                new LoginRequest(
                        "ryan@example.com",
                        "SecurePassword123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "ryan@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePassword123",
                "$2a$10$storedPasswordHash"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("test-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(900L);

        // When
        authService.login(request);

        // Then
        verify(passwordEncoder)
                .matches(
                        eq("SecurePassword123"),
                        eq("$2a$10$storedPasswordHash")
                );
    }

    @Test
    void login_shouldNormalizeEmail() {

        // Given
        User user = new User(
                "ryan@example.com",
                "$2a$10$hashedPassword",
                UserStatus.ACTIVE,
                Instant.now()
        );

        LoginRequest request =
                new LoginRequest(
                        "  RYAN@EXAMPLE.COM  ",
                        "SecurePassword123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "ryan@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePassword123",
                "$2a$10$hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("test-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(900L);

        // When
        authService.login(request);

        // Then
        verify(userRepository)
                .findByEmailIgnoreCase(
                        "ryan@example.com"
                );
    }

    @Test
    void login_shouldRejectUnknownEmail() {

        // Given
        LoginRequest request =
                new LoginRequest(
                        "unknown@example.com",
                        "SecurePassword123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "unknown@example.com"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(
                () -> authService.login(request)
        )
                .isInstanceOf(
                        InvalidCredentialsException.class
                )
                .hasMessage(
                        "Invalid email or password"
                );

        verify(passwordEncoder, never())
                .matches(
                        any(String.class),
                        any(String.class)
                );

        verify(jwtService, never())
                .generateAccessToken(any(User.class));
    }

    @Test
    void login_shouldRejectIncorrectPassword() {

        // Given
        User user = new User(
                "ryan@example.com",
                "$2a$10$hashedPassword",
                UserStatus.ACTIVE,
                Instant.now()
        );

        LoginRequest request =
                new LoginRequest(
                        "ryan@example.com",
                        "WrongPassword123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "ryan@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword123",
                "$2a$10$hashedPassword"
        )).thenReturn(false);

        // When / Then
        assertThatThrownBy(
                () -> authService.login(request)
        )
                .isInstanceOf(
                        InvalidCredentialsException.class
                )
                .hasMessage(
                        "Invalid email or password"
                );

        verify(passwordEncoder)
                .matches(
                        "WrongPassword123",
                        "$2a$10$hashedPassword"
                );

        verify(jwtService, never())
                .generateAccessToken(any(User.class));
    }

    @Test
    void login_shouldRejectInactiveUser() {

        // Given
        User user = new User(
                "ryan@example.com",
                "$2a$10$hashedPassword",
                UserStatus.DISABLED,
                Instant.now()
        );

        LoginRequest request =
                new LoginRequest(
                        "ryan@example.com",
                        "SecurePassword123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "ryan@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePassword123",
                "$2a$10$hashedPassword"
        )).thenReturn(true);

        // When / Then
        assertThatThrownBy(
                () -> authService.login(request)
        )
                .isInstanceOf(
                        InvalidCredentialsException.class
                )
                .hasMessage(
                        "Invalid email or password"
                );

        verify(passwordEncoder)
                .matches(
                        "SecurePassword123",
                        "$2a$10$hashedPassword"
                );

        verify(jwtService, never())
                .generateAccessToken(any(User.class));
    }

    @Test
    void login_shouldReturnConfiguredTokenExpiration() {

        // Given
        User user = new User(
                "ryan@example.com",
                "$2a$10$hashedPassword",
                UserStatus.ACTIVE,
                Instant.now()
        );

        LoginRequest request =
                new LoginRequest(
                        "ryan@example.com",
                        "SecurePassword123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "ryan@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePassword123",
                "$2a$10$hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("test-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(900L);

        // When
        LoginResponse response =
                authService.login(request);

        // Then
        assertThat(response.tokenType())
                .isEqualTo("Bearer");

        assertThat(response.expiresIn())
                .isEqualTo(900);

        assertThat(response.accessToken())
                .isEqualTo("test-token");
    }

    @Test
    void login_shouldGenerateJwtOnlyAfterSuccessfulAuthentication() {

        // Given
        User user = new User(
                "ryan@example.com",
                "$2a$10$hashedPassword",
                UserStatus.ACTIVE,
                Instant.now()
        );

        LoginRequest request =
                new LoginRequest(
                        "ryan@example.com",
                        "SecurePassword123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "ryan@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePassword123",
                "$2a$10$hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("test-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(900L);

        // When
        authService.login(request);

        // Then
        verify(jwtService)
                .generateAccessToken(user);
    }

    // ============================================================
    // TEST UTILITY
    // ============================================================

    /**
     * User.id is normally generated by JPA.
     *
     * Since this is a unit test and there is no JPA
     * persistence context, simulate the generated UUID.
     */
    private void setId(User user, UUID id) {

        try {
            var field =
                    User.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(user, id);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
void register_shouldPassNormalizedEmailToCustomerService() {

    // Given
    RegisterRequest request = new RegisterRequest("ryan@example.com", "SecurePassword123",
            "Ryan",
            "Bondoc"
            
            
    );


    
    UUID userId = UUID.randomUUID();

    when(userRepository.existsByEmailIgnoreCase(
            "ryan@example.com"
    )).thenReturn(false);

    when(passwordEncoder.encode("SecurePassword123"))
            .thenReturn("$2a$10$hashedPassword");

    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                setId(user, userId);
                return user;
            });

    // When
    authService.register(request);

    // Then
    verify(customerServiceClient)
            .createCustomer(
                    eq(userId),
                    eq("Ryan"),
                    eq("Bondoc"),
                    eq("ryan@example.com")
            );
}
}