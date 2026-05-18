package com.nebula.userService.controller;

import com.nebula.userService.dto.ErrorResponseDTO;
import com.nebula.userService.dto.CurrentUserUpdateDTO;
import com.nebula.userService.dto.PaginatedResponseDTO;
import com.nebula.userService.dto.PasswordChangeDTO;
import com.nebula.userService.dto.UserPermissionUpdateDTO;
import com.nebula.userService.dto.UserRequestDTO;
import com.nebula.userService.dto.UserResponseDTO;
import com.nebula.userService.dto.UserUpdateDTO;
import com.nebula.userService.service.RateLimitService;
import com.nebula.userService.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static com.nebula.userService.configs.OpenApiExamples.BAD_REQUEST_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.CHANGE_PASSWORD_REQUEST;
import static com.nebula.userService.configs.OpenApiExamples.CONFLICT_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.CURRENT_USER_UPDATE_REQUEST;
import static com.nebula.userService.configs.OpenApiExamples.CREATE_USER_REQUEST;
import static com.nebula.userService.configs.OpenApiExamples.FORBIDDEN_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.NOT_FOUND_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.PAGINATED_USERS_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.RATE_LIMIT_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.UNAUTHORIZED_RESPONSE;
import static com.nebula.userService.configs.OpenApiExamples.UPDATE_USER_REQUEST;
import static com.nebula.userService.configs.OpenApiExamples.USER_RESPONSE;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(
        name = "Usuarios",
        description = """
                Endpoints para criacao, consulta, atualizacao e exclusao de usuarios.

                Regras gerais:
                - `POST /api/users/createUser` e publico e possui rate limiting
                - `GET /api/users/**` exige autenticacao
                - `PUT /api/users/me` e `POST /api/users/me/change-password` sao de autoatendimento
                - `PUT /api/users/{id}` e `DELETE /api/users/{id}` exigem role `ADMIN`
                """
)
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RateLimitService rateLimitService;

    @Operation(
            summary = "Criar novo usuario",
            description = """
                    Endpoint publico para cadastro de novos usuarios.

                    ## O que este endpoint faz
                    1. valida os campos obrigatorios
                    2. verifica duplicidade de `email` e `username`
                    3. criptografa a senha antes de persistir
                    4. salva o usuario como ativo
                    5. tenta enviar um e-mail de confirmacao

                    ## Regras importantes
                    - `username` e unico e usado no login
                    - `email` tambem precisa ser unico
                    - por seguranca, o cadastro publico aceita apenas `role = USER`
                    - existe rate limiting por IP para evitar abuso
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados do usuario a ser criado.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserRequestDTO.class),
                    examples = @ExampleObject(name = "user", value = CREATE_USER_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario criado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class),
                            examples = @ExampleObject(name = "created", value = USER_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Payload invalido ou campos fora das regras",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "bad-request", value = BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email ou username ja cadastrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "conflict", value = CONFLICT_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Limite de requisicoes excedido para criacao de usuario",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "rate-limit", value = RATE_LIMIT_RESPONSE)
                    )
            )
    })
    @PostMapping("/createUser")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserRequestDTO userRequestDTO,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        if (!rateLimitService.tryConsume(clientIp)) {
            ErrorResponseDTO error = ErrorResponseDTO.builder()
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                    .message("Limite de requisicoes excedido para criacao de usuario. Tente novamente em instantes.")
                    .path(request.getRequestURI())
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }

        log.info("Recebendo requisicao para criar usuario: {}", userRequestDTO.getEmail());
        UserResponseDTO createdUser = userService.createUser(userRequestDTO);
        log.info("Usuario criado com sucesso: ID {}", createdUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @Operation(
            summary = "Consultar meu perfil",
            description = """
                    Retorna os dados do usuario autenticado a partir do access token.

                    ## Quando usar
                    Use este endpoint para montar perfil, cabecalho da aplicacao ou tela de conta do usuario logado.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil do usuario autenticado retornado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class),
                            examples = @ExampleObject(name = "me", value = USER_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, invalido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "unauthorized", value = UNAUTHORIZED_RESPONSE)
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.findCurrentUser(authentication.getName()));
    }

    @Operation(
            summary = "Atualizar meu perfil",
            description = """
                    Permite que o proprio usuario autenticado atualize seu nome completo.

                    ## Escopo atual
                    Este endpoint de autoatendimento nao altera `role`, `active`, `username` ou `email`.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Novo nome completo do usuario autenticado.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CurrentUserUpdateDTO.class),
                    examples = @ExampleObject(name = "me-update", value = CURRENT_USER_UPDATE_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil atualizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class),
                            examples = @ExampleObject(name = "updated", value = USER_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Payload invalido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "bad-request", value = BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, invalido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "unauthorized", value = UNAUTHORIZED_RESPONSE)
                    )
            )
    })
    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateCurrentUser(Authentication authentication,
                                                             @Valid @RequestBody CurrentUserUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateCurrentUser(authentication.getName(), dto));
    }

    @Operation(
            summary = "Trocar minha senha",
            description = """
                    Permite que o usuario autenticado troque sua propria senha informando a senha atual.

                    ## Comportamento
                    - valida a senha atual
                    - salva a nova senha criptografada
                    - reseta tentativas falhas e bloqueio de conta
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Senha atual e nova senha do usuario autenticado.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PasswordChangeDTO.class),
                    examples = @ExampleObject(name = "change-password", value = CHANGE_PASSWORD_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Payload invalido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "bad-request", value = BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token invalido ou senha atual incorreta",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "unauthorized", value = UNAUTHORIZED_RESPONSE)
                    )
            )
    })
    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody PasswordChangeDTO dto,
                                               HttpServletRequest request) {
        userService.changePassword(authentication.getName(), dto, getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Atualizar usuario",
            description = """
                    Atualiza os dados mutaveis de um usuario existente.

                    ## Autorizacao
                    Requer access token valido com role `ADMIN`.

                    ## Campos atualizados
                    - `fullName`
                    - `role`
                    - `active`

                    ## Recomendacao
                    Envie o payload completo com todos os campos mutaveis para evitar atualizacoes incompletas.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Novo estado desejado para o usuario.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserUpdateDTO.class),
                    examples = @ExampleObject(name = "update", value = UPDATE_USER_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario atualizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class),
                            examples = @ExampleObject(name = "updated", value = USER_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "ID invalido ou payload inconsistente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "bad-request", value = BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, invalido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "unauthorized", value = UNAUTHORIZED_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuario autenticado sem permissao administrativa",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "forbidden", value = FORBIDDEN_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario nao encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "not-found", value = NOT_FOUND_RESPONSE)
                    )
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "ID numerico do usuario", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO userUpdateDTO) {

        UserResponseDTO updatedUser = userService.updateUser(id, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
            summary = "Atualizar permissões finas do usuário",
            description = "Permite a um administrador ajustar permissões finas para um usuário específico.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Lista de permissões finas a serem atribuídas ao usuário.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserPermissionUpdateDTO.class)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Permissões atualizadas com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Payload inválido ou ID inválido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário autenticado sem permissão administrativa",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            )
    })
    @PutMapping("/{id}/permissions")
    public ResponseEntity<UserResponseDTO> updateUserPermissions(
            @Parameter(description = "ID numerico do usuário", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserPermissionUpdateDTO dto) {

        UserResponseDTO updatedUser = userService.updatePermissions(id, dto);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
            summary = "Buscar usuario por ID",
            description = """
                    Recupera os dados publicos de um usuario pelo ID.

                    ## Autorizacao
                    Requer access token valido.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class),
                            examples = @ExampleObject(name = "found", value = USER_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "ID invalido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "bad-request", value = BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, invalido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "unauthorized", value = UNAUTHORIZED_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario nao encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "not-found", value = NOT_FOUND_RESPONSE)
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(
            @Parameter(description = "ID numerico do usuario", example = "1", required = true)
            @PathVariable Long id) {

        log.debug("Buscando usuario com ID: {}", id);
        UserResponseDTO user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Listar ou buscar usuarios por nome",
            description = """
                    Executa busca paginada por `fullName`.

                    ## Comportamento
                    - se `fullName` vier vazio ou nao for enviado, a API retorna todos os usuarios paginados
                    - a busca por nome e case-insensitive
                    - `page` comeca em `0`
                    - `size` aceita valores de `1` a `100`

                    ## Exemplo de uso
                    - `GET /api/users/search?fullName=Joao&page=0&size=10`
                    - `GET /api/users/search?page=0&size=20`
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Busca executada com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaginatedResponseDTO.class),
                            examples = @ExampleObject(name = "page", value = PAGINATED_USERS_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parametros de busca ou paginacao invalidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "bad-request", value = BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, invalido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "unauthorized", value = UNAUTHORIZED_RESPONSE)
                    )
            )
    })
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> findByFullName(
            @Parameter(description = "Nome completo ou parte do nome. Se vazio, retorna todos os usuarios.",
                    example = "Joao")
            @RequestParam(required = false, defaultValue = "") String fullName,

            @Parameter(description = "Indice da pagina, iniciado em 0", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Quantidade de itens por pagina. Intervalo permitido: 1 a 100.", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponseDTO> result = (fullName == null || fullName.isBlank())
                ? userService.findAll(pageable)
                : userService.findByFullName(fullName, pageable);

        return ResponseEntity.ok(PaginatedResponseDTO.fromPage(result));
    }

    @Operation(
            summary = "Excluir usuario",
            description = """
                    Remove permanentemente um usuario do sistema.

                    ## Autorizacao
                    Requer access token valido com role `ADMIN`.

                    ## Observacao
                    Esta operacao e destrutiva e nao possui reversao automatica.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario excluido com sucesso"),
            @ApiResponse(
                    responseCode = "400",
                    description = "ID invalido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "bad-request", value = BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, invalido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "unauthorized", value = UNAUTHORIZED_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuario autenticado sem permissao administrativa",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "forbidden", value = FORBIDDEN_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario nao encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "not-found", value = NOT_FOUND_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Usuario possui dependencias que impedem a exclusao",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "conflict", value = CONFLICT_RESPONSE)
                    )
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID numerico do usuario", example = "1", required = true)
            @PathVariable Long id) {

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
