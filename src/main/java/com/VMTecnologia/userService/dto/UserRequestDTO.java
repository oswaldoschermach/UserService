package com.VMTecnologia.userService.dto;

import com.VMTecnologia.userService.entities.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDTO {
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String role;

    public UserRequestDTO(String user, String senha123, String mail) {
    }

    public UserEntity toEntity(PasswordEncoder passwordEncoder) {
        return UserEntity.builder()
                .fullName(this.fullName)
                .email(this.email)
                .username(this.username)
                .password(passwordEncoder.encode(this.password))
                .role(this.role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

}