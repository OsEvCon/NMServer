package service;

import DTO.request.LoginRequest;
import DTO.response.AuthResponse;
import Service.AuthService;
import Service.ClientService;
import Service.CustomUserDetailsService;
import Service.JwtUtil;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import model.Master;
import model.MasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    private String email;
    private String password;
    private LoginRequest loginRequest;

    @Mock
    MasterRepository masterRepository;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtUtil jwtUtil;

    @Mock
    CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void setUpLogging() {
        // Включаем DEBUG для конкретного логгера
        Logger logger = (Logger) LoggerFactory.getLogger(AuthService.class);
        logger.setLevel(Level.DEBUG);
    }

    @BeforeEach
    void setUp() {
        email = "testEmail@mail.ru";
        password = "testPassword";
        loginRequest = new LoginRequest(email, password);
    }

    /**
     * Тест успешной аутентификации с валидными данными
     * Должен возвращаться корректный ответ
     * Токены должны быть сгенерированы
     */
    @Test
    void loginTest_ValidCredentials() {
        Authentication authToken = new UsernamePasswordAuthenticationToken(email, password);

        //Создание успешного Authentication объекта
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                email,
                password,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        //Мок вызова аутентификации
        when(authenticationManager.authenticate(eq(authToken)))
                .thenReturn(authenticated);

        //Создание мастера для теста и мок репозитория
        Master testMaster = new Master();
        testMaster.setEmail(email);
        testMaster.setPassword(password);
        testMaster.setSecretKey("testSecretKey");
        when(masterRepository.findByEmail(email))
                .thenReturn(Optional.of(testMaster));

        when(customUserDetailsService.loadUserByUsername(email))
                .thenReturn(new User(testMaster.getEmail(), testMaster.getPassword(), authenticated.getAuthorities()));

        when(jwtUtil.generateAccessToken(email))
                .thenReturn("testAccessToken");

        when(jwtUtil.generateRefreshToken(email))
                .thenReturn("testRefreshToken");

        AuthResponse authResponse = authService.login(loginRequest);

        assertThat(authResponse.getAccessToken()).isEqualTo("testAccessToken");
        assertThat(authResponse.getRefreshToken()).isEqualTo("testRefreshToken");
        assertThat(authResponse.getSecretKey()).isEqualTo("testSecretKey");
    }
}
