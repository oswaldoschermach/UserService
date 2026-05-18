package com.nebula.userService.configs;

public final class OpenApiExamples {

    private OpenApiExamples() {
    }

    public static final String LOGIN_REQUEST = """
            {
              "username": "joao.silva",
              "password": "Senha@123"
            }
            """;

    public static final String LOGIN_RESPONSE = """
            {
              "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3MTU3NjAwMDAsImV4cCI6MTcxNTg0NjQwMH0.signature",
              "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE3MTU3NjAwMDAsImV4cCI6MTcxNjM2NDgwMH0.signature"
            }
            """;

    public static final String REFRESH_REQUEST = """
            {
              "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE3MTU3NjAwMDAsImV4cCI6MTcxNjM2NDgwMH0.signature"
            }
            """;

    public static final String REFRESH_RESPONSE = """
            {
              "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3MTU3NjM2MDAsImV4cCI6MTcxNTg1MDAwMH0.signature",
              "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE3MTU3NjAwMDAsImV4cCI6MTcxNjM2NDgwMH0.signature"
            }
            """;

    public static final String LOGOUT_REQUEST = """
            {
              "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE3MTU3NjAwMDAsImV4cCI6MTcxNjM2NDgwMH0.signature"
            }
            """;

    public static final String PASSWORD_RESET_REQUEST = """
            {
              "email": "joao@empresa.com"
            }
            """;

    public static final String PASSWORD_RESET_CONFIRM_REQUEST = """
            {
              "token": "550e8400-e29b-41d4-a716-446655440000",
              "newPassword": "NovaSenha@456"
            }
            """;

    public static final String CREATE_USER_REQUEST = """
            {
              "fullName": "Joao da Silva",
              "username": "joao.silva",
              "email": "joao@empresa.com",
              "password": "Senha@123",
              "role": "USER"
            }
            """;

    public static final String CREATE_ADMIN_REQUEST = """
            {
              "fullName": "Maria Oliveira",
              "username": "maria.admin",
              "email": "maria.admin@empresa.com",
              "password": "Senha@123",
              "role": "ADMIN"
            }
            """;

    public static final String USER_RESPONSE = """
            {
              "id": 123,
              "fullName": "Joao da Silva",
              "role": "USER",
              "username": "joao.silva",
              "email": "joao@empresa.com",
              "active": true
            }
            """;

    public static final String UPDATE_USER_REQUEST = """
            {
              "fullName": "Joao da Silva Atualizado",
              "role": "MODERATOR",
              "active": true
            }
            """;

    public static final String CURRENT_USER_UPDATE_REQUEST = """
            {
              "fullName": "Joao da Silva Atualizado"
            }
            """;

    public static final String CHANGE_PASSWORD_REQUEST = """
            {
              "currentPassword": "Senha@123",
              "newPassword": "NovaSenha@456"
            }
            """;

    public static final String PAGINATED_USERS_RESPONSE = """
            {
              "pagina": 0,
              "tamanho": 10,
              "totalItens": 2,
              "totalPaginas": 1,
              "items": [
                {
                  "id": 123,
                  "fullName": "Joao da Silva",
                  "role": "USER",
                  "username": "joao.silva",
                  "email": "joao@empresa.com",
                  "active": true
                },
                {
                  "id": 124,
                  "fullName": "Joana Silva",
                  "role": "ADMIN",
                  "username": "joana.silva",
                  "email": "joana@empresa.com",
                  "active": true
                }
              ]
            }
            """;

    public static final String BAD_REQUEST_RESPONSE = """
            {
              "status": 400,
              "error": "Bad Request",
              "message": "Senha deve ter no minimo 8 caracteres",
              "path": "/api/users/createUser",
              "timestamp": "2026-05-15T14:30:00"
            }
            """;

    public static final String UNAUTHORIZED_RESPONSE = """
            {
              "status": 401,
              "error": "Unauthorized",
              "message": "Credenciais invalidas",
              "path": "/api/auth/login",
              "timestamp": "2026-05-15T14:30:00"
            }
            """;

    public static final String FORBIDDEN_RESPONSE = """
            {
              "status": 403,
              "error": "Forbidden",
              "message": "Access Denied",
              "path": "/api/users/1",
              "timestamp": "2026-05-15T14:30:00"
            }
            """;

    public static final String NOT_FOUND_RESPONSE = """
            {
              "status": 404,
              "error": "Not Found",
              "message": "Usuario nao encontrado com ID: 99",
              "path": "/api/users/99",
              "timestamp": "2026-05-15T14:30:00"
            }
            """;

    public static final String CONFLICT_RESPONSE = """
            {
              "status": 409,
              "error": "Conflict",
              "message": "O e-mail 'joao@empresa.com' ja esta cadastrado",
              "path": "/api/users/createUser",
              "timestamp": "2026-05-15T14:30:00"
            }
            """;

    public static final String RATE_LIMIT_RESPONSE = """
            {
              "status": 429,
              "error": "Too Many Requests",
              "message": "Limite de requisicoes excedido para criacao de usuario. Tente novamente em instantes.",
              "path": "/api/users/createUser",
              "timestamp": "2026-05-15T14:30:00"
            }
            """;
}
