package com.VMTecnologia.userService.configs;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gerenciamento de Usuários")
                        .version("1.0")
                        .description("""
                            <h2>Descrição Geral</h2>
                            <p>Esta API permite o gerenciamento de usuários, oferecendo funcionalidades como:</p>
                            <ul>
                                <li>Cadastro de usuários</li>
                                <li>Autenticação via JWT</li>
                                <li>Listagem e busca de usuários</li>
                            </ul>
                            
                            <h2>Tecnologias Utilizadas</h2>
                            <ul>
                                <li><strong>Linguagem:</strong> Java 21</li>
                                <li><strong>Framework:</strong> Spring Boot 3</li>
                                <li><strong>Banco de Dados:</strong> PostgreSQL</li>
                                <li><strong>Segurança:</strong> JWT (JSON Web Token)</li>
                            </ul>
                            """)
                        .termsOfService("https://oswaldoschermach.com/terms")
                        .contact(new Contact()
                                .name("Oswaldo Schermach")
                                .email("oswaldo.schermach@gmail.com")
                                .url("https://www.linkedin.com/in/oswaldoschermach/"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentação Completa")
                        .url("https://oswaldoschermach.com/docs"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
