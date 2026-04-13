# =============================================================
# Stage 1 ? Build
# Compila o projeto com Maven sem precisar do Maven instalado
# =============================================================
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copia apenas os arquivos de dependência primeiro (melhora cache do Docker)
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Baixa as dependências (esta camada só é refeita se o pom.xml mudar)
RUN ./mvnw dependency:go-offline -q

# Copia o código-fonte e gera o JAR pulando os testes
COPY src ./src
RUN ./mvnw package -DskipTests -q

# =============================================================
# Stage 2 ? Runtime
# Imagem final leve, sem Maven nem código-fonte
# =============================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copia apenas o JAR gerado no stage anterior
COPY --from=build /app/target/userService-*.jar app.jar

# Porta exposta
EXPOSE 35698

# Health check embutido
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:35698/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
