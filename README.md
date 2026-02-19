# MeuBolso

MeuBolso e uma aplicacao de controle financeiro pessoal em formato monorepo, com:
- backend em Spring Boot (API REST + autenticacao JWT + anexos com MinIO)
- frontend em Vue 3 (painel web para uso diario)

## Versao
- Projeto: `2.0`
- Backend: `backend/pom.xml`
- Frontend: `frontend/package.json`

## O que o projeto ja faz
- cadastro e login de usuario (JWT access + refresh token)
- endpoint `/api/v1/auth/me` para dados do usuario logado
- CRUD de contas
- CRUD de categorias
- CRUD de transacoes
- filtros de transacoes por conta, tipo, categoria, periodo e busca textual
- anexos em transacoes (upload, listagem, preview, download e exclusao)
- dashboard mensal com:
  - resumo de receitas, despesas e saldo
  - saldo por conta
  - despesas por conta
  - despesas por categoria

## Estrutura do repositorio
- `backend/`: API Spring Boot, regras de negocio, persistencia, migracoes Flyway
- `frontend/`: aplicacao Vue com autenticacao, dashboard e modulos de gestao

## Como rodar (basico)
Prerequisitos:
- Java 21
- Node 20+
- Docker + Docker Compose

1. Suba infraestrutura do backend (PostgreSQL e MinIO):
```bash
cd backend
docker compose up -d
```

2. Rode a API:
```bash
./mvnw spring-boot:run
```

3. Em outro terminal, rode o frontend:
```bash
cd ../frontend
npm install
npm run dev
```

4. Acessos:
- Frontend: `http://localhost:5173`
- API: `http://localhost:4444`
- Swagger: `http://localhost:4444/swagger-ui/index.html`

## Release 1.0 com Docker (stack completa)
Para subir tudo em um unico comando (frontend + backend + PostgreSQL + MinIO):

1. (Opcional, recomendado) crie `.env` na raiz:
```bash
cp .env.example .env
```

```bash
docker compose up -d --build
```

Acessos:
- App (frontend): `http://localhost:8080`
- API (via frontend proxy): `http://localhost:8080/api`
- Swagger (via frontend proxy): `http://localhost:8080/swagger-ui/index.html`
- PostgreSQL (externo): `localhost:5432`

Portas customizadas no seu ambiente:
- `FRONTEND_PORT` para a porta externa do frontend
- `POSTGRES_PORT` para a porta externa do PostgreSQL

Exemplo:
```bash
FRONTEND_PORT=3000 POSTGRES_PORT=55432 docker compose up -d --build
```

Ou via arquivo `.env` na raiz:
```env
FRONTEND_PORT=3000
POSTGRES_PORT=55432
```

Parar stack:
```bash
docker compose down
```

Parar e remover volumes:
```bash
docker compose down -v
```

## Documentacao por modulo
- Backend: `backend/README.md`
- Frontend: `frontend/README.md`
