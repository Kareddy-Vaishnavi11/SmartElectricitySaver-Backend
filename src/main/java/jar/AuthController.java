package jar;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody AuthRequest request) {

        Map<String, String> response = new HashMap<>();

        if (request.getEmail() == null
                || request.getPassword() == null
                || request.getEmail().isBlank()
                || request.getPassword().isBlank()) {

            response.put("error", "Email and password are required.");

            return ResponseEntity.badRequest().body(response);
        }

        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();

        if (password.length() < 6) {
            response.put("error",
                    "Password must contain at least 6 characters.");

            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            response.put("error", "This email is already registered.");

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(response);
        }

        String encodedPassword = passwordEncoder.encode(password);

        User user = new User(email, encodedPassword);
        userRepository.save(user);

        response.put("message", "Registration successful.");
        response.put("email", email);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Log in an existing user
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody AuthRequest request) {

        Map<String, String> response = new HashMap<>();

        if (request.getEmail() == null
                || request.getPassword() == null
                || request.getEmail().isBlank()
                || request.getPassword().isBlank()) {

            response.put("error", "Email and password are required.");

            return ResponseEntity.badRequest().body(response);
        }

        String email = request.getEmail().trim();

        Optional<User> optionalUser =
                userRepository.findByEmailIgnoreCase(email);

        if (optionalUser.isEmpty()
                || !passwordEncoder.matches(
                        request.getPassword(),
                        optionalUser.get().getPassword())) {

            response.put("error", "Invalid email or password.");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        response.put("message", "Login successful.");
        response.put("email", optionalUser.get().getEmail());

        return ResponseEntity.ok(response);
    }

    // Data received from the frontend
    public static class AuthRequest {

        private String email;
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