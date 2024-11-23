package model;

import Service.CustomUserDetailsService;
import Service.JwtUtil;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.Map;
import java.util.Optional;

@RestController
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

    @Autowired RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

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

   /* @PermitAll
    @PostMapping("/registerUser")
    public String registerUser(@RequestBody Map<String, String> user){
        System.out.println("запрос registerUser");
        String name = user.get("name");
        String email = user.get("email");
        String password = passwordEncoder.encode(user.get("password"));

        Optional<Master> optionalMaster = masterRepository.findByEmail(email);

        if (optionalMaster.isPresent()){
            return "mailError";
        } else {
            Master master = new Master();
            master.setName(name);
            master.setEmail(email);
            master.setPassword(password);
            master.getRoles().add(roleRepository.findByName("ROLE_USER").get());
            masterRepository.save(master);
            return "ok";
        }
    }*/

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
}
