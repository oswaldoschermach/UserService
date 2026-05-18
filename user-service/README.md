# User Service - pacote de deploy

Este diretorio foi preparado para deploy remoto usando um `app.jar` ja gerado localmente.

## Conteudo

- `app.jar`: aplicacao Spring Boot pronta para execucao
- `Dockerfile`: imagem runtime baseada em Java 17
- `docker-compose.yml`: stack remota com API, PostgreSQL, Redis e MailHog
- `.env`: configuracao pronta para o primeiro deploy
- `deploy.sh`: script para subir e atualizar a stack

## Como usar

```bash
chmod +x deploy.sh
./deploy.sh
```

## Acesso esperado

- API: `http://SEU_HOST:18743`
- Swagger: `http://SEU_HOST:18743/swagger-ui.html`
- Health: `http://SEU_HOST:18743/actuator/health`

Mais detalhes operacionais em `DEPLOY.md`.
