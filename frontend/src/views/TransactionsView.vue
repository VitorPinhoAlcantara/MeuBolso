<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, reactive, ref } from 'vue'
import BaseConfirmModal from '../components/BaseConfirmModal.vue'
import RowActionButtons from '../components/RowActionButtons.vue'
import { useToast } from '../composables/useToast'
import { api } from '../services/api'
import { transactionTypeLabel } from '../utils/enumLabels'

type TransactionType = 'INCOME' | 'EXPENSE'
type AccountType = 'CASH' | 'BANK' | 'CARD' | 'OTHER'
type CategoryType = 'INCOME' | 'EXPENSE'

type Account = {
  id: string
  name: string
  type: AccountType
}

type Category = {
  id: string
  name: string
  type: CategoryType
}

type Transaction = {
  id: string
  accountId: string
  categoryId: string
  type: TransactionType
  amount: number
  date: string
  description: string
}

type PageResponse<T> = {
  content: T[]
  number: number
  totalPages: number
}

const loading = ref(false)
const saving = ref(false)
const toast = useToast()

const items = ref<Transaction[]>([])
const page = ref(0)
const totalPages = ref(0)

const accounts = ref<Account[]>([])
const categories = ref<Category[]>([])

const showFormModal = ref(false)
const showDeleteModal = ref(false)
const deleting = ref(false)
const selectedDelete = ref<Transaction | null>(null)

const editingId = ref<string | null>(null)

const filters = reactive({
  from: '',
  to: '',
  type: '' as '' | TransactionType,
  accountId: '',
  categoryId: '',
})

const form = reactive({
  accountId: '',
  categoryId: '',
  type: 'EXPENSE' as TransactionType,
  amount: '',
  date: new Date().toISOString().slice(0, 10),
  description: '',
})

const submitLabel = computed(() => (editingId.value ? 'Salvar alterações' : 'Criar transação'))
const modalTitle = computed(() => (editingId.value ? 'Editar transação' : 'Nova transação'))

const filteredCategories = computed(() => categories.value.filter((c) => c.type === form.type))

const accountNameById = (id: string) => accounts.value.find((a) => a.id === id)?.name ?? id.slice(0, 8)
const categoryNameById = (id: string) => categories.value.find((c) => c.id === id)?.name ?? id.slice(0, 8)

const resetForm = () => {
  editingId.value = null
  form.accountId = ''
  form.categoryId = ''
  form.type = 'EXPENSE'
  form.amount = ''
  form.date = new Date().toISOString().slice(0, 10)
  form.description = ''
}

const openCreateModal = () => {
  resetForm()
  showFormModal.value = true
}

const openEditModal = (item: Transaction) => {
  editingId.value = item.id
  form.accountId = item.accountId
  form.categoryId = item.categoryId
  form.type = item.type
  form.amount = String(item.amount)
  form.date = item.date
  form.description = item.description ?? ''
  showFormModal.value = true
}

const closeFormModal = () => {
  showFormModal.value = false
}

const loadAccounts = async () => {
  const response = await api.get<PageResponse<Account>>('/api/v1/accounts', {
    params: { page: 0, size: 200, sort: 'createdAt,desc' },
  })
  accounts.value = response.data.content
}

const loadCategories = async () => {
  const response = await api.get<PageResponse<Category>>('/api/v1/categories', {
    params: { page: 0, size: 200, sort: 'createdAt,desc' },
  })
  categories.value = response.data.content
}

const loadTransactions = async (targetPage = 0) => {
  loading.value = true

  try {
    const response = await api.get<PageResponse<Transaction>>('/api/v1/transactions', {
      params: {
        page: targetPage,
        size: 10,
        sort: 'transactionDate,desc',
        from: filters.from || undefined,
        to: filters.to || undefined,
        type: filters.type || undefined,
        accountId: filters.accountId || undefined,
        categoryId: filters.categoryId || undefined,
      },
    })

    items.value = response.data.content
    page.value = response.data.number
    totalPages.value = response.data.totalPages
  } catch {
    toast.error('Não foi possível carregar as transações.')
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  saving.value = true

  try {
    const payload = {
      accountId: form.accountId,
      categoryId: form.categoryId,
      type: form.type,
      amount: Number(form.amount),
      date: form.date,
      description: form.description || null,
    }

    if (editingId.value) {
      await api.put(`/api/v1/transactions/${editingId.value}`, payload)
      toast.success('Transação atualizada com sucesso.')
    } else {
      await api.post('/api/v1/transactions', payload)
      toast.success('Transação criada com sucesso.')
    }

    showFormModal.value = false
    await loadTransactions(page.value)
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao salvar transação.')
    } else {
      toast.error('Falha ao salvar transação.')
    }
  } finally {
    saving.value = false
  }
}

const askDelete = (item: Transaction) => {
  selectedDelete.value = item
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  if (!selectedDelete.value) return

  deleting.value = true

  try {
    await api.delete(`/api/v1/transactions/${selectedDelete.value.id}`)
    toast.success('Transação excluída com sucesso.')
    showDeleteModal.value = false
    selectedDelete.value = null
    await loadTransactions(page.value)
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao excluir transação.')
    } else {
      toast.error('Falha ao excluir transação.')
    }
  } finally {
    deleting.value = false
  }
}

onMounted(async () => {
  try {
    await Promise.all([loadAccounts(), loadCategories()])
    await loadTransactions()
  } catch {
    toast.error('Não foi possível carregar dados iniciais de transações.')
  }
})
</script>

<template>
  <section class="panel list-panel">
    <header class="list-header">
      <h3>Transações</h3>
      <button class="btn btn-primary" type="button" @click="openCreateModal">Nova transação</button>
    </header>

    <div class="filters">
      <input v-model="filters.from" type="date" />
      <input v-model="filters.to" type="date" />
      <select v-model="filters.type">
        <option value="">Tipo (todos)</option>
        <option value="EXPENSE">Despesas</option>
        <option value="INCOME">Receitas</option>
      </select>
      <select v-model="filters.accountId">
        <option value="">Conta (todas)</option>
        <option v-for="account in accounts" :key="account.id" :value="account.id">{{ account.name }}</option>
      </select>
      <select v-model="filters.categoryId">
        <option value="">Categoria (todas)</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
      </select>
      <button class="btn btn-secondary" @click="loadTransactions(0)">Aplicar</button>
    </div>

    <p v-if="loading" class="muted">Carregando...</p>
    <p v-else-if="!items.length" class="muted">Nenhuma transação encontrada.</p>

    <table v-else class="table">
      <thead>
        <tr>
          <th>Data</th>
          <th>Tipo</th>
          <th>Conta</th>
          <th>Categoria</th>
          <th>Valor</th>
          <th>Descrição</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ new Date(item.date + 'T00:00:00').toLocaleDateString('pt-BR') }}</td>
          <td>{{ transactionTypeLabel[item.type] ?? item.type }}</td>
          <td>{{ accountNameById(item.accountId) }}</td>
          <td>{{ categoryNameById(item.categoryId) }}</td>
          <td :class="item.type === 'EXPENSE' ? 'amount-expense' : 'amount-income'">
            R$ {{ Number(item.amount).toFixed(2) }}
          </td>
          <td>{{ item.description || '-' }}</td>
          <td class="actions">
            <RowActionButtons
              edit-title="Editar transação"
              delete-title="Excluir transação"
              @edit="openEditModal(item)"
              @delete="askDelete(item)"
            />
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="totalPages > 1" class="pager">
      <button class="btn btn-secondary" :disabled="page === 0" @click="loadTransactions(page - 1)">Anterior</button>
      <span>Página {{ page + 1 }} de {{ totalPages }}</span>
      <button class="btn btn-secondary" :disabled="page + 1 >= totalPages" @click="loadTransactions(page + 1)">Próxima</button>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="showFormModal" class="modal-backdrop" @click.self="closeFormModal">
      <section class="modal-card">
        <h3>{{ modalTitle }}</h3>

        <form class="form" @submit.prevent="submit">
          <label class="field">
            <span>Tipo</span>
            <select v-model="form.type" required>
              <option value="EXPENSE">Despesa</option>
              <option value="INCOME">Receita</option>
            </select>
          </label>

          <label class="field">
            <span>Conta</span>
            <select v-model="form.accountId" required>
              <option value="" disabled>Selecione</option>
              <option v-for="account in accounts" :key="account.id" :value="account.id">
                {{ account.name }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>Categoria</span>
            <select v-model="form.categoryId" required>
              <option value="" disabled>Selecione</option>
              <option v-for="category in filteredCategories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>Valor</span>
            <input v-model="form.amount" type="number" min="0.01" step="0.01" required />
          </label>

          <label class="field">
            <span>Data</span>
            <input v-model="form.date" type="date" required />
          </label>

          <label class="field">
            <span>Descrição</span>
            <input v-model.trim="form.description" type="text" maxlength="255" />
          </label>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="closeFormModal">Cancelar</button>
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? 'Salvando...' : submitLabel }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Teleport>

  <BaseConfirmModal
    :open="showDeleteModal"
    title="Excluir transação"
    :message="'Deseja excluir esta transação?'"
    confirm-text="Excluir"
    :loading="deleting"
    @cancel="showDeleteModal = false"
    @confirm="confirmDelete"
  />
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
  font-size: 14px;
}

.actions {
  display: flex;
  justify-content: flex-end;
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

.amount-income {
  color: #15803d;
}

.amount-expense {
  color: #991b1b;
  font-weight: 700;
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
  max-width: 460px;
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

@media (max-width: 1400px) {
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

@media (max-width: 820px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
