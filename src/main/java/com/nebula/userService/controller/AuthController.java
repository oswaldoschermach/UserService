package com.nebula.userService.controller;

import com.nebula.userService.dto.ErrorResponseDTO;
import com.nebula.userService.dto.JwtResponse;
import com.nebula.userService.dto.LoginRequest;
import com.nebula.userService.dto.PasswordResetConfirmDTO;
import com.nebula.userService.dto.PasswordResetRequestDTO;
import com.nebula.userService.dto.RefreshTokenRequest;
import com.nebula.userService.service.AuthService;
import com.nebula.userService.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoint para autenticação de usuários via JWT")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(
            summary = "Autenticar usuário",
            description = """
                    Autentica um usuário com credenciais válidas e retorna um token JWT.

                    ### Como usar o token:
                    Inclua o token retornado no header de todas as requisições protegidas:
                    ```
                    Authorization: Bearer <token>
                    ```
                    O token expira em **24 horas**.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticação bem-sucedida — token JWT retornado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = """
                                            {
                                              "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTcwNTAwMDAwMCwiZXhwIjoxNzA1MDg2NDAwfQ.signature"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos obrigatórios ausentes ou inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais inválidas",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Credenciais inválidas",
                                              "path": "/api/auth/login",
                                              "timestamp": "2024-01-15T10:30:00"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                             jakarta.servlet.http.HttpServletRequest request) {
        String ip = getClientIp(request);
        JwtResponse response = authService.authenticateUser(loginRequest, ip);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Renovar access token",
            description = "Usa um refresh token válido para emitir um novo access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novo access token gerado",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou revogado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "Logout",
            description = "Revoga o access token e o refresh token imediatamente via blacklist no Redis.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request,
                                       @RequestBody(required = false) RefreshTokenRequest body) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        String refreshToken = (body != null) ? body.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken, getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Solicitar recuperação de senha",
            description = "Envia um e-mail com token de recuperação. " +
                    "Retorna 204 independentemente de o e-mail existir (evita user enumeration).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "E-mail enviado (se o endereço existir)"),
            @ApiResponse(responseCode = "400", description = "E-mail inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO dto,
            jakarta.servlet.http.HttpServletRequest request) {
        passwordResetService.requestReset(dto.getEmail(), getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Confirmar recuperação de senha",
            description = "Redefine a senha usando o token recebido por e-mail (válido por 30 minutos).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou senha fraca",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmDTO dto,
            jakarta.servlet.http.HttpServletRequest request) {
        passwordResetService.resetPassword(dto.getToken(), dto.getNewPassword(), getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
