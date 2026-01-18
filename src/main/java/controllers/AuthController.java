package controllers;

import DTO.request.LoginRequest;
import DTO.request.RefreshRequest;
import DTO.request.RegisterRequest;
import DTO.response.AuthResponse;
import DTO.response.RefreshResponse;
import DTO.response.RegisterResponse;
import service.AuthService;
import service.CustomUserDetailsService;
import service.JwtUtil;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import model.MasterRepository;
import model.RoleRepository;
import model.UpdateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController()
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthService authService;

    @Value("${app.current.version}")
    private String currentVersion;

    @Value("${app.download.url}")
    private String downloadUrl;

    @Value("${app.secretKeySalt}")
    private String salt;

    @PermitAll
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        log.debug("Запрос login пользователя с email : {}",  loginRequest.getUserEmail());
            AuthResponse authResponse = authService.login(loginRequest);
            return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        log.debug("Запрос на обновление accessToken");

        RefreshResponse response = authService.refresh(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@RequestBody @Valid RegisterRequest registerRequest) {
        log.debug("Запрос регистрации от пользователя с email: {}", registerRequest.getEmail());

        RegisterResponse registerResponse = authService.registerUser(registerRequest);

        log.debug("Пользователь с email : {} зарегистрирован", registerRequest.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);
    }

    @GetMapping("/checkUpdate")
    public ResponseEntity<UpdateResponse> checkUpdate(@RequestParam("version") String clientVersion){

        boolean updateNeeded = compareVersions(clientVersion, currentVersion);

        UpdateResponse response = new UpdateResponse();
        response.setUpdateNeeded(updateNeeded);

        if (updateNeeded){
            response.setLatestVersion(currentVersion);
            response.setDownloadUrl(downloadUrl);
            response.setForceUpdate(true);
        }
        System.out.println("Запрос обновления. Ответ: " + response.isUpdateNeeded());
        return ResponseEntity.ok(response);
    }

    private boolean compareVersions(String clientVersion, String serverVersion) {
        // Простая реализация сравнения версий (формат X.Y.Z)
        String[] clientParts = clientVersion.split("\\.");
        String[] serverParts = serverVersion.split("\\.");

        for (int i = 0; i < Math.max(clientParts.length, serverParts.length); i++) {
            int clientPart = i < clientParts.length ? Integer.parseInt(clientParts[i]) : 0;
            int serverPart = i < serverParts.length ? Integer.parseInt(serverParts[i]) : 0;

            if (clientPart < serverPart) return true;
            if (clientPart > serverPart) return false;
        }
        return false;
    }

}
