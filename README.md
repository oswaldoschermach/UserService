# 👤 UserService

Microsserviço RESTful para **cadastro, autenticação e gerenciamento de usuários**, construído com Spring Boot 3 e autenticação stateless via JWT.

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração do Ambiente](#-configuração-do-ambiente)
- [Como Rodar](#-como-rodar)
- [Endpoints da API](#-endpoints-da-api)
- [Autenticação JWT](#-autenticação-jwt)
- [Refresh Token](#-refresh-token)
- [Logout e Blacklist](#-logout-e-blacklist)
- [Rate Limiting](#-rate-limiting)
- [Controle de Acesso por Role](#-controle-de-acesso-por-role)
- [CORS](#-cors)
- [Migrations com Flyway](#-migrations-com-flyway)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Testes](#-testes)
- [Docker](#-docker)
- [Observações Importantes](#-observações-importantes)

---

## 🔍 Visão Geral

O **UserService** é responsável por:

- Criar e gerenciar usuários com roles (`USER`, `ADMIN`, `MODERATOR`)
- Autenticar usuários e emitir **access token** (24h) + **refresh token** (7 dias)
- Renovar o access token via refresh token sem re-login
- Realizar **logout** com invalidação imediata dos tokens via Redis
- **Bloquear conta** automaticamente após 5 tentativas de login falhas (15 min)
- **Recuperação de senha** via token enviado por e-mail (válido 30 min)
- Aplicar **rate limiting** por IP nos endpoints públicos
- Proteger endpoints com autenticação stateless e controle de acesso por role
- Gerenciar o schema do banco via **Flyway** (migrations versionadas)
- Registrar **auditoria** de todas as ações sensíveis de forma assíncrona
- Enviar e-mail de confirmação ao criar uma conta
- Expor documentação interativa via Swagger UI

---

## 🛠 Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Linguagem principal |
| Spring Boot | 3.4.4 | Framework base |
| Spring Security | 6.x | Autenticação e autorização |
| Spring Data JPA | 3.x | Persistência de dados |
| PostgreSQL | 16 | Banco de dados de produção |
| Redis | 7 | Blacklist de tokens e rate limiting |
| Flyway | (gerenciado pelo Boot) | Migrations de banco de dados |
| jjwt | 0.12.6 | Geração e validação de tokens JWT |
| Bucket4j | 8.10.1 | Rate limiting por IP |
| Lombok | 1.18.x | Redução de boilerplate |
| SpringDoc OpenAPI | 2.8.6 | Documentação Swagger UI |
| JaCoCo | 0.8.12 | Cobertura de testes (≥ 80%) |
| H2 | 2.x | Banco de dados em memória (testes) |

---

## 🏗 Arquitetura

```
┌─────────────────────────────────────────────────────┐
│                     HTTP Request                     │
└──────────────────────────┬──────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │  JwtAuthenticationFilter │  ← valida Bearer token
              │  (checa blacklist Redis) │  ← rejeita tokens revogados
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │       Controller         │  ← @Valid, @Min/@Max, rate limit
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │         Service          │  ← regras de negócio, @Transactional
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │        Repository        │  ← Spring Data JPA
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │        PostgreSQL        │  ← schema gerenciado pelo Flyway
              └─────────────────────────┘

              ┌─────────────────────────┐
              │          Redis           │  ← blacklist + rate limit buckets
              └─────────────────────────┘

              ┌─────────────────────────┐
              │   GlobalExceptionHandler │  ← trata todas as exceções
              └─────────────────────────┘
```

**Pacotes principais:**

```
com.nebula.userService
├── configs/       # Security, JWT filter, CORS, Redis, Swagger
├── controller/    # AuthController, UserController
├── dto/           # Request/Response DTOs
├── entities/      # UserEntity (JPA)
├── enums/         # Role (USER, ADMIN, MODERATOR)
├── exception/     # Exceções de negócio tipadas
├── handler/       # GlobalExceptionHandler
├── repository/    # UserRepository (Spring Data)
└── service/       # AuthService, UserService, EmailService,
                   # TokenBlacklistService, RateLimitService,
                   # PasswordResetService, AuditLogService
```

---

## ✅ Pré-requisitos

- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.8+** (ou use o `mvnw` incluído no projeto)
- **PostgreSQL 16+** — [Download](https://www.postgresql.org/download/)
- **Redis 7+** — [Download](https://redis.io/download/) ou via Docker
- **Git**

Para desenvolvimento local, recomenda-se usar o **Docker Compose** que sobe todos os serviços (PostgreSQL, Redis e MailHog) automaticamente.

---

## ⚙️ Configuração do Ambiente

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/userservice.git
cd userservice
```

### 2. Configure as variáveis de ambiente

Copie o arquivo de exemplo e preencha com seus valores:

```bash
cp .env.example .env
```

Edite o `.env`. Para desenvolvimento, os valores padrão já funcionam com o Docker Compose:

```dotenv
DB_NAME=userservice
DB_USERNAME=postgres
DB_PASSWORD=minhasenha123

# Gere com: openssl rand -base64 32
JWT_SECRET=sua_chave_base64_aqui
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_USERNAME=dev@localhost
MAIL_PASSWORD=

CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200

SPRING_PROFILES_ACTIVE=dev
```

> ⚠️ **Nunca commite o arquivo `.env`** — ele já está no `.gitignore`.

### 3. Crie o banco de dados (se rodar sem Docker)

```sql
CREATE DATABASE userservice;
```

> Com Docker Compose o banco é criado automaticamente.

### 4. (Opcional) Gmail — App Password

Para usar o Gmail como SMTP em produção:

1. Acesse [myaccount.google.com/security](https://myaccount.google.com/security)
2. Ative a verificação em duas etapas
3. Vá em **App passwords** e gere uma senha para "Mail"
4. Use essa senha no `MAIL_PASSWORD`

---

## 🚀 Como Rodar

### Via Docker Compose (recomendado)

Sobe a aplicação + PostgreSQL + Redis + MailHog com um único comando:

```bash
docker compose up --build
```

### Manualmente — Perfil de desenvolvimento

Requer PostgreSQL e Redis rodando localmente (ou via Docker):

```bash
# Linux/Mac
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run

# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd spring-boot:run
```

Com o perfil `dev`:
- `ddl-auto: update` e Flyway **desabilitado** — tabelas criadas/atualizadas pelo Hibernate
- `show-sql: true` — queries no console
- SMTP aponta para `localhost:1025` (MailHog)
- `JWT_SECRET` tem valor padrão (não obrigatório definir)

### Manualmente — Perfil de produção

Todas as variáveis do `.env` devem estar definidas. Flyway **habilitado** — migrations executadas automaticamente na inicialização.

```bash
# Linux/Mac
export $(cat .env | xargs)
./mvnw spring-boot:run

# Windows PowerShell
Get-Content .env | ForEach-Object {
  if ($_ -match '^([^#][^=]*)=(.*)$') {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
  }
}
.\mvnw.cmd spring-boot:run
```

A aplicação sobe na porta **35698** por padrão:
- API: `http://localhost:35698`
- Swagger UI: `http://localhost:35698/swagger-ui.html`
- Health: `http://localhost:35698/actuator/health`

---

## 📡 Endpoints da API

### Autenticação — `/api/auth`

| Método | Endpoint | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/login` | ❌ Público | Autentica e retorna access + refresh token |
| `POST` | `/api/auth/refresh` | ❌ Público | Renova o access token com o refresh token |
| `POST` | `/api/auth/logout` | ✅ JWT | Revoga os tokens (logout) |
| `POST` | `/api/auth/password-reset/request` | ❌ Público | Solicita recuperação de senha por e-mail |
| `POST` | `/api/auth/password-reset/confirm` | ❌ Público | Redefine a senha com o token recebido |

**Login:**
```json
POST /api/auth/login
{
  "username": "joao.silva",
  "password": "Senha@123"
}
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Renovar token:**
```json
POST /api/auth/refresh
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Logout:**
```
POST /api/auth/logout
Authorization: Bearer <access_token>

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### Usuários — `/api/users`

| Método | Endpoint | Autenticação | Role mínima | Rate Limit | Descrição |
|---|---|---|---|---|---|
| `POST` | `/api/users/createUser` | ❌ Público | — | ✅ 10 req/min/IP | Cria novo usuário |
| `GET` | `/api/users/{id}` | ✅ JWT | USER | ❌ | Busca usuário por ID |
| `GET` | `/api/users/search` | ✅ JWT | USER | ❌ | Busca paginada por nome |
| `PUT` | `/api/users/{id}` | ✅ JWT | **ADMIN** | ❌ | Atualiza nome, role, status |
| `DELETE` | `/api/users/{id}` | ✅ JWT | **ADMIN** | ❌ | Remove usuário |

**Criar usuário:**
```json
POST /api/users/createUser
{
  "fullName": "João da Silva",
  "username": "joao.silva",
  "email": "joao@empresa.com",
  "password": "Senha@123",
  "role": "USER"
}
```

**Busca paginada** (`size` máximo: 100):
```
GET /api/users/search?fullName=João&page=0&size=10
```

**Resposta paginada:**
```json
{
  "pagina": 0,
  "tamanho": 10,
  "totalItens": 1,
  "totalPaginas": 1,
  "items": [
    {
      "id": 1,
      "fullName": "João da Silva",
      "username": "joao.silva",
      "email": "joao@empresa.com",
      "role": "USER",
      "active": true
    }
  ]
}
```

**Atualizar usuário (requer ADMIN):**
```json
PUT /api/users/1
Authorization: Bearer <token>

{
  "fullName": "João da Silva Atualizado",
  "role": "ADMIN",
  "active": true
}
```

---

### Códigos de Resposta

| Código | Significado |
|---|---|
| `200` | OK |
| `201` | Criado com sucesso |
| `204` | Sem conteúdo (logout, delete) |
| `400` | Dados inválidos / validação falhou |
| `401` | Não autenticado / token inválido / revogado |
| `403` | Sem permissão (role insuficiente) |
| `404` | Recurso não encontrado |
| `409` | Conflito (email/username duplicado) |
| `429` | Too Many Requests (rate limit atingido) |
| `500` | Erro interno do servidor |

---

## 🔐 Autenticação JWT

O serviço usa **JWT stateless** — nenhuma sessão é armazenada no servidor.

### Fluxo completo

```
1. POST /api/auth/login
   → Retorna { token (24h), refreshToken (7 dias) }

2. Requisições protegidas:
   Authorization: Bearer <token>

3. Token expirou?
   POST /api/auth/refresh  →  { refreshToken }
   → Retorna novo { token, refreshToken }

4. Logout:
   POST /api/auth/logout  →  { refreshToken }
   → Ambos os tokens são adicionados à blacklist no Redis
```

### Configurações

| Propriedade | Valor padrão | Variável |
|---|---|---|
| Algoritmo | HMAC-SHA256 | — |
| Expiração access token | 24 horas | `JWT_EXPIRATION_MS` |
| Expiração refresh token | 7 dias | `JWT_REFRESH_EXPIRATION_MS` |
| Chave | Base64 (mínimo 256 bits) | `JWT_SECRET` |

### Gerar uma chave JWT segura

```bash
openssl rand -base64 32
```

---

## 🔄 Refresh Token

O refresh token permite renovar o access token sem precisar fazer login novamente.

- **Duração:** 7 dias (configurável via `JWT_REFRESH_EXPIRATION_MS`)
- **Armazenado em:** apenas no cliente (stateless no servidor)
- **Revogado em:** logout — adicionado à blacklist do Redis
- **Rejeitado se:** enviado como access token em endpoint protegido (claim `type` verificado)

**Fluxo:**
```
Access token expirou (401)
         ↓
POST /api/auth/refresh  com o refreshToken
         ↓
Novo access token retornado
         ↓
Continue usando a aplicação normalmente
```

---

## 🚫 Logout e Blacklist

O logout invalida os tokens **imediatamente**, antes do vencimento natural.

- Implementado via **Redis** com TTL = tempo restante do token
- O `JwtAuthenticationFilter` checa a blacklist a cada requisição
- Tanto o **access token** quanto o **refresh token** são revogados
- Tokens expirados são ignorados (não precisam ser adicionados)

```
POST /api/auth/logout
Authorization: Bearer <access_token>
Body: { "refreshToken": "..." }

→ Ambos os tokens ficam inválidos imediatamente
→ Novas requisições com esses tokens recebem HTTP 401
```

---

## 🚦 Rate Limiting

O endpoint de criação de usuário tem proteção contra abuso por **Bucket4j + Redis**.

| Configuração | Padrão | Variável |
|---|---|---|
| Máx. requisições | 10 | `RATE_LIMIT_CREATE_USER_CAPACITY` |
| Tokens repostos | 10 | `RATE_LIMIT_CREATE_USER_REFILL` |
| Intervalo de reposição | 60 segundos | `RATE_LIMIT_CREATE_USER_SECONDS` |

- **Escopo:** por IP (suporta `X-Forwarded-For` para proxies)
- **Resposta ao exceder:** `HTTP 429 Too Many Requests`
- **Bucket persistido no Redis:** reinício da aplicação não zera os contadores

---

## 👥 Controle de Acesso por Role

| Role | Criar usuário | Buscar | Atualizar | Deletar | Logout |
|---|---|---|---|---|---|
| `USER` | ✅ público | ✅ | ❌ | ❌ | ✅ |
| `MODERATOR` | ✅ público | ✅ | ❌ | ❌ | ✅ |
| `ADMIN` | ✅ público | ✅ | ✅ | ✅ | ✅ |

> `PUT` e `DELETE` em `/api/users/**` exigem `ROLE_ADMIN`.

---

## 🌐 CORS

As origens permitidas são configuradas via variável de ambiente — **não há wildcard `*` em produção**.

```dotenv
# Desenvolvimento
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200,http://localhost:5173

# Produção — substitua pelos domínios reais do seu frontend
CORS_ALLOWED_ORIGINS=https://app.suaempresa.com,https://admin.suaempresa.com
```

Headers permitidos: `Authorization`, `Content-Type`, `Accept`
Métodos permitidos: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`

---

## 🗃 Migrations com Flyway

O schema do banco é gerenciado pelo **Flyway** em produção.

| Profile | Comportamento |
|---|---|
| `dev` | Flyway **desabilitado** — Hibernate gerencia o schema (`ddl-auto: update`) |
| `test` | Flyway **desabilitado** — H2 em memória com `ddl-auto: create-drop` |
| produção | Flyway **habilitado** — migrations executadas na inicialização (`ddl-auto: validate`) |

**Migrations disponíveis:**

| Versão | Arquivo | Descrição |
|---|---|---|
| V1 | `V1__create_users_table.sql` | Tabela `users` com índices e `CHECK` constraint no `role` |
| V2 | `V2__add_security_fields_and_audit.sql` | Campos de segurança em `users`, tabela `password_reset_tokens` e `audit_log` |

**Schema completo após V2:**

```
users
├── id, full_name, username, email, password, role, active
├── created_at, updated_at
├── last_login_at           ← último login bem-sucedido
├── failed_attempts         ← tentativas falhas consecutivas
└── locked_until            ← bloqueio temporário (nulo = não bloqueado)

password_reset_tokens
├── id, user_id (FK), token (UUID único)
├── expires_at (30 min), used (boolean)
└── created_at

audit_log
├── id, user_id (FK nullable), action
├── entity, entity_id, detail (TEXT)
├── ip_address
└── created_at
```

**Para adicionar uma nova migration:**
```
src/main/resources/db/migration/V3__descricao_da_mudanca.sql
```
> O Flyway executa as migrations em ordem e registra no histórico — **nunca edite um arquivo já aplicado**.

---

## 🌍 Variáveis de Ambiente

| Variável | Obrigatória | Padrão (dev) | Descrição |
|---|---|---|---|
| `DB_URL` | ✅ | `jdbc:postgresql://localhost:5432/userservice_dev` | URL JDBC do PostgreSQL |
| `DB_NAME` | ✅ | `userservice` | Nome do banco (usado no Docker) |
| `DB_USERNAME` | ✅ | `postgres` | Usuário do banco |
| `DB_PASSWORD` | ✅ | `postgres` | Senha do banco |
| `JWT_SECRET` | ✅ | valor de teste | Chave Base64 ≥ 256 bits para assinar tokens |
| `JWT_EXPIRATION_MS` | ❌ | `86400000` (24h) | Expiração do access token |
| `JWT_REFRESH_EXPIRATION_MS` | ❌ | `604800000` (7d) | Expiração do refresh token |
| `REDIS_HOST` | ✅ | `localhost` | Host do Redis |
| `REDIS_PORT` | ❌ | `6379` | Porta do Redis |
| `REDIS_PASSWORD` | ❌ | *(vazio)* | Senha do Redis (se configurada) |
| `MAIL_USERNAME` | ✅ | `dev@localhost` | E-mail remetente |
| `MAIL_PASSWORD` | ✅ | *(vazio)* | Senha / App Password |
| `MAIL_HOST` | ❌ | `smtp.gmail.com` | Servidor SMTP |
| `MAIL_PORT` | ❌ | `587` | Porta SMTP |
| `CORS_ALLOWED_ORIGINS` | ❌ | `http://localhost:3000,...` | Origens CORS separadas por vírgula |
| `RATE_LIMIT_CREATE_USER_CAPACITY` | ❌ | `10` | Capacidade máxima do bucket |
| `RATE_LIMIT_CREATE_USER_REFILL` | ❌ | `10` | Tokens repostos por intervalo |
| `RATE_LIMIT_CREATE_USER_SECONDS` | ❌ | `60` | Intervalo de reposição em segundos |
| `SERVER_PORT` | ❌ | `35698` | Porta HTTP da aplicação |
| `SPRING_PROFILES_ACTIVE` | ❌ | *(vazio)* | `dev` para desenvolvimento local |

---

## 📁 Estrutura do Projeto

```
userservice/
├── src/
│   ├── main/
│   │   ├── java/com/nebula/userService/
│   │   │   ├── configs/
│   │   │   │   ├── JwtAuthenticationFilter.java   # Filtro JWT + blacklist
│   │   │   │   ├── JwtConfig.java                 # Access + refresh token
│   │   │   │   ├── RedisConfig.java               # Cliente Redis/Lettuce
│   │   │   │   ├── ScheduledTasks.java            # Limpeza de tokens expirados
│   │   │   │   ├── SecurityConfig.java            # Segurança, CORS, roles
│   │   │   │   └── SwaggerConfig.java             # OpenAPI
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java            # login, refresh, logout,
│   │   │   │   │                                  # password-reset/request, /confirm
│   │   │   │   └── UserController.java            # CRUD + rate limit
│   │   │   ├── dto/
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── JwtResponse.java               # token + refreshToken
│   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   ├── PasswordResetRequestDTO.java
│   │   │   │   ├── PasswordResetConfirmDTO.java
│   │   │   │   ├── UserRequestDTO.java
│   │   │   │   ├── UserResponseDTO.java
│   │   │   │   ├── UserUpdateDTO.java
│   │   │   │   ├── PaginatedResponseDTO.java
│   │   │   │   └── ErrorResponseDTO.java
│   │   │   ├── entities/
│   │   │   │   ├── UserEntity.java                # + lastLoginAt, failedAttempts, lockedUntil
│   │   │   │   ├── PasswordResetTokenEntity.java  # Tokens de recuperação de senha
│   │   │   │   └── AuditLogEntity.java            # Registro de auditoria
│   │   │   ├── enums/
│   │   │   │   └── Role.java                      # USER | ADMIN | MODERATOR
│   │   │   ├── exception/
│   │   │   │   ├── BusinessException.java
│   │   │   │   ├── UserNotFoundException.java
│   │   │   │   ├── DuplicateEmailException.java
│   │   │   │   ├── DuplicateUsernameException.java
│   │   │   │   └── DatabaseIntegrityException.java
│   │   │   ├── handler/
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── PasswordResetTokenRepository.java
│   │   │   │   └── AuditLogRepository.java
│   │   │   └── service/
│   │   │       ├── AuthService.java               # login+bloqueio, refresh, logout+auditoria
│   │   │       ├── UserService.java               # CRUD + auditoria
│   │   │       ├── EmailService.java
│   │   │       ├── PasswordResetService.java      # Esqueci minha senha
│   │   │       ├── AuditLogService.java           # Auditoria assíncrona
│   │   │       ├── TokenBlacklistService.java     # Blacklist Redis
│   │   │       └── RateLimitService.java          # Bucket4j por IP
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/
│   │           ├── V1__create_users_table.sql
│   │           └── V2__add_security_fields_and_audit.sql
│   └── test/
│       ├── java/com/nebula/userService/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── handler/
│       │   └── service/
│       └── resources/
│           └── application-test.yml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .env.example
├── .gitignore
└── pom.xml
```

---

## 🧪 Testes

O projeto possui **83 testes** com cobertura de **≥ 80%** verificada pelo JaCoCo.

### Rodar todos os testes

```bash
./mvnw test
```

### Rodar com relatório de cobertura

```bash
./mvnw verify
```

O relatório HTML é gerado em:
```
target/site/jacoco/index.html
```

### Suíte de testes

| Classe | Testes | O que cobre |
|---|---|---|
| `UserServiceTest` | 27 | createUser, updateUser, findById, findAll, findByFullName, deleteUser, email |
| `UserControllerTest` | 12 | Todos os endpoints REST, status codes, validações, rate limit mock |
| `GlobalExceptionHandlerTest` | 12 | Todos os handlers de exceção |
| `JwtConfigTest` | 9 | Geração de access/refresh token, parsing, expiração |
| `AuthServiceTest` | 6 | Login com access+refresh, roles, credenciais inválidas |
| `EmailServiceTest` | 7 | Envio, validação de parâmetros, falha SMTP |
| `JwtAuthenticationFilterTest` | 6 | Blacklist check, tipo de token, expirado, inválido |
| `AuthControllerTest` | 4 | Login, validação de campos |

> Os testes usam **H2 em memória** e **mocks do Redis** — nenhuma instância real é necessária.

---

## 🐳 Docker

O projeto possui suporte completo a Docker com **build multi-stage** e **quatro serviços** orquestrados via Docker Compose.

### Serviços

| Container | Imagem | Porta | Descrição |
|---|---|---|---|
| `userservice_app` | Build local (multi-stage) | `35698` | Aplicação Spring Boot |
| `userservice_postgres` | `postgres:16-alpine` | `5432` | Banco de dados |
| `userservice_redis` | `redis:7-alpine` | `6379` | Blacklist + rate limiting |
| `userservice_mailhog` | `mailhog/mailhog` | `1025` / `8025` | SMTP fake (dev) |

### Pré-requisito

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e rodando

### 1. Configure as variáveis

```bash
cp .env.example .env
# Edite .env com DB_PASSWORD e JWT_SECRET obrigatoriamente
```

### 2. Suba todos os serviços

```bash
docker compose up --build
```

Na primeira vez o Docker irá:
1. Baixar as imagens (`postgres:16-alpine`, `redis:7-alpine`, `mailhog/mailhog`, `eclipse-temurin:17`)
2. Compilar o projeto com Maven dentro do container (stage `build`)
3. Gerar a imagem final leve com JRE + JAR (stage `runtime`)
4. Subir os quatro containers — `app` aguarda `postgres` e `redis` ficarem `healthy`

### 3. Verificar se está rodando

```bash
docker compose ps
```

```
NAME                    STATUS          PORTS
userservice_postgres    healthy         0.0.0.0:5432->5432/tcp
userservice_redis       healthy         0.0.0.0:6379->6379/tcp
userservice_mailhog     running         0.0.0.0:1025->1025/tcp, 0.0.0.0:8025->8025/tcp
userservice_app         healthy         0.0.0.0:35698->35698/tcp
```

### URLs disponíveis

| Serviço | URL |
|---|---|
| API | `http://localhost:35698` |
| Swagger UI | `http://localhost:35698/swagger-ui.html` |
| Health Check | `http://localhost:35698/actuator/health` |
| MailHog (e-mails) | `http://localhost:8025` |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |

### Comandos úteis

```bash
# Subir em background
docker compose up -d --build

# Ver logs em tempo real
docker compose logs -f app

# Parar tudo (mantém os dados)
docker compose stop

# Parar e remover containers (mantém os volumes)
docker compose down

# Parar, remover containers E apagar todos os dados
docker compose down -v

# Rebuild apenas da aplicação
docker compose up -d --build app
```

### Dockerfile — Multi-stage

```
Stage 1 (build)   →  eclipse-temurin:17-jdk-alpine  ≈ 400 MB
                      mvnw package -DskipTests
                         ↓
Stage 2 (runtime) →  eclipse-temurin:17-jre-alpine   ≈ 100 MB
                      COPY app.jar → java -jar app.jar
                      USER appuser (não-root)
```

### Para produção

Configure as variáveis reais no `.env` e remova o `mailhog`:

```dotenv
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu@email.com
MAIL_PASSWORD=sua_app_password
CORS_ALLOWED_ORIGINS=https://app.suaempresa.com
SPRING_PROFILES_ACTIVE=
```

---

## 📝 Observações Importantes

- **Flyway em produção:** as migrations são aplicadas automaticamente na inicialização. Nunca edite um arquivo `.sql` já aplicado — crie uma nova versão (`V3__...`).
- **CORS:** configure `CORS_ALLOWED_ORIGINS` com os domínios exatos do seu frontend antes de ir para produção. Nunca use `*` com `allowCredentials: true`.
- **Refresh token:** armazene o refresh token de forma segura no cliente (ex: `httpOnly cookie`). Nunca o exponha em `localStorage`.
- **Rate limiting:** os contadores ficam no Redis — reiniciar a aplicação não os zera. Para ajustar os limites, altere as variáveis `RATE_LIMIT_*` e reinicie.
- **Redis em produção:** configure senha via `REDIS_PASSWORD` e use TLS se o Redis estiver em rede pública.

---

## 👨‍💻 Desenvolvido por

**Nebula** — User Service `v1.0.0`
