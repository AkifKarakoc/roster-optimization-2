-- Default admin user (BCrypt hash for 'admin123')
INSERT INTO users (id, username, password, role, active) VALUES
    (1, 'admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN', true)
    ON DUPLICATE KEY UPDATE username = username;