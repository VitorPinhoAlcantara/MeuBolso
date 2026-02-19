<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-vue-next'
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
const showMonthModal = ref(false)

const now = new Date()
const selectedYear = ref(now.getFullYear())
const selectedMonth = ref(now.getMonth() + 1)
const modalYear = ref(selectedYear.value)
const modalMonth = ref(selectedMonth.value)

const monthOptions = [
  { value: 1, label: 'Janeiro' },
  { value: 2, label: 'Fevereiro' },
  { value: 3, label: 'Março' },
  { value: 4, label: 'Abril' },
  { value: 5, label: 'Maio' },
  { value: 6, label: 'Junho' },
  { value: 7, label: 'Julho' },
  { value: 8, label: 'Agosto' },
  { value: 9, label: 'Setembro' },
  { value: 10, label: 'Outubro' },
  { value: 11, label: 'Novembro' },
  { value: 12, label: 'Dezembro' },
]

const fallbackCategoryColors = ['#2563EB', '#16A34A', '#F97316', '#8B5CF6', '#06B6D4', '#E11D48', '#F59E0B']
const fallbackAccountColors = ['#2563EB', '#16A34A', '#F97316', '#8B5CF6', '#0EA5E9', '#F59E0B', '#E11D48', '#14B8A6']

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

const monthLabel = computed(() => {
  const label = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' }).format(
    new Date(selectedYear.value, selectedMonth.value - 1, 1),
  )
  return label.charAt(0).toUpperCase() + label.slice(1)
})

const expenseByAccountLegend = computed(() => {
  const items = report.value?.expensesByAccount ?? []
  const total = items.reduce((acc, item) => acc + Number(item.total), 0)

  return items.map((item, index) => {
    const value = Number(item.total)
    const percent = total > 0 ? (value / total) * 100 : 0
    return {
      ...item,
      value,
      percent,
      color: fallbackAccountColors[index % fallbackAccountColors.length],
    }
  })
})

const expenseByAccountBarStyle = computed(() => {
  const segments = expenseByAccountLegend.value
  if (!segments.length) {
    return { background: '#e2e8f0' }
  }

  let start = 0
  const gradientParts = segments.map((segment) => {
    const end = start + segment.percent
    const part = `${segment.color} ${start.toFixed(2)}% ${end.toFixed(2)}%`
    start = end
    return part
  })

  return {
    background: `linear-gradient(90deg, ${gradientParts.join(', ')})`,
  }
})

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
    const [reportResponse, accountsResponse] = await Promise.all([
      api.get<MonthlyReportResponse>('/api/v1/reports/monthly', {
        params: {
          year: selectedYear.value,
          month: selectedMonth.value,
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

const prevMonth = async () => {
  if (selectedMonth.value === 1) {
    selectedMonth.value = 12
    selectedYear.value -= 1
  } else {
    selectedMonth.value -= 1
  }
  await loadDashboard()
}

const nextMonth = async () => {
  if (selectedMonth.value === 12) {
    selectedMonth.value = 1
    selectedYear.value += 1
  } else {
    selectedMonth.value += 1
  }
  await loadDashboard()
}

const openMonthModal = () => {
  modalYear.value = selectedYear.value
  modalMonth.value = selectedMonth.value
  showMonthModal.value = true
}

const applyMonthSelection = async () => {
  selectedYear.value = modalYear.value
  selectedMonth.value = modalMonth.value
  showMonthModal.value = false
  await loadDashboard()
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

        <div v-else class="empty-state">
          <img src="/brand/badge.svg" alt="MeuBolso" />
          <p>Nenhuma conta cadastrada.</p>
        </div>
      </article>

      <article class="panel">
        <header class="panel-header">
          <div>
            <h3>Resumo do mês</h3>
            <p class="month-hint">{{ monthLabel }}</p>
          </div>
          <div class="month-controls">
            <button type="button" class="icon-btn" title="Mês anterior" @click="prevMonth">
              <ChevronLeft :size="18" />
            </button>
            <button type="button" class="icon-btn" title="Próximo mês" @click="nextMonth">
              <ChevronRight :size="18" />
            </button>
            <button type="button" class="icon-btn" title="Selecionar mês" @click="openMonthModal">
              <CalendarDays :size="18" />
            </button>
          </div>
        </header>

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
      <p class="month-hint">{{ monthLabel }}</p>

      <p v-if="loading" class="muted">Carregando...</p>
      <p v-else-if="error" class="error">{{ error }}</p>

      <template v-else-if="expenseByAccountLegend.length">
        <div class="stacked-bar-track">
          <div class="stacked-bar-fill" :style="expenseByAccountBarStyle"></div>
        </div>

        <ul class="account-expense-list">
          <li v-for="item in expenseByAccountLegend" :key="item.accountId">
            <span class="account-expense-name">
              <i class="dot" :style="{ backgroundColor: item.color }"></i>
              {{ item.accountName }}
            </span>
            <div class="account-expense-meta">
              <small>{{ item.percent.toFixed(1) }}%</small>
              <strong>{{ formatMoney(item.value) }}</strong>
            </div>
          </li>
        </ul>
      </template>

      <div v-else class="empty-state">
        <img src="/brand/badge.svg" alt="MeuBolso" />
        <p>Nenhuma despesa por conta no mês.</p>
      </div>
    </article>

    <article class="panel">
      <h3>Gastos por categoria</h3>
      <p class="month-hint">{{ monthLabel }}</p>

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
        <div v-else class="empty-state">
          <img src="/brand/badge.svg" alt="MeuBolso" />
          <p>Nenhum gasto categorizado no mês.</p>
        </div>

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

  <Teleport to="body">
    <div v-if="showMonthModal" class="modal-backdrop" @click.self="showMonthModal = false">
      <section class="modal-card">
        <h3>Selecionar mês</h3>
        <div class="modal-fields">
          <label class="field">
            <span>Mês</span>
            <select v-model.number="modalMonth">
              <option v-for="option in monthOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>Ano</span>
            <input v-model.number="modalYear" type="number" min="2000" max="2100" />
          </label>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-secondary" @click="showMonthModal = false">Cancelar</button>
          <button type="button" class="btn btn-primary" @click="applyMonthSelection">Aplicar</button>
        </div>
      </section>
    </div>
  </Teleport>
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

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.month-hint {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.month-controls {
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: #fff;
  color: #334155;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.icon-btn:hover {
  background: #f8fafc;
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
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.account-expense-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.account-expense-meta {
  display: inline-flex;
  align-items: baseline;
  gap: 12px;
}

.account-expense-meta small {
  color: var(--muted);
  font-size: 12px;
}

.stacked-bar-track {
  margin-top: 14px;
  margin-bottom: 12px;
  height: 16px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #e2e8f0;
  overflow: hidden;
}

.stacked-bar-fill {
  height: 100%;
  border-radius: 999px;
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

.empty-state {
  margin-top: 12px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  min-height: 120px;
  display: grid;
  place-items: center;
  gap: 8px;
  text-align: center;
  color: var(--muted);
  padding: 14px;
}

.empty-state img {
  width: 44px;
  height: 44px;
  opacity: 0.9;
}

.empty-state p {
  margin: 0;
}

.error {
  color: var(--danger);
  margin-top: 12px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  display: grid;
  place-items: center;
  z-index: 60;
  padding: 16px;
}

.modal-card {
  width: 100%;
  max-width: 380px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  padding: 18px;
}

.modal-card h3 {
  margin: 0;
}

.modal-fields {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-size: 13px;
  color: #334155;
}

.field input,
.field select {
  height: 40px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
  padding: 0 10px;
}

.modal-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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
