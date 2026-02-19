# MeuBolso Backend

API REST do MeuBolso.

## Versao
- Backend: `0.3` (definida em `pom.xml`)

## Responsabilidade
O backend concentra:
- autenticacao/autorizacao (JWT access + refresh)
- regras de negocio financeiras
- persistencia em PostgreSQL
- migracoes com Flyway
- anexos de transacao em MinIO
- documentacao OpenAPI/Swagger

## Stack
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA + PostgreSQL
- Flyway
- MinIO (S3 compatible)
- Springdoc OpenAPI
- JUnit 5 + Mockito

## Como rodar
Opcao 1 (local):

1. Suba infraestrutura (PostgreSQL + MinIO):
```bash
docker compose up -d
```

2. Rode a API:
```bash
./mvnw spring-boot:run
```

3. URLs principais:
- API: `http://localhost:4444`
- Swagger: `http://localhost:4444/swagger-ui/index.html`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

Opcao 2 (release via monorepo):
- Na raiz do projeto, use `docker compose up -d --build` para subir frontend + backend + infraestrutura juntos.

## Variaveis de ambiente
Use `backend/.env.example` como base.

Principais:
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

## Endpoints principais
Auth:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

Accounts:
- `GET /api/v1/accounts`
- `POST /api/v1/accounts`
- `GET /api/v1/accounts/{id}`
- `PUT /api/v1/accounts/{id}`
- `DELETE /api/v1/accounts/{id}`

Categories:
- `GET /api/v1/categories`
- `POST /api/v1/categories`
- `GET /api/v1/categories/{id}`
- `PUT /api/v1/categories/{id}`
- `DELETE /api/v1/categories/{id}`

Transactions:
- `GET /api/v1/transactions` (filtros: conta, tipo, categoria, data, busca textual `q`)
- `POST /api/v1/transactions`
- `GET /api/v1/transactions/{id}`
- `PUT /api/v1/transactions/{id}`
- `DELETE /api/v1/transactions/{id}`

Attachments:
- `POST /api/v1/transactions/{id}/attachments`
- `GET /api/v1/transactions/{id}/attachments`
- `GET /api/v1/transactions/{id}/attachments/{attachmentId}/preview`
- `GET /api/v1/transactions/{id}/attachments/{attachmentId}/download`
- `DELETE /api/v1/transactions/{id}/attachments/{attachmentId}`

Reports:
- `GET /api/v1/reports/monthly?year=YYYY&month=MM`

## Regras importantes
- cada usuario acessa apenas os proprios dados
- valor (`amount`) de transacao deve ser maior que zero
- categoria e transacao devem ter tipos compativeis (INCOME/EXPENSE)
- conta/categoria com transacoes vinculadas nao pode ser excluida

## Testes
```bash
./mvnw test
```
