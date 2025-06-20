package controllers;

import Service.CustomUserDetailsService;
import Service.JwtUtil;
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

    @PermitAll
    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> user){
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
            System.out.println("успешный вход " + userDetails.getUsername());
            return "ok " + jwtUtil.generateToken(userDetails.getUsername());
        } catch (Exception e) {
            System.out.println("login error " + e.getMessage());
            e.printStackTrace();
            return "AuthError";
        }
    }

    @GetMapping("/pingServer")
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/registerUser")
    public ResponseEntity<ResponseBody> registerUser(@RequestBody Map<String, String> user) {
        try {
            System.out.println("запрос registerUser");
            String name = user.get("name");
            String email = user.get("email");
            String password = passwordEncoder.encode(user.get("password"));

            Optional<Master> optionalMaster = masterRepository.findByEmail(email);
            if (optionalMaster.isPresent()){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            } else {
                Master master = new Master();
                master.setName(name);
                master.setEmail(email);
                master.setPassword(password);
                master.getRoles().add(roleRepository.findByName("ROLE_USER").get());
                masterRepository.save(master);
                return ResponseEntity.status(HttpStatus.CREATED).build();
            }

        } catch (Exception e) {
            // Обработка других исключений
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
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

    /*@PostMapping("/secretKey")
    private String saveSecretKey(@RequestBody SecretKey secretKey){

    }*/
}
