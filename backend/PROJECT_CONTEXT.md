# MeuBolso — Contexto Completo do Projeto (para IA)

## 1) Visão geral
MeuBolso é um backend (API REST) para controle de finanças pessoais. O sistema permite que usuários registrem:
- contas (carteira, banco, cartão etc.)
- categorias (alimentação, aluguel, transporte...)
- transações (receitas e despesas)
- relatórios mensais (saldo, totais, gastos por categoria/mês)

Objetivo: ser um projeto de portfólio backend com padrão de mercado, demonstrando boas práticas:
- autenticação com JWT
- arquitetura em camadas
- validação e tratamento de erros
- documentação Swagger (OpenAPI)
- testes unitários e de integração (Testcontainers)
- migrations com Flyway
- containerização com Docker/Compose
- CI com GitHub Actions
- pronto para deploy

Tagline: **"Seu controle de finanças pessoais, simples e completo."**

---

## 2) Stack e padrões escolhidos
**Linguagem/Framework**
- Java 21 (LTS)
- Spring Boot 3.x

**Banco**
- PostgreSQL

**Persistência**
- Spring Data JPA (Hibernate)

**Migrations**
- Flyway (SQL versionado em `src/main/resources/db/migration`)

**Segurança**
- Spring Security
- JWT (Access Token + Refresh Token)
- Roles: `USER` (padrão) e opcionalmente `ADMIN` (futuro)

**DTO/Validação**
- DTOs para requests/responses
- Bean Validation (Jakarta): `@NotNull`, `@Positive`, `@Size`, `@Email`, etc.
- (Opcional) MapStruct para mapear Entity <-> DTO

**Documentação**
- Springdoc OpenAPI + Swagger UI

**Testes**
- Unit: JUnit 5 + Mockito
- Integração: Testcontainers com PostgreSQL real

**Infra**
- Docker + Docker Compose (app + postgres)
- CI: GitHub Actions (build/test)

---

## 3) Público-alvo e uso real
- Público: o próprio usuário (uso pessoal) e demonstração de portfólio.
- Uso real: registrar entradas/saídas e acompanhar saldo e gastos por categoria/mês.

---

## 4) Escopo do MVP (mínimo viável)
### Módulos
1) Auth
2) Accounts (Contas)
3) Categories (Categorias)
4) Transactions (Transações)
5) Monthly Report (Relatório mensal)

### O que deve existir no MVP (Definition of Done)
- API rodando localmente com docker compose
- Migrações Flyway criando o schema
- Endpoints CRUD básicos para contas, categorias e transações
- JWT funcionando (register/login/refresh)
- Swagger UI documentando endpoints e DTOs
- Validação + handler global de erros padronizado
- Testes unitários de services e pelo menos 1 integração com Postgres (Testcontainers)

---

## 5) Requisitos funcionais (detalhados)
### 5.1 Autenticação
- Registrar usuário com email e senha
- Login retorna `accessToken` e `refreshToken`
- Refresh gera novo accessToken (e opcionalmente rotaciona refresh)
- Logout (opcional no MVP): invalidar refresh token

### 5.2 Contas (Accounts)
- CRUD completo
- Campos sugeridos:
  - `id` (UUID)
  - `userId` (UUID)
  - `name` (string, obrigatório)
  - `type` (enum: CASH, BANK, CARD, OTHER) [opcional]
  - `currency` (ex: BRL) [opcional]
  - `createdAt`, `updatedAt`

### 5.3 Categorias (Categories)
- CRUD completo
- Campos sugeridos:
  - `id` (UUID)
  - `userId` (UUID)
  - `name` (string, obrigatório)
  - `type` (enum: INCOME, EXPENSE, BOTH) — recomendação: começar com INCOME/EXPENSE
  - `color` (string) [opcional]
  - `createdAt`, `updatedAt`

### 5.4 Transações (Transactions)
- CRUD completo
- Campos sugeridos:
  - `id` (UUID)
  - `userId` (UUID)
  - `accountId` (UUID, obrigatório)
  - `categoryId` (UUID, obrigatório)
  - `type` (INCOME/EXPENSE)
  - `amount` (decimal > 0)
  - `date` (YYYY-MM-DD)
  - `description` (string, opcional)
  - `createdAt`, `updatedAt`

- Filtros no `GET /transactions`:
  - intervalo `from`/`to`
  - `type`
  - `accountId`
  - `categoryId`

### 5.5 Relatório mensal
Endpoint:
- `GET /api/v1/reports/monthly?year=YYYY&month=MM`

Retorna:
- `month`, `year`
- `totalIncome`
- `totalExpense`
- `net` (= income - expense)
- `expensesByCategory` (lista de {categoryId, categoryName, total})
- (Opcional) `balanceByAccount` (soma por conta)

---

## 6) Regras de negócio (MVP)
- Todo dado pertence ao usuário autenticado (`userId`).
- O usuário não pode acessar dados de outro usuário.
- `amount` sempre positivo; a direção é definida por `type`.
- `date` obrigatório.
- `category.type` deve ser compatível com a transaction (ex: categoria EXPENSE não pode ser usada em INCOME, se você aplicar essa regra no MVP).
- Deleção:
  - Se uma categoria/conta estiver sendo usada por transações, decidir:
    - (Recomendação MVP) bloquear delete com erro 409, ou
    - soft delete.

---

## 7) Requisitos não-funcionais
- Paginação em listagens (opcional no MVP, recomendado no Plus)
- Tratamento de erros consistente (Problem Details ou padrão próprio)
- Logs estruturados (pelo menos com requestId)
- Segurança básica:
  - senha hash (BCrypt)
  - JWT secret via env var
- Performance: queries básicas bem feitas (indexes em userId/date)

---

## 8) Modelo de dados (tabelas sugeridas)
### users
- id UUID PK
- email unique
- password_hash
- created_at, updated_at

### refresh_tokens (opcional mas recomendado)
- id UUID PK
- user_id FK
- token_hash (armazenar hash)
- expires_at
- revoked_at (nullable)
- created_at

### accounts
- id UUID PK
- user_id FK
- name
- type
- currency
- created_at, updated_at

### categories
- id UUID PK
- user_id FK
- name
- type
- color
- created_at, updated_at

### transactions
- id UUID PK
- user_id FK
- account_id FK
- category_id FK
- type
- amount (NUMERIC(14,2))
- date (DATE)
- description
- created_at, updated_at

**Índices recomendados**
- transactions(user_id, date)
- transactions(user_id, category_id)
- transactions(user_id, account_id)

---

## 9) API — padrões e convenções
- Base path: `/api/v1`
- JSON padrão:
  - usar `camelCase` nos DTOs
- Respostas:
  - 200/201 ok
  - 400 validação
  - 401 não autenticado
  - 403 sem permissão
  - 404 não encontrado
  - 409 conflito (ex: tentativa de deletar conta/categoria em uso)
- Ids: UUID
- Versionamento via path (`/v1`)

---

## 10) Estrutura sugerida do projeto (packages)
- `br.com.meubolso`
  - `config`
  - `controller`
  - `dto`
  - `domain` (ou `entity`)
  - `repository`
  - `service`
  - `security`
  - `exception`
  - `util`

---

## 11) Plano de execução (para não desistir)
### Sprint 1 (colocar no ar local + swagger)
1. Criar projeto Spring Boot
2. Subir PostgreSQL via docker compose
3. Configurar Flyway + primeira migration (users)
4. Implementar Auth básico (register/login) + JWT
5. Swagger UI funcionando

### Sprint 2 (CRUD + regras)
6. CRUD Accounts
7. CRUD Categories
8. CRUD Transactions + filtros

### Sprint 3 (relatórios + testes + qualidade)
9. Endpoint relatório mensal
10. Handler global de erros
11. Testes unitários de service
12. 1+ teste de integração com Testcontainers

### Sprint 4 (polimento portfolio)
13. README, exemplos curl, diagrama simples (opcional)
14. GitHub Actions (build + test)
15. Deploy (opcional no MVP, recomendado)

---

## 12) Extras (Plus / diferenciação)
- Orçamento por categoria (budget mensal)
- Recorrência (assinaturas e salário)
- Importação CSV (fila/async)
- Auditoria de eventos (transaction_created, etc.)
- Rate limiting e cache com Redis
- Observabilidade: Actuator + OpenTelemetry

---

## 13) Decisões importantes (registradas)
- Projeto: **MeuBolso**
- Tagline: “Seu controle de finanças pessoais, simples e completo.”
- Foco: backend e portfólio
- Stack: Java 21 + Spring Boot 3 + PostgreSQL + Flyway + Swagger + Testcontainers
