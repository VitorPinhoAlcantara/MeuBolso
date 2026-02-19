# MeuBolso Frontend

Aplicacao web do MeuBolso.

## Versao
- Frontend: `0.3` (definida em `package.json`)

## Responsabilidade
O frontend entrega:
- login/cadastro e sessao autenticada
- renovacao automatica de token (refresh)
- dashboard mensal (resumo, contas, despesas por conta e categoria)
- gestao de contas, categorias e transacoes
- anexos em transacoes (upload, listagem, preview e download)

## Stack
- Vue 3 + TypeScript
- Vite
- Vue Router
- Pinia
- Axios

## Como rodar
Prerequisito:
- Node 20+

Opcao 1 (desenvolvimento local):

1. Configure ambiente:
```bash
cp .env.example .env
```

2. Instale dependencias:
```bash
npm install
```

3. Rode em desenvolvimento:
```bash
npm run dev
```

4. Build de producao:
```bash
npm run build
```

Opcao 2 (release via monorepo):
- Na raiz do projeto, use `docker compose up -d --build`.
- O frontend sobe em `http://localhost:8080` e faz proxy para o backend via Nginx.

## Configuracao
Arquivo: `.env`

Variavel principal:
```env
VITE_API_BASE_URL=http://localhost:4444
```

## Estrutura principal
- `src/layouts/`: estrutura da aplicacao (sidebar + area de conteudo)
- `src/views/`: telas de negocio (dashboard, contas, categorias, transacoes, auth)
- `src/services/`: integracao HTTP com backend
- `src/stores/`: estado global (auth)
- `src/components/`: componentes reutilizaveis

## UX/UI
Decisoes de consistencia visual e layout:
- `frontend/FRONTEND_CONTEXT.md`
