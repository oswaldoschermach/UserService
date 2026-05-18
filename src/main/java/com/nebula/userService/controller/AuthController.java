package com.nebula.userService.controller;

import com.nebula.userService.dto.ErrorResponseDTO;
import com.nebula.userService.dto.JwtResponse;
import com.nebula.userService.dto.LoginRequest;
import com.nebula.userService.dto.PasswordResetConfirmDTO;
import com.nebula.userService.dto.PasswordResetRequestDTO;
import com.nebula.userService.dto.RefreshTokenRequest;
import com.nebula.userService.dto.UserSessionDTO;
import com.nebula.userService.service.AuthService;
import com.nebula.userService.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.nebula.userService.configs.OpenApiExamples.BAD_REQUEST_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.LOGIN_REQUEST;
import static com.nebula.userService.configs.OpenApiExamples.LOGIN_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.LOGOUT_REQUEST;
import static com.nebula.userService.configs.OpenApiExamples.PASSWORD_RESET_CONFIRM_REQUEST;
import static com.nebula.userService.configs.OpenApiExamples.PASSWORD_RESET_REQUEST;
import static com.nebula.userService.configs.OpenApiExamples.REFRESH_REQUEST;
import static com.nebula.userService.configs.OpenApiExamples.REFRESH_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.UNAUTHORIZED_RESPONSE;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Autenticacao",
        description = """
                Endpoints responsaveis por login, refresh token, logout e recuperacao de senha.

                Use esta sequencia para integrar:
                1. `POST /api/auth/login`
                2. envie `Authorization: Bearer <token>` nas rotas protegidas
                3. renove o token com `POST /api/auth/refresh`
                4. revogue a sessao com `POST /api/auth/logout`
                """
)
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(
            summary = "Autenticar usuario",
            description = """
                    Autentica um usuario com `username` e `password` e retorna:
                    - `token`: access token para endpoints protegidos
                    - `refreshToken`: token para emitir um novo access token sem novo login

                    ## Como usar o token retornado
                    Envie o header abaixo em todas as rotas protegidas:
                    ```http
                    Authorization: Bearer <access-token>
                    ```

                    ## Observacoes importantes
                    - o campo de entrada e `username`, nao `email`
                    - o access token expira em 24 horas
                    - o refresh token expira em 7 dias
                    - falhas de autenticacao retornam mensagem generica para o consumidor
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Credenciais do usuario para iniciar sessao.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(name = "login", value = LOGIN_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticacao concluida com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponse.class),
                            examples = @ExampleObject(name = "success", value = LOGIN_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos obrigatorios ausentes ou payload invalido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "invalid-payload", value = BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais invalidas ou conta bloqueada para autenticacao",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "invalid-credentials", value = UNAUTHORIZED_RESPONSE)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                             HttpServletRequest request) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        JwtResponse response = authService.authenticateUser(loginRequest, ip, userAgent);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Renovar access token",
            description = """
                    Recebe um `refreshToken` valido e devolve um novo `token`.

                    ## Quando usar
                    Use este endpoint quando o access token expirar, sem forcar o usuario a fazer login novamente.

                    ## Comportamento
                    - o refresh token precisa ser um token do tipo `refresh`
                    - tokens revogados em logout nao podem mais ser reutilizados
                    - o mesmo refresh token pode ser retornado na resposta
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Refresh token retornado anteriormente no login.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RefreshTokenRequest.class),
                    examples = @ExampleObject(name = "refresh", value = REFRESH_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Novo access token emitido com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponse.class),
                            examples = @ExampleObject(name = "success", value = REFRESH_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Payload invalido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "invalid-payload", value = BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token invalido, expirado ou revogado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "unauthorized", value = UNAUTHORIZED_RESPONSE)
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @Operation(
            summary = "Encerrar sessao (logout)",
            description = """
                    Revoga imediatamente o access token e, se informado, tambem o refresh token.

                    ## Como chamar
                    - envie o access token no header `Authorization`
                    - envie o refresh token no corpo se quiser revogar os dois tokens de uma vez

                    ## Observacao
                    O logout usa blacklist em Redis, impedindo o reuso dos tokens revogados.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Opcionalmente envie o refresh token para revoga-lo junto com o access token.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RefreshTokenRequest.class),
                    examples = @ExampleObject(name = "logout-body", value = LOGOUT_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access token ausente, invalido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "unauthorized", value = UNAUTHORIZED_RESPONSE)
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       @RequestBody(required = false) RefreshTokenRequest body) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        String refreshToken = (body != null) ? body.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken, getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Listar sessões ativas",
            description = "Retorna todas as sessões ativas do usuário autenticado.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/sessions")
    public ResponseEntity<java.util.List<UserSessionDTO>> listSessions(Authentication authentication) {
        return ResponseEntity.ok(authService.listSessions(authentication.getName()));
    }

    @Operation(
            summary = "Revogar todas as sessões do usuário autenticado",
            description = "Revoga todas as sessões ativas desse usuário, invalidando tokens sem necessidade de logout individual.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/sessions/revoke-all")
    public ResponseEntity<Void> revokeAllSessions(Authentication authentication,
                                                  HttpServletRequest request) {
        authService.revokeAllSessions(authentication.getName(), getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Revogar sessão específica",
            description = "Revoga uma sessão ativa identificada por sessionId.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/sessions/{sessionId}/revoke")
    public ResponseEntity<Void> revokeSession(@PathVariable String sessionId,
                                              Authentication authentication,
                                              HttpServletRequest request) {
        authService.revokeSession(sessionId, authentication.getName(), getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Solicitar recuperacao de senha",
            description = """
                    Gera um token de recuperacao e tenta envia-lo para o e-mail informado.

                    ## Comportamento de seguranca
                    Este endpoint sempre retorna `204 No Content` quando o payload e valido, mesmo que o e-mail nao exista.
                    Isso evita vazamento de informacao sobre usuarios cadastrados.

                    ## Validade do token
                    O token enviado por e-mail vale por 30 minutos.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "E-mail cadastrado da conta.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PasswordResetRequestDTO.class),
                    examples = @ExampleObject(name = "request-reset", value = PASSWORD_RESET_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Solicitacao processada com sucesso"),
            @ApiResponse(
                    responseCode = "400",
                    description = "E-mail ausente ou invalido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "invalid-email", value = BAD_REQUEST_RESPONSE)
                    )
            )
    })
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO dto,
            HttpServletRequest request) {
        passwordResetService.requestReset(dto.getEmail(), getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Confirmar recuperacao de senha",
            description = """
                    Redefine a senha usando o token recebido por e-mail.

                    ## Regras
                    - o token precisa existir, estar valido e nao ter sido usado
                    - a nova senha precisa ter no minimo 8 caracteres
                    - ao redefinir a senha, tentativas falhas de login e bloqueio de conta sao resetados
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Token de recuperacao e nova senha.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PasswordResetConfirmDTO.class),
                    examples = @ExampleObject(name = "confirm-reset", value = PASSWORD_RESET_CONFIRM_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida com sucesso"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token invalido, expirado ou nova senha invalida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "invalid-reset", value = BAD_REQUEST_RESPONSE)
                    )
            )
    })
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmDTO dto,
            HttpServletRequest request) {
        passwordResetService.resetPassword(dto.getToken(), dto.getNewPassword(), getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
