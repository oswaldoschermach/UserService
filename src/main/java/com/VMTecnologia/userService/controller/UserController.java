package com.VMTecnologia.userService.controller;

import com.VMTecnologia.userService.dto.PaginatedResponseDTO;
import com.VMTecnologia.userService.dto.UserRequestDTO;
import com.VMTecnologia.userService.dto.UserResponseDTO;
import com.VMTecnologia.userService.dto.UserUpdateDTO;
import com.VMTecnologia.userService.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para gerenciar usuários.
 * <p>
 * Este controlador fornece endpoints para criar, atualizar, buscar e excluir usuários.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Endpoints para gerenciamento de usuários")
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

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
                    - **password**: Obrigatório (mínimo 8 caracteres, letras e números)
                    - **role**: Obrigatório (valores permitidos: USER, ADMIN, MODERATOR)
                    
                    ### Observações:
                    - A senha é criptografada automaticamente antes de ser armazenada
                    - O usuário é criado com status 'active = true'
                    - Campos inválidos retornam erro 400 com detalhes
                    - Email/username duplicados retornam erro 409
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário criado com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = UserResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "successExample",
                                    value = """
                                                {
                                                    "id": 123,
                                                    "fullName": "Maria Oliveira",
                                                    "username": "maria.oliveira",
                                                    "email": "maria@empresa.com",
                                                    "role": "USER",
                                                    "active": true,
                                                    "createdAt": "2023-11-25T14:30:00Z"
                                                }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos na requisição",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "invalidEmail",
                                            value = """
                                                        {
                                                            "timestamp": "2023-11-25T14:32:00Z",
                                                            "status": 400,
                                                            "error": "Bad Request",
                                                            "message": "Email deve ser válido",
                                                            "path": "/api/users"
                                                        }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "invalidPassword",
                                            value = """
                                                        {
                                                            "timestamp": "2023-11-25T14:33:00Z",
                                                            "status": 400,
                                                            "error": "Bad Request",
                                                            "message": "Senha deve conter letras e números",
                                                            "path": "/api/users"
                                                        }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito - Dados já existentes",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "emailConflict",
                                            value = """
                                                        {
                                                            "timestamp": "2023-11-25T14:35:00Z",
                                                            "status": 409,
                                                            "error": "Conflict",
                                                            "message": "O email informado já está em uso",
                                                            "path": "/api/users"
                                                        }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "usernameConflict",
                                            value = """
                                                        {
                                                            "timestamp": "2023-11-25T14:36:00Z",
                                                            "status": 409,
                                                            "error": "Conflict",
                                                            "message": "O username já está sendo utilizado",
                                                            "path": "/api/users"
                                                        }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                                {
                                                    "timestamp": "2023-11-25T14:40:00Z",
                                                    "status": 500,
                                                    "error": "Internal Server Error",
                                                    "message": "Ocorreu um erro inesperado",
                                                    "path": "/api/users"
                                                }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/createUser")
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody @Valid UserRequestDTO userRequestDTO) {
        log.info("Recebendo requisição para criar usuário: {}", userRequestDTO.getEmail());

        UserResponseDTO createdUser = userService.createUser(userRequestDTO);

        log.info("Usuário criado com sucesso: ID {}", createdUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }


    @Operation(
            summary = "Atualizar usuário",
            description = """
        Atualiza os dados de um usuário existente.
        
        ### Campos atualizáveis:
        - fullName
        - role
        - active
        
        ### Restrições:
        - Não é possível alterar: id, email, username
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário atualizado com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = UserResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                        "id": 1,
                        "fullName": "Novo Nome",
                        "username": "usuario.antigo",
                        "email": "email@original.com",
                        "role": "ADMIN",
                        "active": true
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                        "status": 400,
                        "message": "Nome completo é obrigatório",
                        "timestamp": "2023-11-26T15:00:00Z"
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                        "status": 404,
                        "message": "Usuário com ID 999 não encontrado",
                        "timestamp": "2023-11-26T15:01:00Z"
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                        "status": 500,
                        "message": "Erro ao atualizar usuário",
                        "timestamp": "2023-11-26T15:02:00Z"
                    }
                """
                            )
                    )
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id,

            @Valid @RequestBody UserUpdateDTO userUpdateDTO) {

        try {
            UserResponseDTO updatedUser = userService.updateUser(id, userUpdateDTO);
            return ResponseEntity.ok(updatedUser);

        } catch (EntityNotFoundException ex) {
            log.warn("Tentativa de atualizar usuário inexistente: ID {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());

        } catch (IllegalArgumentException ex) {
            log.warn("Requisição inválida para atualização: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());

        } catch (Exception ex) {
            log.error("Erro ao atualizar usuário ID: {}", id, ex);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro durante a atualização do usuário"
            );
        }
    }


    @Operation(
            summary = "Buscar usuário por ID",
            description = "Recupera os detalhes completos de um usuário pelo seu ID único",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuário encontrado",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponseDTO.class),
                                    examples = @ExampleObject(
                                            value = """
                                        {
                                            "id": 1,
                                            "fullName": "João Silva",
                                            "username": "joao.silva",
                                            "email": "joao@empresa.com",
                                            "role": "USER",
                                            "active": true
                                        }
                                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "ID inválido (nulo ou ≤ 0)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                        {
                                            "timestamp": "2023-11-26T14:30:00Z",
                                            "status": 400,
                                            "error": "Bad Request",
                                            "message": "ID deve ser um número positivo",
                                            "path": "/api/users/0"
                                        }
                                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                        {
                                            "timestamp": "2023-11-26T14:31:00Z",
                                            "status": 404,
                                            "error": "Not Found",
                                            "message": "Usuário com ID 999 não encontrado",
                                            "path": "/api/users/999"
                                        }
                                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Erro interno no servidor",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                        {
                                            "timestamp": "2023-11-26T14:32:00Z",
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Erro ao acessar o banco de dados",
                                            "path": "/api/users/1"
                                        }
                                        """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id) {

        if (id == null || id <= 0) {
            log.warn("Tentativa de busca com ID inválido: {}", id);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID deve ser um número positivo"
            );
        }

        try {
            log.debug("Buscando usuário com ID: {}", id);
            UserResponseDTO user = userService.findById(id);

            if (user == null) {
                log.warn("Usuário não encontrado para ID: {}", id);
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário com ID " + id + " não encontrado"
                );
            }

            log.debug("Usuário encontrado: {}", user);
            return ResponseEntity.ok(user);

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (EntityNotFoundException ex) {
            log.warn("Usuário não encontrado: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuário com ID " + id + " não encontrado",
                    ex
            );

        } catch (Exception ex) {
            log.error("Erro ao buscar usuário ID: {}", id, ex);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro durante a busca do usuário",
                    ex
            );
        }
    }

    @Operation(
            summary = "Buscar usuários por nome",
            description = """
        Realiza uma busca paginada de usuários pelo nome completo (case-insensitive), em caso de vazio retorna todos os dados.
        
        ### Fluxo:
        1. Valida os parâmetros de paginação
        2. Executa a busca no banco de dados
        3. Retorna os resultados paginados
        
        ### Observações:
        - Busca parcial (LIKE) pelo nome
        - Paginação padrão: página 0, tamanho 10
        - Campos de ordenação podem ser adicionados
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Busca realizada com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = PaginatedResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                        "content": [
                            {
                                "id": 1,
                                "fullName": "João Silva",
                                "username": "joao.silva",
                                "email": "joao@empresa.com",
                                "role": "USER",
                                "active": true
                            },
                            {
                                "id": 2,
                                "fullName": "João Pereira",
                                "username": "joao.pereira",
                                "email": "joao.pereira@empresa.com",
                                "role": "ADMIN",
                                "active": true
                            }
                        ],
                        "pageNumber": 0,
                        "pageSize": 10,
                        "totalElements": 2,
                        "totalPages": 1,
                        "last": true
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros inválidos",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                    {
                        "timestamp": "2023-11-25T17:30:00Z",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Parâmetro 'page' deve ser maior ou igual a 0",
                        "path": "/api/users/search"
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                    {
                        "timestamp": "2023-11-25T17:35:00Z",
                        "status": 500,
                        "error": "Internal Server Error",
                        "message": "Erro ao acessar o banco de dados",
                        "path": "/api/users/search"
                    }
                """
                            )
                    )
            )
    })
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> findByFullName(
            @Parameter(description = "Nome completo ou parte do nome (vazio retorna todos)", required = false, example = "João")
            @RequestParam(required = false, defaultValue = "") String fullName,

            @Parameter(description = "Número da página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Quantidade de itens por página", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserResponseDTO> result;

            if (fullName == null || fullName.isBlank()) {
                result = userService.findAll(pageable);
            } else {
                result = userService.findByFullName(fullName, pageable);
            }

            return ResponseEntity.ok(PaginatedResponseDTO.fromPage(result));

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            log.error("Erro na busca por nome", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro durante a busca");
        }
    }

    @Operation(
            summary = "Excluir usuário",
            description = """
        Remove permanentemente um usuário do sistema.
        
        ### Fluxo:
        1. Verifica se o usuário com o ID informado existe
        2. Realiza a exclusão lógica (soft delete) ou física
        3. Retorna status apropriado
        
        ### Observações:
        - Operação irreversível
        - IDs inválidos retornam HTTP 400
        - Usuário não encontrado retorna HTTP 404
        - Em caso de sucesso, retorna HTTP 204 (No Content)
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Usuário excluído com sucesso",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "ID inválido (nulo ou ≤ 0)",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                    {
                        "timestamp": "2023-11-25T18:00:00Z",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "ID deve ser maior que zero",
                        "path": "/api/users/0"
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                    {
                        "timestamp": "2023-11-25T18:01:00Z",
                        "status": 404,
                        "error": "Not Found",
                        "message": "Usuário com ID 999 não encontrado",
                        "path": "/api/users/999"
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                    {
                        "timestamp": "2023-11-25T18:02:00Z",
                        "status": 500,
                        "error": "Internal Server Error",
                        "message": "Falha ao excluir usuário",
                        "path": "/api/users/1"
                    }
                """
                            )
                    )
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro durante a exclusão");
        }
    }
}