# MeuBolso

> **Seu controle de finanças pessoais, simples e completo.**

API backend para controle de finanças pessoais: contas, categorias, transações (receitas/despesas) e relatórios mensais.  
Projeto pensado para portfólio backend com foco em boas práticas (arquitetura em camadas, validação, documentação, testes e deploy).

---

## ✨ Principais funcionalidades (MVP)
- Autenticação (cadastro/login) com **JWT**
- CRUD de **Contas**
- CRUD de **Categorias**
- CRUD de **Transações** (receita/despesa)
- **Resumo mensal**: saldo do mês, totais por tipo e gastos por categoria
- Documentação automática com **Swagger (OpenAPI)**

---

## 🧰 Stack (padrão de mercado)
- **Java 21** + **Spring Boot 3**
- **Spring Web** (REST)
- **Spring Security** (JWT)
- **PostgreSQL**
- **Flyway** (migrations)
- **Spring Data JPA (Hibernate)**
- **Bean Validation** (Jakarta Validation)
- **Springdoc OpenAPI** (Swagger UI)
- **JUnit 5 + Mockito**
- **Testcontainers** (integração com Postgres real)
- **Docker + Docker Compose**
- **GitHub Actions** (CI)

---

## 🧱 Arquitetura (camadas)
- **Controller**: HTTP/REST e DTOs
- **Service**: regras de negócio e casos de uso
- **Repository**: acesso a dados
- **Domain/Entity**: entidades JPA
- **Security**: autenticação/autorização
- **Exception**: tratamento global de erros

---

## 📦 Como rodar localmente (Docker)

### 1) Subir banco (PostgreSQL)
```bash
docker compose up -d
```

### 2) Rodar a aplicação
```bash
./mvnw spring-boot:run
```

A API sobe em:
- `http://localhost:8080`

Swagger UI:
- `http://localhost:8080/swagger-ui/index.html`

Healthcheck (Actuator, se habilitado):
- `http://localhost:8080/actuator/health`

---

## ⚙️ Variáveis de ambiente (exemplo)
Crie um `.env` (ou configure no seu sistema/IDE):

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=meubolso
DB_USER=meubolso
DB_PASSWORD=meubolso
JWT_SECRET=troque_isto_por_um_segredo_longo
JWT_ACCESS_TTL_MIN=15
JWT_REFRESH_TTL_DAYS=7
```

---

## 🔌 Endpoints (MVP)

### Auth
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`

### Contas
- `GET /api/v1/accounts`
- `POST /api/v1/accounts`
- `GET /api/v1/accounts/{id}`
- `PUT /api/v1/accounts/{id}`
- `DELETE /api/v1/accounts/{id}`

### Categorias
- `GET /api/v1/categories`
- `POST /api/v1/categories`
- `GET /api/v1/categories/{id}`
- `PUT /api/v1/categories/{id}`
- `DELETE /api/v1/categories/{id}`

### Transações
- `GET /api/v1/transactions?from=YYYY-MM-DD&to=YYYY-MM-DD&type=INCOME|EXPENSE&categoryId=&accountId=`
- `POST /api/v1/transactions`
- `GET /api/v1/transactions/{id}`
- `PUT /api/v1/transactions/{id}`
- `DELETE /api/v1/transactions/{id}`

### Relatórios
- `GET /api/v1/reports/monthly?year=2026&month=2`

---

## ✅ Regras de negócio (MVP)
- Transações pertencem a **um usuário**
- Transação tem: `type (INCOME/EXPENSE)`, `amount > 0`, `date`, `description`, `account`, `category`
- Categoria pode ser marcada como `INCOME` ou `EXPENSE` (ou `BOTH`, se você preferir)
- Resumo mensal considera apenas transações do mês e usuário logado

---

## 🧪 Testes
Rodar testes unitários:
```bash
./mvnw test
```

(Quando implementar) Testes de integração com Testcontainers:
```bash
./mvnw -Dtest=*IT test
```

---

## 🗺️ Roadmap (pós-MVP)
- Orçamentos por categoria (limite mensal + alertas)
- Recorrências (assinaturas/salário)
- Importação CSV (Nubank/Inter/etc.) com processamento assíncrono
- Auditoria (eventos) e idempotência
- Cache e rate limit (Redis)
- Deploy (Render/Railway/Fly) e depois AWS

---

## 📄 Licença
MIT (ou a que você preferir).
