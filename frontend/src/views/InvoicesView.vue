<script setup lang="ts">
import axios from 'axios'
import { onMounted, reactive, ref } from 'vue'
import { Eye, Pencil } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useToast } from '../composables/useToast'
import { api } from '../services/api'

type InvoiceStatus = 'OPEN' | 'CLOSED' | 'PAID' | 'OVERDUE'

type CardInvoice = {
  id: string
  accountId: string
  paymentMethodId: string
  periodYear: number
  periodMonth: number
  closingDate: string
  dueDate: string
  totalAmount: number
  status: InvoiceStatus
}

type PaymentMethod = {
  id: string
  accountId: string
  name: string
  type: 'CARD' | 'PIX' | 'CASH'
}

type Account = {
  id: string
  name: string
  paymentMethods: PaymentMethod[]
}

type PageResponse<T> = {
  content: T[]
  number: number
  totalPages: number
}

const toast = useToast()
const router = useRouter()
const loading = ref(false)
const paying = ref(false)
const canceling = ref(false)

const items = ref<CardInvoice[]>([])
const page = ref(0)
const totalPages = ref(0)

const accounts = ref<Account[]>([])
const methods = ref<Array<PaymentMethod & { accountName: string }>>([])

const filters = reactive({
  accountId: '',
  paymentMethodId: '',
  status: '' as '' | InvoiceStatus,
  year: '',
  month: '',
})

const showPayModal = ref(false)
const showManageModal = ref(false)
const selectedInvoice = ref<CardInvoice | null>(null)
const managingInvoice = ref<CardInvoice | null>(null)
const payForm = reactive({
  fromAccountId: '',
})

const statusLabel: Record<InvoiceStatus, string> = {
  OPEN: 'Aberta',
  CLOSED: 'Fechada',
  PAID: 'Paga',
  OVERDUE: 'Atrasada',
}

const formatMoney = (value: number) => {
  const amount = new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value ?? 0))
  return `$ ${amount}`
}

const formatDate = (isoDate: string) => {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString('pt-BR')
}

const monthLabel = (year: number, month: number) => {
  return new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
    .format(new Date(year, month - 1, 1))
    .replace(/^./, (s) => s.toUpperCase())
}

const accountNameById = (id: string) => accounts.value.find((a) => a.id === id)?.name ?? '-'
const methodNameById = (id: string) => methods.value.find((m) => m.id === id)?.name ?? '-'

const canPay = (invoice: CardInvoice) => invoice.status !== 'PAID' && Number(invoice.totalAmount) > 0

const loadAccounts = async () => {
  const response = await api.get<PageResponse<Account>>('/api/v1/accounts', {
    params: { page: 0, size: 200, sort: 'name,asc' },
  })
  accounts.value = response.data.content
  methods.value = accounts.value.flatMap((account) =>
    (account.paymentMethods ?? [])
      .filter((method) => method.type === 'CARD')
      .map((method) => ({ ...method, accountName: account.name })),
  )
}

const loadInvoices = async (targetPage = 0) => {
  loading.value = true
  try {
    const response = await api.get<PageResponse<CardInvoice>>('/api/v1/invoices', {
      params: {
        page: targetPage,
        size: 10,
        sort: 'periodYear,periodMonth,desc',
        accountId: filters.accountId || undefined,
        paymentMethodId: filters.paymentMethodId || undefined,
        status: filters.status || undefined,
        year: filters.year || undefined,
        month: filters.month || undefined,
      },
    })
    items.value = response.data.content
    page.value = response.data.number
    totalPages.value = response.data.totalPages
  } catch {
    toast.error('Não foi possível carregar as faturas.')
  } finally {
    loading.value = false
  }
}

const openPayModal = (invoice: CardInvoice) => {
  selectedInvoice.value = invoice
  payForm.fromAccountId = ''
  showPayModal.value = true
}

const submitPay = async () => {
  if (!selectedInvoice.value || !payForm.fromAccountId) return
  paying.value = true
  try {
    await api.post(`/api/v1/invoices/${selectedInvoice.value.id}/pay`, {
      fromAccountId: payForm.fromAccountId,
    })
    toast.success('Fatura paga com sucesso.')
    showPayModal.value = false
    await Promise.all([loadAccounts(), loadInvoices(page.value)])
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao pagar fatura.')
    } else {
      toast.error('Falha ao pagar fatura.')
    }
  } finally {
    paying.value = false
  }
}

const openManageModal = (invoice: CardInvoice) => {
  managingInvoice.value = invoice
  showManageModal.value = true
}

const openTransactionsForInvoice = (invoice: CardInvoice) => {
  const closingDate = new Date(`${invoice.closingDate}T00:00:00`)
  const previousClosingDate = new Date(closingDate)
  previousClosingDate.setMonth(previousClosingDate.getMonth() - 1)
  const fromDate = new Date(previousClosingDate)
  fromDate.setDate(fromDate.getDate() + 1)

  const toIso = (date: Date) => {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }

  router.push({
    name: 'transactions',
    query: {
      accountId: invoice.accountId,
      paymentMethodId: invoice.paymentMethodId,
      datePreset: 'CUSTOM',
      from: toIso(fromDate),
      to: toIso(closingDate),
    },
  })
}

const submitCancelPayment = async () => {
  if (!managingInvoice.value) return
  canceling.value = true
  try {
    await api.post(`/api/v1/invoices/${managingInvoice.value.id}/cancel-payment`, {})
    toast.success('Pagamento da fatura cancelado com sucesso.')
    showManageModal.value = false
    await Promise.all([loadAccounts(), loadInvoices(page.value)])
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao cancelar pagamento.')
    } else {
      toast.error('Falha ao cancelar pagamento.')
    }
  } finally {
    canceling.value = false
  }
}

onMounted(async () => {
  try {
    await loadAccounts()
    await loadInvoices()
  } catch {
    toast.error('Não foi possível carregar dados iniciais de faturas.')
  }
})
</script>

<template>
  <section class="panel list-panel">
    <header class="list-header">
      <h3>Faturas de cartão</h3>
    </header>

    <div class="filters">
      <select v-model="filters.accountId">
        <option value="">Conta (todas)</option>
        <option v-for="account in accounts" :key="account.id" :value="account.id">{{ account.name }}</option>
      </select>

      <select v-model="filters.paymentMethodId">
        <option value="">Cartão (todos)</option>
        <option v-for="method in methods" :key="method.id" :value="method.id">
          {{ method.accountName }} - {{ method.name }}
        </option>
      </select>

      <select v-model="filters.status">
        <option value="">Status (todos)</option>
        <option value="OPEN">Aberta</option>
        <option value="CLOSED">Fechada</option>
        <option value="PAID">Paga</option>
        <option value="OVERDUE">Atrasada</option>
      </select>

      <input v-model="filters.year" type="number" min="2000" max="2100" placeholder="Ano" />
      <input v-model="filters.month" type="number" min="1" max="12" placeholder="Mês" />
      <button class="btn btn-secondary" @click="loadInvoices(0)">Aplicar</button>
    </div>

    <p v-if="loading" class="muted">Carregando...</p>
    <div v-else-if="!items.length" class="empty-state">
      <img src="/brand/badge.svg" alt="MeuBolso" />
      <p>Nenhuma fatura encontrada.</p>
    </div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>Competência</th>
          <th>Conta</th>
          <th>Cartão</th>
          <th>Fechamento</th>
          <th>Vencimento</th>
          <th>Valor</th>
          <th>Status</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ monthLabel(item.periodYear, item.periodMonth) }}</td>
          <td>{{ accountNameById(item.accountId) }}</td>
          <td>{{ methodNameById(item.paymentMethodId) }}</td>
          <td>{{ formatDate(item.closingDate) }}</td>
          <td>{{ formatDate(item.dueDate) }}</td>
          <td>{{ formatMoney(item.totalAmount) }}</td>
          <td>{{ statusLabel[item.status] ?? item.status }}</td>
          <td class="actions">
            <button class="icon-btn icon-view" type="button" title="Ver transações da fatura" @click="openTransactionsForInvoice(item)">
              <Eye :size="16" :stroke-width="2.2" />
            </button>
            <button class="icon-btn icon-edit" type="button" title="Editar fatura" @click="openManageModal(item)">
              <Pencil :size="16" :stroke-width="2.2" />
            </button>
            <button
              v-if="canPay(item)"
              class="btn btn-primary btn-sm"
              type="button"
              @click="openPayModal(item)"
            >
              Pagar
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="totalPages > 1" class="pager">
      <button class="btn btn-secondary" :disabled="page === 0" @click="loadInvoices(page - 1)">Anterior</button>
      <span>Página {{ page + 1 }} de {{ totalPages }}</span>
      <button class="btn btn-secondary" :disabled="page + 1 >= totalPages" @click="loadInvoices(page + 1)">Próxima</button>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="showPayModal && selectedInvoice" class="modal-backdrop" @click.self="showPayModal = false">
      <section class="modal-card">
        <h3>Pagar fatura</h3>
        <form class="form" @submit.prevent="submitPay">
          <label class="field">
            <span>Fatura</span>
            <input :value="`${monthLabel(selectedInvoice.periodYear, selectedInvoice.periodMonth)} - ${formatMoney(selectedInvoice.totalAmount)}`" type="text" disabled />
          </label>
          <label class="field">
            <span>Conta pagadora</span>
            <select v-model="payForm.fromAccountId" required>
              <option value="" disabled>Selecione</option>
              <option v-for="account in accounts" :key="account.id" :value="account.id">{{ account.name }}</option>
            </select>
          </label>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="showPayModal = false">Cancelar</button>
            <button type="submit" class="btn btn-primary" :disabled="paying">
              {{ paying ? 'Pagando...' : 'Confirmar pagamento' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Teleport>

  <Teleport to="body">
    <div v-if="showManageModal && managingInvoice" class="modal-backdrop" @click.self="showManageModal = false">
      <section class="modal-card">
        <h3>Editar fatura</h3>
        <form class="form" @submit.prevent>
          <label class="field">
            <span>Fatura</span>
            <input :value="`${monthLabel(managingInvoice.periodYear, managingInvoice.periodMonth)} - ${formatMoney(managingInvoice.totalAmount)}`" type="text" disabled />
          </label>
          <label class="field">
            <span>Status</span>
            <input :value="statusLabel[managingInvoice.status] ?? managingInvoice.status" type="text" disabled />
          </label>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="showManageModal = false">Fechar</button>
            <button
              v-if="managingInvoice.status === 'PAID'"
              type="button"
              class="btn btn-danger-soft"
              :disabled="canceling"
              @click="submitCancelPayment"
            >
              {{ canceling ? 'Cancelando...' : 'Cancelar pagamento' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.panel {
  margin-top: 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 18px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.list-header h3 {
  margin: 0;
}

.filters {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.filters input,
.filters select {
  height: 36px;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 0 10px;
  outline: none;
}

.filters input:focus,
.filters select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.table {
  width: 100%;
  margin-top: 12px;
  border-collapse: collapse;
}

.table th,
.table td {
  text-align: left;
  padding: 10px;
  border-bottom: 1px solid var(--border);
}

.actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.btn-sm {
  height: 34px;
  padding: 0 10px;
  font-size: 13px;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #f8fafc;
  cursor: pointer;
  display: inline-grid;
  place-items: center;
  color: #0f172a;
}

.icon-view:hover {
  background: #e2e8f0;
}

.icon-edit:hover {
  background: #e2e8f0;
}

.pager {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.muted {
  color: var(--muted);
  margin-top: 10px;
}

.empty-state {
  margin-top: 14px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  min-height: 130px;
  display: grid;
  place-items: center;
  gap: 10px;
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

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  display: grid;
  place-items: center;
  z-index: 50;
  padding: 16px;
}

.modal-card {
  width: 100%;
  max-width: 440px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
}

.modal-card h3 {
  margin: 0;
  font-size: 20px;
}

.form {
  margin-top: 14px;
  display: grid;
  gap: 12px;
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-size: 14px;
  color: var(--text);
}

.field input,
.field select {
  height: 40px;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 0 10px;
  outline: none;
}

.field input:focus,
.field select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.modal-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 1100px) {
  .filters {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 920px) {
  .table {
    display: block;
    overflow-x: auto;
    white-space: nowrap;
  }
}

@media (max-width: 680px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
