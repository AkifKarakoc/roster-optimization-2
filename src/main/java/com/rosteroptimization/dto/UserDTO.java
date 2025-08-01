package com.rosteroptimization.dto;

import com.rosteroptimization.entity.User;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private User.Role role;
    private Boolean active;
}