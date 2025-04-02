package com.VMTecnologia.userService.controller;

import com.VMTecnologia.userService.dto.JwtResponse;
import com.VMTecnologia.userService.dto.LoginRequest;
import com.VMTecnologia.userService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para autenticação de usuários")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Autenticar usuário",
            description = "Autentica um usuário com credenciais válidas e retorna um token JWT",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = JwtResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                            content = @Content(mediaType = "application/json"))
            }
    )
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        JwtResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Valida um token JWT
     * @param tokenRequest DTO contendo o token
     * @return ResponseEntity com status de validação
     */
//    @PostMapping("/validate-token")
//    public ResponseEntity<ValidationResponse> validateToken(
//            @Valid @RequestBody TokenValidationRequest tokenRequest) {
//
//        boolean isValid = authService.validateToken(tokenRequest.getToken());
//        return ResponseEntity.ok(new ValidationResponse(isValid));
//    }
}
