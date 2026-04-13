package com.nebula.userService.controller;

import com.nebula.userService.dto.PaginatedResponseDTO;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gerenciar usuários.
 * <p>
 * Exceções de negócio são tratadas centralizadamente pelo GlobalExceptionHandler.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "User Management", description = "Endpoints para gerenciamento de usuários")
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RateLimitService rateLimitService;

    @Operation(
            summary = "Cria um novo usuário",
            description = """
                    ## Endpoint para criação de novos usuários

                    ### Fluxo esperado:
                    1. Recebe os dados do usuário (fullName, username, email, password, role)
                    2. Valida os campos obrigatórios e formatos
                    3. Verifica se email ou username já existem
                    4. Criptografa a senha
                    5. Salva o novo usuário ativo no sistema

                    ### Regras de validação:
                    - **fullName**: Obrigatório (3-100 caracteres)
                    - **username**: Obrigatório, único (3-50 caracteres, apenas a-zA-Z0-9._-)
                    - **email**: Obrigatório, formato válido, único no sistema
                    - **password**: Obrigatório (mínimo 8 caracteres)
                    - **role**: Obrigatório (valores permitidos: USER, ADMIN, MODERATOR)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "id": 123, "fullName": "Maria Oliveira",
                                        "username": "maria.oliveira", "email": "maria@empresa.com",
                                        "role": "USER", "active": true
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Email ou username já cadastrado",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "429", description = "Muitas requisições — tente novamente em instantes",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class)))
    })
    @PostMapping("/createUser")
    public ResponseEntity<UserResponseDTO> createUser(
            @RequestBody @Valid UserRequestDTO userRequestDTO,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        if (!rateLimitService.tryConsume(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        log.info("Recebendo requisição para criar usuário: {}", userRequestDTO.getEmail());
        UserResponseDTO createdUser = userService.createUser(userRequestDTO);
        log.info("Usuário criado com sucesso: ID {}", createdUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    @Operation(
            summary = "Atualizar usuário",
            description = "Atualiza os dados de um usuário existente (fullName, role, active).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO userUpdateDTO) {

        UserResponseDTO updatedUser = userService.updateUser(id, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
            summary = "Buscar usuário por ID",
            description = "Recupera os detalhes completos de um usuário pelo seu ID único.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id) {

        log.debug("Buscando usuário com ID: {}", id);
        UserResponseDTO user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Buscar usuários por nome",
            description = "Busca paginada de usuários pelo nome completo (case-insensitive). Se vazio, retorna todos.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = PaginatedResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> findByFullName(
            @Parameter(description = "Nome completo ou parte do nome", example = "João")
            @RequestParam(required = false, defaultValue = "") String fullName,

            @Parameter(description = "Número da página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Quantidade de itens por página (1-100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponseDTO> result = (fullName == null || fullName.isBlank())
                ? userService.findAll(pageable)
                : userService.findByFullName(fullName, pageable);

        return ResponseEntity.ok(PaginatedResponseDTO.fromPage(result));
    }

    @Operation(
            summary = "Excluir usuário",
            description = "Remove permanentemente um usuário do sistema.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "ID inválido",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Usuário possui relacionamentos ativos",
                    content = @Content(schema = @Schema(implementation = com.nebula.userService.dto.ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id) {

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

