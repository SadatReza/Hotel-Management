<?php
// generate_hash.php
// Open this in browser: http://localhost/aurelia-grand/generate_hash.php
// Copy the hash, paste into phpMyAdmin users table, then DELETE this file.

$password = 'Admin@1234'; // Change this to your desired admin password
$hash = password_hash($password, PASSWORD_BCRYPT);

echo "<h2>Aurelia Grand — Admin Password Hash Generator</h2>";
echo "<p><strong>Password:</strong> " . htmlspecialchars($password) . "</p>";
echo "<p><strong>Hash:</strong></p>";
echo "<textarea style='width:100%;padding:10px;font-family:monospace'>" . $hash . "</textarea>";
echo "<hr>";
echo "<p>Run this SQL in phpMyAdmin:</p>";
echo "<textarea style='width:100%;height:80px;padding:10px;font-family:monospace'>";
echo "UPDATE users SET password='" . $hash . "' WHERE email='admin@aureliagrand.com';";
echo "</textarea>";
echo "<p style='color:red'><strong>⚠️ Delete this file after use!</strong></p>";
?>
