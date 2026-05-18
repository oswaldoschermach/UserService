# Deploy remoto

Esta pasta foi preparada para ser enviada inteira ao servidor remoto.
Ela ja inclui o `app.jar`, entao o servidor nao precisa compilar o projeto.
Ela tambem ja inclui um `.env` pronto para o primeiro deploy.

## Passos

1. Envie a pasta `user-service` para o servidor
2. Entre nela no terminal
3. Se quiser, ajuste antes:
   - `PUBLIC_BASE_URL`
   - `CORS_ALLOWED_ORIGINS`
   - configuracoes de SMTP real
4. Execute:
   `chmod +x deploy.sh && ./deploy.sh`

## Resultado esperado

- API: `http://SEU_HOST:18743`
- Swagger: `http://SEU_HOST:18743/swagger-ui.html`
- Health: `http://SEU_HOST:18743/actuator/health`

## Observacao

O `docker-compose.yml` desta pasta ja esta pronto para servidor remoto:
- somente a API fica exposta externamente
- PostgreSQL e Redis ficam apenas na rede interna do Docker
- a imagem Docker e montada a partir do `app.jar` ja empacotado nesta pasta
