# Frontend Context (MeuBolso)

Este arquivo guarda as decisoes de UI/UX do frontend para manter consistencia ao longo do projeto.
Quando alguma direcao mudar, atualizar este arquivo primeiro.

## Objetivo atual
- Entregar um frontend funcional e limpo, sem foco em "showcase de design".
- Priorizar usabilidade, consistencia visual e velocidade de implementacao.

## Stack definida
- Vue 3
- Vite
- TypeScript
- Vue Router
- Pinia
- Axios
- TailwindCSS

## Direcao visual atual
- Base clara (branco/cinza claro) com detalhes em azul.
- Interface minimalista e objetiva.
- Evitar visual "dashboard generico de cards".

## Paleta (v1)
- `bg`: `#F8FAFC`
- `surface`: `#FFFFFF`
- `primary`: `#1D4ED8`
- `primary-600`: `#2563EB`
- `text`: `#0F172A`
- `muted`: `#64748B`
- `border`: `#E2E8F0`
- `success`: `#16A34A`
- `danger`: `#DC2626`
- `warning`: `#D97706`

## Layout (v1)
- Sidebar fixa como navegacao principal.
- Sem header global obrigatorio.
- Cada pagina pode ter topbar local (titulo + acoes/filtros).
- Estrategia principal: "list-first UI" + split layout quando fizer sentido.

## Regras de UX
- Nunca usar `window.confirm`.
- Toda confirmacao sensivel deve usar modal custom reutilizavel.
- Exibir estados claros: loading, vazio, erro.
- Feedback de acao com toast/snackbar (sucesso/erro).
- Formularios com validacao legivel e mensagens objetivas.

## Componentes base planejados
- `BaseButton`
- `BaseInput`
- `BaseSelect`
- `BaseCard` (uso pontual, nao dominar layout)
- `BaseTable`
- `BaseModal`
- `ConfirmModal`
- `BaseBadge`

## Padrões de consistencia
- Espaçamento: escala 8/12/16/24/32.
- Bordas sutis e contraste moderado.
- Tipografia simples e legivel.
- Mesmos padroes de acao para criar/editar/excluir em todas as telas.

## Rotas/paginas alvo (MVP front)
- Auth: login/register
- Dashboard
- Accounts
- Categories
- Transactions
- Profile (`/auth/me`)

## Mudancas futuras
- O layout pode mudar no futuro.
- Qualquer mudanca estrutural deve:
  1. Atualizar este arquivo.
  2. Explicar o motivo da mudanca.
  3. Indicar impacto nos componentes base.
