package com.nebula.userService.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${APP_EXTERNAL_PORT:18743}") String appExternalPort,
            @Value("${PUBLIC_BASE_URL:}") String publicBaseUrl) {

        List<Server> servers = new ArrayList<>();
        servers.add(new Server()
                .url("/")
                .description("Servidor atual. O Swagger usa automaticamente o mesmo host e porta de onde foi aberto."));
        servers.add(new Server()
                .url("http://localhost:" + appExternalPort)
                .description("Ambiente local via Docker Compose."));

        if (StringUtils.hasText(publicBaseUrl)) {
            servers.add(new Server()
                    .url(publicBaseUrl)
                    .description("Endpoint publico configurado para homologacao ou producao."));
        }

        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .version("1.0.0")
                        .description("""
                                API REST para cadastro, autenticacao e gerenciamento de usuarios.

                                ## Visao geral
                                Esta API oferece:
                                - criacao publica de usuarios com rate limiting por IP
                                - autenticacao com access token e refresh token
                                - logout com blacklist em Redis
                                - recuperacao de senha por token enviado por e-mail
                                - consulta e administracao de usuarios com controle por role

                                ## Fluxo recomendado de autenticacao
                                1. Chame `POST /api/auth/login` com `username` e `password`
                                2. Guarde `token` e `refreshToken`
                                3. Envie o header `Authorization: Bearer <token>` nos endpoints protegidos
                                4. Quando o access token expirar, chame `POST /api/auth/refresh`
                                5. Ao encerrar a sessao, chame `POST /api/auth/logout`

                                ## Perfis e autorizacao
                                - `USER`: leitura autenticada
                                - `ADMIN`: leitura, atualizacao e exclusao de usuarios
                                - `MODERATOR`: role de dominio disponivel para regras futuras

                                ## Convencoes de resposta
                                - `200/201`: operacao concluida com corpo JSON
                                - `204`: operacao concluida sem corpo
                                - `400`: payload invalido ou parametro inconsistente
                                - `401`: autenticacao ausente, invalida ou token invalido
                                - `403`: autenticado sem permissao para a operacao
                                - `404`: recurso nao encontrado
                                - `409`: conflito de negocio
                                - `429`: limite de requisicoes excedido

                                ## Paginacao
                                O endpoint `GET /api/users/search` usa:
                                - `page`: indice iniciado em `0`
                                - `size`: quantidade de itens por pagina

                                ## Ambientes uteis
                                - Swagger local: `http://localhost:%s/swagger-ui.html`
                                - API local: `http://localhost:%s`
                                """.formatted(appExternalPort, appExternalPort))
                        .contact(new Contact()
                                .name("Equipe User Service")
                                .email("dev@nebula.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Informe o access token no formato: `Bearer eyJ...`")));
    }
}
