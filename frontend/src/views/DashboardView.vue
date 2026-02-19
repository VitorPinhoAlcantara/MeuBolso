<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../services/api'
import { accountTypeLabel } from '../utils/enumLabels'

type ExpenseByCategoryItem = {
  categoryId: string
  categoryName: string
  categoryColor?: string
  total: number
}

type ExpenseByAccountItem = {
  accountId: string
  accountName: string
  total: number
}

type MonthlyReportResponse = {
  year: number
  month: number
  totalIncome: number
  totalExpense: number
  net: number
  expensesByAccount: ExpenseByAccountItem[]
  expensesByCategory: ExpenseByCategoryItem[]
}

type AccountType = 'CASH' | 'BANK' | 'CARD' | 'OTHER'

type AccountBalanceItem = {
  id: string
  name: string
  type: AccountType
  balance: number
}

type PageResponse<T> = {
  content: T[]
}

const loading = ref(true)
const error = ref('')
const report = ref<MonthlyReportResponse | null>(null)
const balances = ref<AccountBalanceItem[]>([])

const fallbackCategoryColors = ['#2563EB', '#16A34A', '#F97316', '#8B5CF6', '#06B6D4', '#E11D48', '#F59E0B']

const formatMoney = (value: number) =>
  `R$ ${new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(
    Number(value ?? 0),
  )}`

const normalizeHex = (value?: string) => {
  if (!value) return null
  const hex = value.trim().replace('#', '')
  if (/^[0-9a-fA-F]{6}$/.test(hex)) return `#${hex.toUpperCase()}`
  if (/^[0-9a-fA-F]{3}$/.test(hex)) {
    return `#${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}`.toUpperCase()
  }
  return null
}

const netClass = computed(() => {
  const net = Number(report.value?.net ?? 0)
  if (net < 0) return 'net-critical'
  if (net <= 50) return 'net-low'
  if (net <= 200) return 'net-warning'
  return 'net-good'
})

const sortedBalances = computed(() =>
  [...balances.value].sort((a, b) => Number(b.balance ?? 0) - Number(a.balance ?? 0)),
)

const reportPeriodLabel = computed(() => {
  if (!report.value) return ''
  const year = report.value.year
  const month = report.value.month
  const start = new Date(year, month - 1, 1)
  const end = new Date(year, month, 0)
  const formatDate = (date: Date) =>
    new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date)
  return `Período: ${formatDate(start)} até ${formatDate(end)}`
})

const maxExpenseByAccount = computed(() =>
  Math.max(0, ...(report.value?.expensesByAccount?.map((item) => Number(item.total)) ?? [0])),
)

const accountExpenseWidth = (value: number) => {
  const max = maxExpenseByAccount.value
  if (max <= 0) return 0
  return Math.max(8, Math.round((Number(value) / max) * 100))
}

const categoryLegend = computed(() => {
  const items = report.value?.expensesByCategory ?? []
  return items.map((item, index) => ({
    ...item,
    resolvedColor: normalizeHex(item.categoryColor) ?? fallbackCategoryColors[index % fallbackCategoryColors.length],
  }))
})

const categoryPieStyle = computed(() => {
  const items = categoryLegend.value
  const total = items.reduce((acc, item) => acc + Number(item.total), 0)

  if (!items.length || total <= 0) {
    return { backgroundImage: 'conic-gradient(#e2e8f0 0 100%)' }
  }

  let start = 0
  const slices = items.map((item) => {
    const value = Number(item.total)
    const end = start + (value / total) * 100
    const slice = `${item.resolvedColor} ${start.toFixed(2)}% ${end.toFixed(2)}%`
    start = end
    return slice
  })

  return { backgroundImage: `conic-gradient(${slices.join(', ')})` }
})

const loadDashboard = async () => {
  loading.value = true
  error.value = ''

  try {
    const now = new Date()

    const [reportResponse, accountsResponse] = await Promise.all([
      api.get<MonthlyReportResponse>('/api/v1/reports/monthly', {
        params: {
          year: now.getFullYear(),
          month: now.getMonth() + 1,
        },
      }),
      api.get<PageResponse<AccountBalanceItem>>('/api/v1/accounts', {
        params: {
          page: 0,
          size: 100,
          sort: 'name,asc',
        },
      }),
    ])

    report.value = reportResponse.data
    balances.value = accountsResponse.data.content
  } catch {
    error.value = 'Não foi possível carregar os dados do dashboard.'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <section class="dashboard-layout">
    <div class="dashboard-row top-row">
      <article class="panel">
        <h3>Saldo atual por conta</h3>

        <p v-if="loading" class="muted">Carregando...</p>
        <p v-else-if="error" class="error">{{ error }}</p>

        <ul v-else-if="sortedBalances.length" class="account-balance-list">
          <li v-for="item in sortedBalances" :key="item.id">
            <div>
              <strong>{{ item.name }}</strong>
              <small>{{ accountTypeLabel[item.type] ?? item.type }}</small>
            </div>
            <strong>{{ formatMoney(item.balance) }}</strong>
          </li>
        </ul>

        <p v-else class="muted">Nenhuma conta cadastrada.</p>
      </article>

      <article class="panel">
        <h3>Resumo do mês</h3>
        <p v-if="reportPeriodLabel" class="period-hint">{{ reportPeriodLabel }}</p>

        <p v-if="loading" class="muted">Carregando...</p>
        <p v-else-if="error" class="error">{{ error }}</p>

        <div v-else-if="report" class="stats">
          <div class="stat stat-income">
            <span>Receitas</span>
            <strong class="ok">{{ formatMoney(report.totalIncome) }}</strong>
          </div>
          <div class="stat stat-expense">
            <span>Despesas</span>
            <strong class="danger">{{ formatMoney(report.totalExpense) }}</strong>
          </div>
          <div class="stat stat-balance">
            <span>Saldo</span>
            <strong :class="netClass">
              {{ formatMoney(report.net) }}
            </strong>
          </div>
        </div>
      </article>
    </div>

    <article class="panel">
      <h3>Relatório mensal de despesas por conta</h3>
      <p v-if="reportPeriodLabel" class="period-hint">{{ reportPeriodLabel }}</p>

      <p v-if="loading" class="muted">Carregando...</p>
      <p v-else-if="error" class="error">{{ error }}</p>

      <ul v-else-if="report?.expensesByAccount?.length" class="account-expense-list">
        <li v-for="item in report.expensesByAccount" :key="item.accountId">
          <div class="account-expense-info">
            <span>{{ item.accountName }}</span>
            <strong>{{ formatMoney(item.total) }}</strong>
          </div>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: `${accountExpenseWidth(item.total)}%` }"></div>
          </div>
        </li>
      </ul>

      <p v-else class="muted">Nenhuma despesa por conta no mês.</p>
    </article>

    <article class="panel">
      <h3>Gastos por categoria</h3>
      <p v-if="reportPeriodLabel" class="period-hint">{{ reportPeriodLabel }}</p>

      <p v-if="loading" class="muted">Carregando...</p>
      <p v-else-if="error" class="error">{{ error }}</p>

      <div v-else-if="report" class="category-section">
        <ul v-if="categoryLegend.length" class="category-list">
          <li v-for="item in categoryLegend" :key="item.categoryId">
            <span class="category-name">
              <i class="dot" :style="{ backgroundColor: item.resolvedColor }"></i>
              {{ item.categoryName }}
            </span>
            <strong>{{ formatMoney(item.total) }}</strong>
          </li>
        </ul>
        <p v-else class="muted">Nenhum gasto categorizado no mês.</p>

        <div class="pie-card">
          <div class="pie-chart" :style="categoryPieStyle">
            <div class="pie-hole">
              <span>Total</span>
              <strong>{{ formatMoney(report.totalExpense) }}</strong>
            </div>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.dashboard-layout {
  margin-top: 20px;
  display: grid;
  gap: 16px;
}

.dashboard-row {
  display: grid;
  gap: 16px;
}

.top-row {
  grid-template-columns: 1fr 1fr;
}

.panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 18px;
}

.panel h3 {
  margin: 0;
}

.period-hint {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.stats {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
}

.stat-income {
  background: linear-gradient(135deg, rgba(22, 163, 74, 0.18), rgba(22, 163, 74, 0.08));
  border-color: rgba(22, 163, 74, 0.35);
}

.stat-expense {
  background: linear-gradient(135deg, rgba(153, 27, 27, 0.2), rgba(225, 29, 72, 0.1));
  border-color: rgba(153, 27, 27, 0.38);
}

.stat-balance {
  background: linear-gradient(135deg, rgba(71, 85, 105, 0.12), rgba(100, 116, 139, 0.06));
  border-color: rgba(100, 116, 139, 0.35);
}

.stat span {
  color: var(--muted);
}

.ok {
  color: #15803d;
}

.danger {
  color: #991b1b;
}

.net-critical {
  color: #991b1b;
  font-weight: 700;
}

.net-low {
  color: #e11d48;
  font-weight: 700;
}

.net-warning {
  color: #feaf33;
  font-weight: 700;
}

.net-good {
  color: #15803d;
  font-weight: 700;
}

.account-balance-list,
.account-expense-list,
.category-list {
  list-style: none;
  margin: 14px 0 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.account-balance-list li,
.category-list li {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.account-balance-list strong {
  display: block;
}

.account-balance-list small {
  color: var(--muted);
}

.account-expense-list li {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
}

.account-expense-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-track {
  height: 8px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
}

.category-section {
  margin-top: 14px;
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr 340px;
  align-items: start;
}

.pie-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  display: grid;
  place-items: center;
}

.pie-chart {
  width: 260px;
  height: 260px;
  border-radius: 50%;
  position: relative;
}

.pie-hole {
  position: absolute;
  inset: 50%;
  transform: translate(-50%, -50%);
  width: 126px;
  height: 126px;
  border-radius: 50%;
  background: var(--surface);
  border: 1px solid var(--border);
  display: grid;
  place-content: center;
  text-align: center;
  gap: 2px;
}

.pie-hole span {
  font-size: 12px;
  color: var(--muted);
}

.pie-hole strong {
  font-size: 14px;
}

.category-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  flex: 0 0 auto;
}

.muted {
  color: var(--muted);
  margin-top: 12px;
}

.error {
  color: var(--danger);
  margin-top: 12px;
}

@media (max-width: 1200px) {
  .top-row {
    grid-template-columns: 1fr;
  }

  .category-section {
    grid-template-columns: 1fr;
  }

  .pie-card {
    justify-items: center;
  }
}
</style>
