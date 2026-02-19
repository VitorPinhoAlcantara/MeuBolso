# MeuBolso

Seu controle de financas pessoais, simples e completo.

API REST para contas, categorias, transacoes e relatorio mensal.

## Stack
- Java 21
- Spring Boot 3
- Spring Security + JWT (access e refresh token)
- Spring Data JPA + PostgreSQL
- Flyway
- Springdoc OpenAPI (Swagger)
- JUnit 5 + Mockito

## Como rodar
1. Suba o banco:
```bash
docker compose up -d
```

2. Rode a aplicacao:
```bash
./mvnw spring-boot:run
```

3. Acesse:
- API: `http://localhost:4444`
- Swagger UI: `http://localhost:4444/swagger-ui/index.html`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

## Variaveis de ambiente
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=meubolso
DB_USER=meubolso
DB_PASSWORD=meubolso
JWT_SECRET=change-me-in-env-change-me-in-env-123456
JWT_ACCESS_TTL_MIN=15
JWT_REFRESH_TTL_DAYS=7
SERVER_PORT=4444
MINIO_ENDPOINT=http://localhost:9000
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
MINIO_BUCKET=meubolso-attachments
MINIO_SECURE=false
APP_STORAGE_MAX_FILE_SIZE_BYTES=10485760
```

## MinIO
- O `docker compose up -d` tambem sobe o MinIO e cria automaticamente o bucket `meubolso-attachments`.
- Credenciais padrao:
  - user: `minioadmin`
  - password: `minioadmin123`
- Acesse o console web em `http://localhost:9001`.

## Fluxo rapido de autenticacao (curl)
Registrar usuario:
```bash
curl -X POST http://localhost:4444/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@meubolso.com","password":"123456"}'
```

Login:
```bash
curl -X POST http://localhost:4444/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@meubolso.com","password":"123456"}'
```

Resposta de login/register:
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer"
}
```

Use o access token:
```bash
curl http://localhost:4444/api/v1/accounts \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN"
```

Refresh:
```bash
curl -X POST http://localhost:4444/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"SEU_REFRESH_TOKEN"}'
```

Logout (revoga refresh token):
```bash
curl -X POST http://localhost:4444/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"SEU_REFRESH_TOKEN"}'
```

## Endpoints principais
Auth:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

Accounts:
- `GET /api/v1/accounts?page=0&size=20&sort=createdAt,desc&type=BANK`
- `POST /api/v1/accounts`
- `GET /api/v1/accounts/{id}`
- `PUT /api/v1/accounts/{id}`
- `DELETE /api/v1/accounts/{id}`

Categories:
- `GET /api/v1/categories?page=0&size=20&sort=createdAt,desc&type=EXPENSE`
- `POST /api/v1/categories`
- `GET /api/v1/categories/{id}`
- `PUT /api/v1/categories/{id}`
- `DELETE /api/v1/categories/{id}`

Transactions:
- `GET /api/v1/transactions?page=0&size=20&sort=transactionDate,desc&from=2026-02-01&to=2026-02-28&type=EXPENSE&accountId=&categoryId=`
- `POST /api/v1/transactions`
- `GET /api/v1/transactions/{id}`
- `PUT /api/v1/transactions/{id}`
- `DELETE /api/v1/transactions/{id}`
- `POST /api/v1/transactions/{id}/attachments` (multipart: campo `file`)
- `GET /api/v1/transactions/{id}/attachments`
- `GET /api/v1/transactions/{id}/attachments/{attachmentId}/download`
- `DELETE /api/v1/transactions/{id}/attachments/{attachmentId}`

Reports:
- `GET /api/v1/reports/monthly?year=2026&month=2`

## Regras importantes
- Cada usuario so acessa os proprios dados.
- `amount` de transacao deve ser maior que zero.
- Tipo de categoria deve ser compativel com tipo da transacao.
- Nao e permitido deletar conta/categoria com transacoes vinculadas (`409 Conflict`).

## Testes
```bash
./mvnw test
```
