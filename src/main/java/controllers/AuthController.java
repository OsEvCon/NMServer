package controllers;

import DTO.request.LoginRequest;
import DTO.request.RefreshRequest;
import DTO.request.RegisterRequest;
import DTO.response.AuthResponse;
import DTO.response.RefreshResponse;
import DTO.response.RegisterResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import service.AuthService;
import lombok.extern.slf4j.Slf4j;
import model.UpdateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
    public ResponseEntity<UpdateResponse> checkUpdate(
            @RequestParam ("version") @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$",
                                               message = "Версия должна быть в формате X.Y.Z")
            String clientVersion) {
        log.debug("Запрос обновления. Версия клиента: {}", clientVersion);

        UpdateResponse updateResponse = authService.checkUpdate(clientVersion);

        return ResponseEntity.ok(updateResponse);
    }

}
