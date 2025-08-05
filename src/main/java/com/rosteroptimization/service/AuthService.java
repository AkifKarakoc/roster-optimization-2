package com.rosteroptimization.service;

import com.rosteroptimization.dto.LoginRequestDTO;
import com.rosteroptimization.dto.LoginResponseDTO;
import com.rosteroptimization.dto.UserDTO;
import com.rosteroptimization.entity.User;
import com.rosteroptimization.repository.UserRepository;
import com.rosteroptimization.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponseDTO login(LoginRequestDTO request) {
        System.out.println("=== LOGIN DEBUG ===");
        System.out.println("Username received: " + request.getUsername());
        System.out.println("Password received length: " + (request.getPassword() != null ? request.getPassword().length() : "null"));

        // Check if user exists in database
        User dbUser = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (dbUser == null) {
            System.out.println("ERROR: User not found in database");
            throw new RuntimeException("User not found");
        }

        System.out.println("User found in DB: " + dbUser.getUsername());
        System.out.println("User active: " + dbUser.getActive());
        System.out.println("User role: " + dbUser.getRole());
        System.out.println("DB password hash: " + dbUser.getPassword());

        // Test password matching manually
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), dbUser.getPassword());
        System.out.println("Password matches: " + passwordMatches);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            System.out.println("Authentication successful!");

            String token = tokenProvider.generateToken(authentication);
            System.out.println("Token generated successfully");

            LoginResponseDTO response = new LoginResponseDTO();
            response.setToken(token);
            response.setUsername(dbUser.getUsername());
            response.setRole(dbUser.getRole().name());
            response.setExpiresIn(3600L);

            return response;

        } catch (Exception e) {
            System.out.println("Authentication failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRole(user.getRole());
        userDTO.setActive(user.getActive());

        return userDTO;
    }
}