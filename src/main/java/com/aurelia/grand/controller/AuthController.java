package com.aurelia.grand.controller;

import com.aurelia.grand.model.User;
import com.aurelia.grand.model.StaffProfile;
import com.aurelia.grand.repository.UserRepository;
import com.aurelia.grand.repository.StaffProfileRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.time.LocalTime;
import java.util.*;

@RestController
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @PostMapping("/login.php")
    public ResponseEntity<?> login(
        @RequestParam(value = "email", required = false) String email,
        @RequestParam(value = "password", required = false) String password,
        HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();

        if (email == null || password == null || email.trim().isEmpty() || password.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email and password are required.");
            return ResponseEntity.ok(response);
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid email or password.");
            return ResponseEntity.ok(response);
        }

        User user = userOpt.get();
        if (user.getIsActive() != 1) {
            response.put("success", false);
            response.put("message", "Invalid email or password.");
            return ResponseEntity.ok(response);
        }

        String storedHash = user.getPassword();
        if (storedHash != null && storedHash.startsWith("$2y$")) {
            storedHash = "$2a$" + storedHash.substring(4);
        }
        if (!BCrypt.checkpw(password, storedHash)) {
            response.put("success", false);
            response.put("message", "Invalid email or password.");
            return ResponseEntity.ok(response);
        }

        // Set session
        session.setAttribute("logged_in", true);
        session.setAttribute("user_id", user.getId());
        session.setAttribute("user_name", user.getFirstName() + " " + user.getLastName());
        session.setAttribute("email", user.getEmail());
        session.setAttribute("role", user.getRole());

        response.put("success", true);
        response.put("message", "Login successful.");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getFirstName() + " " + user.getLastName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());
        response.put("user", userMap);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register.php")
    public ResponseEntity<?> register(
        @RequestParam(value = "first_name", required = false) String firstName,
        @RequestParam(value = "last_name", required = false) String lastName,
        @RequestParam(value = "email", required = false) String email,
        @RequestParam(value = "password", required = false) String password,
        @RequestParam(value = "role", defaultValue = "guest") String role,
        @RequestParam(value = "phone", defaultValue = "") String phone,
        HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        List<String> errors = new ArrayList<>();

        if (firstName == null || firstName.trim().isEmpty()) errors.add("First name is required.");
        if (lastName == null || lastName.trim().isEmpty()) errors.add("Last name is required.");
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) errors.add("Valid email is required.");
        if (password == null || password.length() < 8) errors.add("Password must be at least 8 characters.");

        if (!role.equals("guest") && !role.equals("staff")) {
            role = "guest";
        }

        if (!errors.isEmpty()) {
            response.put("success", false);
            response.put("errors", errors);
            return ResponseEntity.ok(response);
        }

        Optional<User> checkUser = userRepository.findByEmail(email.trim());
        if (checkUser.isPresent()) {
            response.put("success", false);
            response.put("message", "Email already registered.");
            return ResponseEntity.ok(response);
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(email.trim());
        user.setPassword(hashedPassword);
        user.setRole(role);
        user.setPhone(phone.trim());

        User savedUser = userRepository.save(user);

        if (role.equals("staff")) {
            StaffProfile profile = new StaffProfile();
            profile.setUser(savedUser);
            profile.setDepartment("Housekeeper");
            profile.setZone("All Floors");
            profile.setShiftStart(LocalTime.of(8, 0));
            profile.setShiftEnd(LocalTime.of(16, 0));
            profile.setOnDuty(1);
            staffProfileRepository.save(profile);
        }

        // Set session
        session.setAttribute("logged_in", true);
        session.setAttribute("user_id", savedUser.getId());
        session.setAttribute("user_name", savedUser.getFirstName() + " " + savedUser.getLastName());
        session.setAttribute("email", savedUser.getEmail());
        session.setAttribute("role", savedUser.getRole());

        response.put("success", true);
        response.put("message", "Account created!");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", savedUser.getId());
        userMap.put("name", savedUser.getFirstName() + " " + savedUser.getLastName());
        userMap.put("email", savedUser.getEmail());
        userMap.put("role", savedUser.getRole());
        response.put("user", userMap);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/logout.php", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get_session.php")
    public ResponseEntity<?> getSession(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Boolean loggedIn = (Boolean) session.getAttribute("logged_in");

        if (loggedIn != null && loggedIn) {
            response.put("logged_in", true);
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", session.getAttribute("user_id"));
            userMap.put("name", session.getAttribute("user_name"));
            userMap.put("email", session.getAttribute("email"));
            userMap.put("role", session.getAttribute("role"));
            response.put("user", userMap);
        } else {
            response.put("logged_in", false);
        }

        return ResponseEntity.ok(response);
    }
}
