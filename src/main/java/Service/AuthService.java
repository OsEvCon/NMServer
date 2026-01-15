package Service;

import DTO.request.LoginRequest;
import DTO.response.AuthResponse;
import exception.AuthException;
import exception.BusinessException;
import exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.Master;
import model.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        String userEmail = loginRequest.getUserEmail();
        String password = loginRequest.getPassword();

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userEmail, password)
            );

            // Устанавливаем новый Authentication в SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Получаем текущий userDetails
            final UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // Получаем текущего мастера
            Master master = masterRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Мастер с email : %s не найден".formatted(userEmail)));

            if (master.getSecretKey() == null || master.getSecretKey().isBlank()) {
                throw new BusinessException("У мастера с ID: %s отсутствует secret key".formatted(master.getId()));
            }

            log.debug("Успешный вход пользователя {}", userEmail);

            //Формируем токены
            String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

            // Формируем ответ
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .secretKey(master.getSecretKey())
                    .build();
    }
}
