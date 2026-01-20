package serviceTest;

import DTO.request.LoginRequest;
import DTO.request.RefreshRequest;
import DTO.request.RegisterRequest;
import DTO.response.AuthResponse;
import DTO.response.RefreshResponse;
import DTO.response.RegisterResponse;
import exception.AuthException;
import exception.BusinessException;
import exception.ResourceNotFoundException;
import model.Role;
import model.RoleRepository;
import org.springframework.test.util.ReflectionTestUtils;
import service.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import model.Master;
import model.MasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private String userName;
    private String email;
    private String password;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private RefreshRequest  refreshRequest;

    @Mock
    private ClientService clientService;

    @Mock
    MasterRepository masterRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtUtil jwtUtil;

    @Mock
    CustomUserDetailsService customUserDetailsService;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

   /* @BeforeEach
    void setUpLogging() {
        // Включаем DEBUG для конкретного логгера
        Logger logger = (Logger) LoggerFactory.getLogger(AuthService.class);
        logger.setLevel(Level.DEBUG);
    }*/

    @BeforeEach
    void setUp() {
        userName = "testUser";
        email = "testEmail@mail.ru";
        password = "testPassword";
        loginRequest = new LoginRequest(email, password);
        registerRequest = new RegisterRequest(userName, password, email);
        refreshRequest = new RefreshRequest("testRefreshToken");

        // Инициализация salt перед тестами
        ReflectionTestUtils.setField(authService, "salt", "test-salt-value-12345");
    }

    @Nested
    @DisplayName("Login Tests")
    class Login {
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

    @Nested
    @DisplayName("Register Tests")
    class Register {

        /**
         * Тест успешной регистрации с валидными данными.
         * Тест должен завершаться без ошибок
         * masterRepository.save должен быть вызван
         * Должен возвращаться корректный RegisterResponse
         */
        @Test
        void registerTest_ValidCredentials() {
            when(masterRepository.findByEmail(email))
                    .thenReturn(Optional.empty());

            when(passwordEncoder.encode(registerRequest.getPassword()))
                    .thenReturn(registerRequest.getPassword());

            when(roleRepository.findByName("ROLE_USER"))
                    .thenReturn(Optional.of(new Role("ROLE_USER")));

            RegisterResponse registerResponse = authService.registerUser(registerRequest);

            verify(masterRepository).save(argThat(master ->
                    master.getEmail().equals(email) &&
                    master.getName().equals(userName) &&
                    !master.getRoles().isEmpty()
            ));

            assertThat(registerResponse.getMessage()).isEqualTo("Пользователь с почтой: testEmail@mail.ru зарегистрирован");
            assertThat(registerResponse.getSecretKey()).isNotNull();
        }

        /**
         * Тест регистрации уже существующего пользователя
         * Должно возникать исключение BusinessException
         * Метод masterRepository.save не должен вызываться
         */
        @Test
        void registerTest_DuplicateEmail() {
            when(masterRepository.findByEmail(email))
                    .thenReturn(Optional.of(new Master()));

            assertThatThrownBy(() -> authService.registerUser(registerRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Пользователь с почтой: testEmail@mail.ru уже существует");

            verify(masterRepository, never()).save(any());
        }

        /**
         * Тест регистрации с отсутствием роли ROLE_USER в БД
         * Должно возникать исключение ResourceNotFoundException
         * Метод masterRepository.save не должен вызываться
         */
        @Test
        void registerTest_NoRole() {
            when(masterRepository.findByEmail(email))
                    .thenReturn(Optional.empty());

            when(roleRepository.findByName("ROLE_USER"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerUser(registerRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Роль ROLE_USER не найдена");

            verify(masterRepository, never()).save(any());

        }

        /**
         * Тест на ошибку БД при сохранении нового пользователя
         * Метод должен завершаться с исключением
         */
        @Test
        void registerTest_DBError() {
            when(masterRepository.findByEmail(email))
                    .thenReturn(Optional.empty());

            when(masterRepository.save(any()))
                    .thenThrow(new DataAccessException("Database connection lost") {});

            when(roleRepository.findByName("ROLE_USER"))
                    .thenReturn(Optional.of(new Role("ROLE_USER")));

            assertThatThrownBy(() -> authService.registerUser(registerRequest))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("Database connection lost");
        }
    }

    @Nested
    @DisplayName("Refresh Tests")
    class Refresh {

        /**
         * Тест на обновление accessToken с валидными данными
         * Метод должен завершаться без ошибок
         * Должен формироваться корректный RefreshResponse
         */
        @Test
        void refreshTest_ValidCredentials() {

            when(jwtUtil.validateToken(refreshRequest.getRefreshToken()))
                    .thenReturn(true);

            when(jwtUtil.isRefreshToken(refreshRequest.getRefreshToken()))
                    .thenReturn(true);

            when(jwtUtil.extractUserEmail(refreshRequest.getRefreshToken()))
                    .thenReturn(email);

            when(jwtUtil.generateAccessToken(email))
                    .thenReturn("testNewAccessToken");

            when(masterRepository.findByEmail(email))
            .thenReturn(Optional.of(new Master()));


            RefreshResponse refreshResponse = authService.refresh(refreshRequest);

            assertThat(refreshResponse.getAccessToken()).isEqualTo("testNewAccessToken");
            assertThat(refreshResponse.getMessage()).isEqualTo(
                    "AccessToken для пользователя %s успешно обновлен"
                    .formatted(email));
        }

        /**
         * Тест на обновление AccessToken с не валидным RefreshToken
         * Должно возникать исключение AuthException("Refresh token не действителен")
         * Методы jwtUtil и masterRepository не должны вызываться
         */
        @Test
        void refreshTest_InvalidRefreshToken() {
            when(jwtUtil.validateToken(refreshRequest.getRefreshToken()))
            .thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(refreshRequest))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Refresh token не действителен");

            verify(jwtUtil, never()).isRefreshToken(refreshRequest.getRefreshToken());
            verify(jwtUtil, never()).extractUserEmail(refreshRequest.getRefreshToken());
            verify(masterRepository, never()).findByEmail(email);
            verify(jwtUtil, never()).generateAccessToken(email);
        }

        /**
         * Тест на обновление AccessToken, когда в запросе вместо RefreshToken приходит AccessToken
         * Должно возникать исключение AuthException("Токен в запросе не является RefreshToken")
         * Методы jwtUtil и masterRepository не должны вызываться
         */
        @Test
        void refreshTest_NotRefreshToken() {
            when(jwtUtil.validateToken(refreshRequest.getRefreshToken()))
                    .thenReturn(true);

            when(jwtUtil.isRefreshToken(refreshRequest.getRefreshToken()))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(refreshRequest))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Токен в запросе не является RefreshToken");

            verify(jwtUtil, never()).extractUserEmail(refreshRequest.getRefreshToken());
            verify(masterRepository, never()).findByEmail(email);
            verify(jwtUtil, never()).generateAccessToken(email);
        }

        /**
         * Тест на обновление AccessToken от не зарегистрированного пользователя
         * Должно возникать исключение ResourceNotFoundException("Пользователь %s не найден".formatted(userEmail)
         * jwtUtil.generateAccessToken(userEmail) не должен вызываться
         */
        @Test
        void refreshTest_NoRegisteredUser() {
            when(jwtUtil.validateToken(refreshRequest.getRefreshToken()))
                    .thenReturn(true);

            when(jwtUtil.isRefreshToken(refreshRequest.getRefreshToken()))
                    .thenReturn(true);

            when(jwtUtil.extractUserEmail(refreshRequest.getRefreshToken()))
                    .thenReturn(email);

            when(masterRepository.findByEmail(email))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(refreshRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Пользователь %s не найден".formatted(email));

            verify(jwtUtil, never()).generateAccessToken(email);
            verify(jwtUtil, times(1)).validateToken(refreshRequest.getRefreshToken());
            verify(jwtUtil, times(1)).isRefreshToken(refreshRequest.getRefreshToken());
            verify(jwtUtil, times(1)).extractUserEmail(refreshRequest.getRefreshToken());
        }
    }
}
