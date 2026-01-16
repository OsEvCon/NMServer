package service;

import DTO.request.LoginRequest;
import DTO.response.AuthResponse;
import Service.AuthService;
import Service.ClientService;
import Service.CustomUserDetailsService;
import Service.JwtUtil;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import model.Master;
import model.MasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    private String email;
    private String password;
    private LoginRequest loginRequest;

    @Mock
    private ClientService clientService;

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

    /**
     * Тест проваленной аутентификации
     * Метод должен завершаться с исключением
     */
    @ParameterizedTest
    @MethodSource("authExceptions")
    void loginTest_AuthFails(Class<? extends Exception> exceptionClass) {
        when(authenticationManager.authenticate(any()))
                .thenThrow(exceptionClass);

        assertThrows(exceptionClass, () -> authService.login(loginRequest));
    }

    /**
     * Тест на ошибку БД при поиске мастера
     * Метод должен завершаться с исключением
     * Аутентификация не должна быть пройдена
     */
    @Test
    void loginTest_DBError() {
        Authentication authToken = new UsernamePasswordAuthenticationToken(email, password);

        //Создание успешного Authentication объекта
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                email,
                password,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        //Очистка SecurityContext перед тестом
        SecurityContextHolder.clearContext();

        //Мок вызова аутентификации
        when(authenticationManager.authenticate(eq(authToken)))
                .thenReturn(authenticated);

        // Мок ошибки БД
        when(masterRepository.findByEmail(email))
                .thenThrow(new DataAccessException("Database connection lost") {});

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(DataAccessException.class)
                .hasMessage("Database connection lost");

        //Проверка, что SecurityContext остался чистым
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtUtil, never()).generateAccessToken(any());
        verify(jwtUtil, never()).generateRefreshToken(any());

    }

    /**
     * Тест на ошибку генерации JWT токенов
     * Метод должен завершаться с исключением
     * Аутентификация не должна быть пройдена
     */
    @ParameterizedTest
    @MethodSource("jwtExceptions")
    void loginTest_JwtError(JwtException exception) {
        Authentication authToken = new UsernamePasswordAuthenticationToken(email, password);

        //Создание успешного Authentication объекта
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                email,
                password,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        //Очистка SecurityContext перед тестом
        SecurityContextHolder.clearContext();

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
                .thenThrow(exception);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isSameAs(exception);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // Ошибки аутентификации
    private static Stream<Arguments> authExceptions() {
        return Stream.of(
                Arguments.arguments(BadCredentialsException.class),
                Arguments.arguments(LockedException.class),
                Arguments.arguments(DisabledException.class),
                Arguments.arguments(CredentialsExpiredException.class)
        );
    }

    // Ошибки генерации JWT
    private static Stream<JwtException> jwtExceptions() {
        return Stream.of(
                new SignatureException("Invalid signature"),
                new MalformedJwtException("Malformed JWT"),
                new JwtException("General JWT error")
        );
    }
}
