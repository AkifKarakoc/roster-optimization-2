package com.rosteroptimization.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token;
    private String username;
    private String role;
    private Long expiresIn;
}