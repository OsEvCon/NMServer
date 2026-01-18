package service;

import DTO.request.LoginRequest;
import DTO.request.RefreshRequest;
import DTO.request.RegisterRequest;
import DTO.response.AuthResponse;
import DTO.response.RefreshResponse;
import DTO.response.RegisterResponse;
import exception.AuthException;
import exception.BusinessException;
import exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.Master;
import model.MasterRepository;
import model.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private RoleRepository  roleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.secretKeySalt}")
    private String salt;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        String userEmail = loginRequest.getUserEmail();
        String password = loginRequest.getPassword();

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userEmail, password)
            );

            // Получаем текущий userDetails
            final UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // Получаем текущего мастера
            Master master = masterRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Мастер с email : %s не найден".formatted(userEmail)));

            if (master.getSecretKey() == null || master.getSecretKey().isBlank()) {
                throw new BusinessException("У мастера с ID: %s отсутствует secret key".formatted(master.getId()));
            }

            //Формируем токены
            String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

        log.debug("Успешный вход пользователя {}", userEmail);

        // Устанавливаем новый Authentication в SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

            // Формируем ответ
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .secretKey(master.getSecretKey())
                    .build();
    }

    @Transactional
    public RegisterResponse registerUser(RegisterRequest registerRequest) {
        log.debug("Регистрация клиента с email: {}", registerRequest.getEmail());

        if (masterRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            log.warn("Пользователь с почтой {} уже существует", registerRequest.getEmail());
            throw new BusinessException("Пользователь с почтой: %s уже существует".formatted(registerRequest.getEmail()));
        }

        Master masterForSave = createMasterFromRequest(registerRequest);

        masterRepository.save(masterForSave);

        log.info("Зарегистрирован пользователь с email: {} и id: {}", registerRequest.getEmail(), masterForSave.getId() );

        return new RegisterResponse(
                masterForSave.getSecretKey(),
                "Пользователь с почтой: %s зарегистрирован"
                        .formatted(masterForSave.getEmail())
        );
    }

    public RefreshResponse refresh(RefreshRequest request) {
        // Валидация refreshToken
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            log.warn("Refresh token не действителен");
            throw new AuthException("Refresh token не действителен");
        }

        if (!jwtUtil.isRefreshToken(request.getRefreshToken())) {
            log.warn("Токен в запросе не является RefreshToken");
            throw new AuthException("Токен в запросе не является RefreshToken");
        }

        String userEmail = jwtUtil.extractUserEmail(request.getRefreshToken());
        log.debug("Запрос на обновление accessToken от пользователя: {}", userEmail);

        // Проверка, что пользователь существует
        masterRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("Пользователь %s не найден".formatted(userEmail)));

        String newAccessToken = jwtUtil.generateAccessToken(userEmail);

        return new RefreshResponse(newAccessToken,
                "AccessToken для пользователя %s успешно обновлен"
                        .formatted(userEmail));
    }

    private Master createMasterFromRequest(RegisterRequest registerRequest) {
        Master masterForSave = new Master();
        masterForSave.setName(registerRequest.getUsername());
        masterForSave.setEmail(registerRequest.getEmail());
        masterForSave.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        masterForSave.setSecretKey(generateSecretKey(registerRequest.getEmail()));
        masterForSave.getRoles().add(roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Роль ROLE_USER не найдена"))
        );
        return masterForSave;
    }

    private String generateSecretKey(String userEmail) {
        return SecretKeyGenerator.keyToString(SecretKeyGenerator.generateKeyFromEmail(userEmail, salt));
    }
}
