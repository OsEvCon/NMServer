package controllers;

import DTO.RefreshRequest;
import Service.CustomUserDetailsService;
import Service.JwtUtil;
import Service.SecretKeyGenerator;
import com.mysql.cj.util.StringUtils;
import jakarta.annotation.security.PermitAll;
import model.Master;
import model.MasterRepository;
import model.RoleRepository;
import model.UpdateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    @Value("${app.current.version}")
    private String currentVersion;

    @Value("${app.download.url}")
    private String downloadUrl;

    @Value("${app.secretKeySalt}")
    private String salt;

    @PermitAll
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> user){
        System.out.println("запрос login");
        String userEmail = user.get("email");
        String password = user.get("password");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userEmail, password)
            );
            System.out.println("Before setting: " + SecurityContextHolder.getContext().getAuthentication());

            // Устанавливаем новый Authentication в SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("After setting: " + SecurityContextHolder.getContext().getAuthentication());

            final UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            Master master = masterRepository.findByEmail(userEmail).get();
            System.out.println("успешный вход " + userDetails.getUsername());

            String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

            Map<String, String> tokensAndSK = new HashMap<>();
            tokensAndSK.put("accessToken", accessToken);
            tokensAndSK.put("refreshToken", refreshToken);
            tokensAndSK.put("secretKey", master.getSecretKey());

            return ResponseEntity.ok(tokensAndSK);
        } catch (Exception e) {
            System.out.println("login error " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("AuthError");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        System.out.println("Запрос на refreshToken ");
        if (jwtUtil.validateToken(refreshToken)) {
            String username = jwtUtil.extractUserEmail(refreshToken);
            String newAccessToken = jwtUtil.generateAccessToken(username);

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", newAccessToken);

            return ResponseEntity.ok(response);
        } else {
            System.out.println("Запрос на refreshToken не прошел");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @GetMapping("/pingServer")
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/registerUser")
    public ResponseEntity<String> registerUser(@RequestBody Map<String, String> user) {
        try {
            System.out.println("запрос registerUser");
            String name = user.get("name");
            String email = user.get("email");
            String password = passwordEncoder.encode(user.get("password"));

            Optional<Master> optionalMaster = masterRepository.findByEmail(email);
            if (optionalMaster.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
            } else {
                SecretKey secretKey = SecretKeyGenerator.generateKeyFromEmail(email, salt);
                String stringKey = SecretKeyGenerator.keyToString(secretKey);
                Master master = new Master();
                master.setName(name);
                master.setEmail(email);
                master.setPassword(password);
                master.setSecretKey(stringKey);
                master.getRoles().add(roleRepository.findByName("ROLE_USER").get());
                masterRepository.save(master);

                // Возвращаем stringKey в теле ответа
                return ResponseEntity.status(HttpStatus.CREATED).body(stringKey);
            }

        } catch (Exception e) {
            // Обработка других исключений
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/checkUpdate")
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
