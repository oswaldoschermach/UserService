package com.VMTecnologia.userService.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API do Sistema de Usuários")
                        .version("1.0")
                        .description("""
                            <h2>Descrição Geral</h2>
                            <p>Aplicação para gerenciamento de usuários com:</p>
                            <ul>
                                <li>Cadastro</li>
                                <li>Autenticação</li>
                                <li>Controle de perfis (USER, ADMIN)</li>
                            </ul>
                            
                            <h2>Tecnologias</h2>
                            <ul>
                                <li>Java 17</li>
                                <li>Spring Boot 3</li>
                                <li>Banco de Dados: PostgreSQL</li>
                            </ul>
                            """)
                        .termsOfService("https://seusite.com/terms")
                        .contact(new Contact()
                                .name("Equipe de Suporte")
                                .email("oswaldo.schermach@gmail.com")
                                .url("https://seusite.com/contact"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")))
                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                        .description("Documentação Completa")
                        .url("https://seusite.com/docs"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)));
    }
}