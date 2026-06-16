package com.aurelia.grand.controller;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HashGeneratorController {

    @GetMapping(value = "/generate_hash.php", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> generateHash() {
        String password = "Admin@1234";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        String html = "<h2>Aurelia Grand — Admin Password Hash Generator</h2>"
                + "<p><strong>Password:</strong> " + password + "</p>"
                + "<p><strong>Hash:</strong></p>"
                + "<textarea style='width:100%;padding:10px;font-family:monospace'>" + hash + "</textarea>"
                + "<hr>"
                + "<p>Run this SQL in phpMyAdmin:</p>"
                + "<textarea style='width:100%;height:80px;padding:10px;font-family:monospace'>"
                + "UPDATE users SET password='" + hash + "' WHERE email='admin@aureliagrand.com';"
                + "</textarea>"
                + "<p style='color:red'><strong>⚠️ Delete this route or controller after use!</strong></p>";

        return ResponseEntity.ok(html);
    }
}
