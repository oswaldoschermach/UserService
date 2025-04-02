# 📝 User Service API

<div align="center">
  <img src="https://img.shields.io/badge/Java-21+-blue?style=for-the-badge" alt="Java 21+">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen?style=for-the-badge" alt="Spring Boot 3.1.5">
  <img src="https://img.shields.io/badge/PostgreSQL-15+-blue?style=for-the-badge" alt="PostgreSQL 15+">
  <img src="https://img.shields.io/badge/Swagger-2.8.6-green?style=for-the-badge" alt="Swagger 2.8.6">
</div>

## 🌟 Visão Geral
API RESTful para gerenciamento completo de usuários com:

- Autenticação JWT
- Envio de e-mails de confirmação
- Documentação Swagger integrada
- Validações robustas
- Paginação e filtros

### Documentação

A documentação da API pode ser acessada no ambiente através do link:

🔗 **[Swagger - Ambiente](http://44.242.202.136:37989/swagger-ui/index.html#/)**

```yaml
# Configuração Swagger
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tagsSorter: alpha
    operationsSorter: alpha
  api-docs:
    path: /v3/api-docs
```
# ⚙️ Configuração

## 📌 Pré-requisitos

- **Java 21+**
- **Maven 3.9+**
- **PostgreSQL 15+**
- **SMTP Server** (ex: Gmail)

## 🚀 Tecnologias Utilizadas

## 🚀 Tecnologias Utilizadas

- **Spring Boot** 
- **PostgreSQL** 
- **JWT** 
- **Swagger** 

---

# 🛠️ Instalação

## 🔹 Clone o repositório:

```bash
git clone https://github.com/seu-usuario/user-service.git
cd user-service
```

## 🔹 Subindo o banco de dados com Docker

A aplicação pode ser executada utilizando **Docker Compose**, o que facilita a configuração do banco de dados PostgreSQL. Para isso, execute o seguinte comando:

```bash
docker-compose up -d
```

Isso criará e iniciará um container com PostgreSQL já configurado.

### 📄 Arquivo `docker-compose.yml` (apenas dev)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:latest
    container_name: postgres_signuphub
    restart: always
    environment:
      POSTGRES_DB: SignUpHub
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: dm1UZWNub2xvZ2lh
    ports:
      - "37568:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  user_service:
    image: eclipse-temurin:21-jdk
    container_name: user_service
    restart: always
    depends_on:
      - postgres
    ports:
      - "35698:35698"
    volumes:
      - ./userService-0.0.1-SNAPSHOT.jar:/app/userService.jar
    working_dir: /app
    command: ["java", "-jar", "userService.jar"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/SignUpHub
      SPRING_DATASOURCE_USERNAME: admin
      SPRING_DATASOURCE_PASSWORD: dm1UZWNub2xvZ2lh
      SPRING_MAIL_USERNAME: oswaldo.schermach@gmail.com
      SPRING_MAIL_PASSWORD: pcaa cmlc ogrr jwdx
      SERVER_PORT: 35698

volumes:
  postgres_data:
```

## 🔹 Execute a aplicação manualmente (sem Docker):

Se preferir executar sem Docker, siga os passos abaixo:

1. **Configure o banco de dados manualmente** (caso não esteja usando Docker):

```sql
CREATE DATABASE userdb;
CREATE USER admin WITH PASSWORD 'dm1UZWNub2xvZ2lh';
GRANT ALL PRIVILEGES ON DATABASE userdb TO admin;
```

2. **Configure o arquivo `application.yml`**:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: admin
    password: dm1UZWNub2xvZ2lh
  jpa:
    hibernate:
      ddl-auto: update
```

3. **Execute a aplicação:**

```bash
mvn spring-boot:run
```

---

# 🌐 Endpoints Principais

## 🔑 Autenticação

| Método | Endpoint               | Body Request Example |
|--------|------------------------|----------------------|
| **POST** | `/api/auth/createUser` | `{ "email": "user@test.com", ... }` |
| **POST** | `/api/auth/login`      | `{ "email": "user@test.com", ... }` |

## 👤 Usuários

| Método | Endpoint              | Parâmetros |
|--------|----------------------|------------|
| **GET** | `/api/users/{id}`    | `id: Long` |
| **GET** | `/api/users/search`  | `?name=termo&page=0&size=10` |

---

# 📂 Estrutura do Projeto

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── vmt/
│   │           ├── config/
│   │           ├── controller/
│   │           ├── dto/
│   │           ├── exception/
│   │           ├── model/
│   │           ├── repository/
│   │           ├── service/
│   │           └── UserServiceApplication.java
│   └── resources/
│       └── application.yml
```

---

# 🚨 Exceções Personalizadas

| Exceção | HTTP Status | Descrição |
|---------|------------|-----------|
| `DuplicateEmailException` | **409** | E-mail já cadastrado |
| `UserNotFoundException` | **404** | Usuário não encontrado |
| `InvalidRoleException` | **400** | Perfil inválido especificado |

---

# 🔑 Variáveis de Ambiente (apenas para DEV)

```env
# Banco de Dados
DB_URL=jdbc:postgresql://localhost:5432/userdb
DB_USERNAME=admin
DB_PASSWORD=dm1UZWNub2xvZ2lh

# Email (Gmail Example)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=seu-email@gmail.com
SMTP_PASSWORD=sua-senha
```
---

# 📌 Possibilidades para Frontend

A implementação do frontend pode ser feita com diferentes tecnologias. Algumas das principais opções são:

### 1. **Angular**
✅ Prós:
- Estrutura robusta e escalável
- Suporte a TypeScript
- Ferramentas poderosas como RxJS e CLI integrada

❌ Contras:
- Curva de aprendizado maior
- Pode ser pesado para projetos pequenos

### 2. **React**
✅ Prós:
- Grande comunidade e suporte
- Flexibilidade na escolha de bibliotecas
- Performance otimizada com Virtual DOM

❌ Contras:
- Maior necessidade de configuração inicial
- Manutenção de estado pode ser complexa

### 3. **Vue.js**
✅ Prós:
- Simplicidade e curva de aprendizado mais suave
- Abordagem reativa intuitiva
- Melhor desempenho em comparação com Angular

❌ Contras:
- Menor adoção em grandes empresas
- Pode faltar suporte para algumas bibliotecas específicas

A escolha da tecnologia depende das necessidades do projeto, da equipe disponível e dos objetivos a longo prazo. 

---

# 📌 Cobertura de Testes

Os testes unitários foram implementados para garantir a funcionalidade correta dos principais fluxos da API de usuários. A motivação para a cobertura dos métodos e cenários testados é a seguinte:

1. **`createUser_WithValidData_ShouldCreateUser`**
    - **Motivação**: Testar se um usuário é criado corretamente quando os dados são válidos.
    - **Cenário**: Simula o fluxo de criação de um novo usuário garantindo que o repositório e o serviço de e-mails sejam chamados corretamente.

2. **`createUser_WithDuplicateEmail_ShouldThrowException`**
    - **Motivação**: Garantir que não seja possível cadastrar usuários com e-mails duplicados.
    - **Cenário**: Simula a tentativa de cadastro de um usuário com um e-mail já existente, esperando que uma exceção seja lançada.

3. **`updateUser_WithValidData_ShouldUpdateUser`**
    - **Motivação**: Testar a atualização de informações de um usuário existente.
    - **Cenário**: Simula a atualização de nome e perfil de um usuário e verifica se os dados foram salvos corretamente.

4. **`deleteUser_ShouldThrowWhenUserHasRelations`**
    - **Motivação**: Garantir que a exclusão de usuários vinculados a outras entidades seja tratada corretamente.
    - **Cenário**: Simula um cenário onde um usuário com vínculos tenta ser excluído e espera que uma exceção de integridade seja lançada.

5. **`findByFullName_ShouldRejectInvalidPagination`**
    - **Motivação**: Garantir que valores inválidos de paginação não sejam aceitos.
    - **Cenário**: Simula a busca de usuários com parâmetros de paginação inválidos e verifica se exceções são lançadas corretamente.

A cobertura de testes foca em validar regras de negócio críticas, prevenindo falhas comuns e garantindo a robustez do sistema. A abordagem utilizada prioriza testes de unidade para componentes individuais e testes de integração para cenários mais amplos. 
