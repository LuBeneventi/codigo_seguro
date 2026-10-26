package cl.duoc.api.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequestDto dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setRole("CLIENTE"); // Rol asignado exclusivamente por el servidor

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_MS = 15 * 60 * 1000; // 15 minutos
    private final java.util.Map<String, Integer> attemptsCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Long> lockTimeCache = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        long now = System.currentTimeMillis();

        if (email != null && lockTimeCache.containsKey(email)) {
            if (now < lockTimeCache.get(email)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Cuenta temporalmente bloqueada por reiterados intentos fallidos (lockAccount). Intente mas tarde."));
            } else {
                lockTimeCache.remove(email);
                attemptsCache.remove(email);
            }
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        String password = loginRequest.getPassword() != null ? loginRequest.getPassword() : loginRequest.getPasswordHash();

        if (userOpt.isEmpty() || password == null || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            if (email != null) {
                int attempts = attemptsCache.getOrDefault(email, 0) + 1;
                attemptsCache.put(email, attempts);
                if (attempts >= MAX_ATTEMPTS) {
                    lockTimeCache.put(email, now + LOCK_TIME_MS);
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(Map.of("error", "Cuenta bloqueada por exceso de intentos fallidos (maxAttempts superado)."));
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales invalidas"));
        }

        if (email != null) {
            attemptsCache.remove(email);
            lockTimeCache.remove(email);
        }

        User user = userOpt.get();
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole()
        ));
    }

    // BASELINE-VULN [SEC-REQ-007]: el endpoint /me expone la entidad User completa incluyendo el passwordHash
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                var claims = jwtService.parseAndValidateToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    return ResponseEntity.ok(userOpt.get());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token invalido o expirado"));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Cabecera Authorization no proporcionada"));
    }

    public static class LoginRequest {
        private String email;
        @JsonAlias({"passwordHash", "clave"})
        private String password;
        private String passwordHash;

        public LoginRequest() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }
    }
}
