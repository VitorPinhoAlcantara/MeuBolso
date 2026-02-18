<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../services/api'

type ExpenseByCategoryItem = {
  categoryId: string
  categoryName: string
  total: number
}

type MonthlyReportResponse = {
  year: number
  month: number
  totalIncome: number
  totalExpense: number
  net: number
  expensesByCategory: ExpenseByCategoryItem[]
}

const loading = ref(true)
const error = ref('')
const report = ref<MonthlyReportResponse | null>(null)

const netClass = computed(() => {
  const net = Number(report.value?.net ?? 0)

  if (net < 0) return 'net-critical'
  if (net <= 50) return 'net-low'
  if (net <= 200) return 'net-warning'
  return 'net-good'
})

const loadReport = async () => {
  loading.value = true
  error.value = ''

  try {
    const now = new Date()
    const response = await api.get<MonthlyReportResponse>('/api/v1/reports/monthly', {
      params: {
        year: now.getFullYear(),
        month: now.getMonth() + 1,
      },
    })
    report.value = response.data
  } catch {
    error.value = 'Não foi possível carregar o resumo mensal'
  } finally {
    loading.value = false
  }
}

onMounted(loadReport)
</script>

<template>
  <section class="dashboard-grid">
    <article class="panel">
      <h3>Resumo do mês</h3>

      <p v-if="loading" class="muted">Carregando...</p>
      <p v-else-if="error" class="error">{{ error }}</p>

      <template v-else-if="report">
        <div class="stats">
          <div class="stat">
            <span>Receitas</span>
            <strong class="ok">R$ {{ Number(report.totalIncome).toFixed(2) }}</strong>
          </div>
          <div class="stat">
            <span>Despesas</span>
            <strong class="danger">R$ {{ Number(report.totalExpense).toFixed(2) }}</strong>
          </div>
          <div class="stat">
            <span>Saldo</span>
            <strong :class="netClass">
              R$ {{ Number(report.net).toFixed(2) }}
            </strong>
          </div>
        </div>
      </template>
    </article>

    <article class="panel">
      <h3>Gastos por categoria</h3>

      <p v-if="loading" class="muted">Carregando...</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <ul v-else-if="report?.expensesByCategory?.length" class="category-list">
        <li v-for="item in report.expensesByCategory" :key="item.categoryId">
          <span>{{ item.categoryName }}</span>
          <strong>R$ {{ Number(item.total).toFixed(2) }}</strong>
        </li>
      </ul>
      <p v-else class="muted">Nenhum gasto categorizado no mês.</p>
    </article>
  </section>
</template>

<style scoped>
.dashboard-grid {
  margin-top: 20px;
  display: grid;
  gap: 16px;
  grid-template-columns: 1.2fr 1fr;
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

.muted {
  color: var(--muted);
  margin-top: 12px;
}

.error {
  color: var(--danger);
  margin-top: 12px;
}

.category-list {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.category-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
}

@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
