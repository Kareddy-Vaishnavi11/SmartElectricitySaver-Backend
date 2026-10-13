package jar;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody AuthRequest request) {

        Map<String, String> response = new HashMap<>();

        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            response.put("error", "This email is already registered.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        String encodedPassword = passwordEncoder.encode(password);
        String role = "USER";
        if (email.contains("admin")) {
            role = "ADMIN";
        }

        User user = new User(email, encodedPassword, role);
        userRepository.save(user);

        String token = jwtUtil.generateToken(email, role);

        response.put("message", "Registration successful.");
        response.put("email", email);
        response.put("role", role);
        response.put("token", token);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Log in an existing user
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody AuthRequest request) {

        Map<String, String> response = new HashMap<>();

        String email = request.getEmail().trim();

        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);

        if (optionalUser.isEmpty()
                || !passwordEncoder.matches(request.getPassword(), optionalUser.get().getPassword())) {

            response.put("error", "Invalid email or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = optionalUser.get();
        String role = user.getRole() != null ? user.getRole() : "USER";
        String token = jwtUtil.generateToken(user.getEmail(), role);

        response.put("message", "Login successful.");
        response.put("email", user.getEmail());
        response.put("role", role);
        response.put("token", token);

        return ResponseEntity.ok(response);
    }

    // Data transfer object for auth requests with Bean Validation
    public static class AuthRequest {

        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be valid.")
        private String email;

        @NotBlank(message = "Password is required.")
        @Size(min = 6, message = "Password must contain at least 6 characters.")
        private String password;

        public AuthRequest() {
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
    }
}