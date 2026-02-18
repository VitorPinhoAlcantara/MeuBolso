<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, reactive, ref } from 'vue'
import BaseConfirmModal from '../components/BaseConfirmModal.vue'
import RowActionButtons from '../components/RowActionButtons.vue'
import { useToast } from '../composables/useToast'
import { api } from '../services/api'
import { accountTypeLabel } from '../utils/enumLabels'

type AccountType = 'CASH' | 'BANK' | 'CARD' | 'OTHER'

type Account = {
  id: string
  name: string
  type: AccountType
  currency: string
  balance: number
  createdAt: string
}

type PageResponse<T> = {
  content: T[]
  number: number
  totalPages: number
  first: boolean
  last: boolean
}

const typeOptions: Array<{ label: string; value: AccountType }> = [
  { label: 'Banco', value: 'BANK' },
  { label: 'Cartão', value: 'CARD' },
  { label: 'Dinheiro', value: 'CASH' },
  { label: 'Outro', value: 'OTHER' },
]

const loading = ref(false)
const saving = ref(false)
const toast = useToast()

const items = ref<Account[]>([])
const page = ref(0)
const totalPages = ref(0)

const showFormModal = ref(false)
const showDeleteModal = ref(false)
const deleting = ref(false)
const selectedDelete = ref<Account | null>(null)

const editingId = ref<string | null>(null)
const form = reactive({
  name: '',
  type: 'BANK' as AccountType,
  currency: 'BRL',
  balance: '0.00',
})

const submitLabel = computed(() => (editingId.value ? 'Salvar alterações' : 'Criar conta'))
const modalTitle = computed(() => (editingId.value ? 'Editar conta' : 'Nova conta'))

const formatMoney = (value: number) =>
  new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(Number(value ?? 0))

const loadAccounts = async (targetPage = 0) => {
  loading.value = true

  try {
    const response = await api.get<PageResponse<Account>>('/api/v1/accounts', {
      params: {
        page: targetPage,
        size: 10,
        sort: 'createdAt,desc',
      },
    })
    items.value = response.data.content
    page.value = response.data.number
    totalPages.value = response.data.totalPages
  } catch {
    toast.error('Não foi possível carregar as contas.')
  } finally {
    loading.value = false
  }
}

const openCreateModal = () => {
  editingId.value = null
  form.name = ''
  form.type = 'BANK'
  form.currency = 'BRL'
  form.balance = '0.00'
  showFormModal.value = true
}

const openEditModal = (item: Account) => {
  editingId.value = item.id
  form.name = item.name
  form.type = item.type
  form.currency = item.currency
  form.balance = Number(item.balance ?? 0).toFixed(2)
  showFormModal.value = true
}

const closeFormModal = () => {
  showFormModal.value = false
}

const submit = async () => {
  saving.value = true

  try {
    const payload = {
      name: form.name,
      type: form.type,
      currency: (form.currency || 'BRL').toUpperCase(),
      balance: Number(form.balance || 0),
    }

    if (editingId.value) {
      await api.put(`/api/v1/accounts/${editingId.value}`, payload)
      toast.success('Conta atualizada com sucesso.')
    } else {
      await api.post('/api/v1/accounts', payload)
      toast.success('Conta criada com sucesso.')
    }

    showFormModal.value = false
    await loadAccounts(page.value)
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao salvar conta.')
    } else {
      toast.error('Falha ao salvar conta.')
    }
  } finally {
    saving.value = false
  }
}

const askDelete = (item: Account) => {
  selectedDelete.value = item
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  if (!selectedDelete.value) return

  deleting.value = true

  try {
    await api.delete(`/api/v1/accounts/${selectedDelete.value.id}`)
    toast.success('Conta excluída com sucesso.')
    showDeleteModal.value = false
    selectedDelete.value = null
    await loadAccounts(page.value)
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao excluir conta.')
    } else {
      toast.error('Falha ao excluir conta.')
    }
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  loadAccounts()
})
</script>

<template>
  <section class="panel list-panel">
    <header class="list-header">
      <h3>Contas cadastradas</h3>
      <button class="btn btn-primary" type="button" @click="openCreateModal">Nova conta</button>
    </header>

    <p v-if="loading" class="muted">Carregando...</p>
    <p v-else-if="!items.length" class="muted">Nenhuma conta cadastrada.</p>

    <table v-else class="table">
      <thead>
        <tr>
          <th>Nome</th>
          <th>Tipo</th>
          <th>Moeda</th>
          <th>Saldo</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.name }}</td>
          <td>{{ accountTypeLabel[item.type] ?? item.type }}</td>
          <td>{{ item.currency }}</td>
          <td>{{ formatMoney(item.balance) }}</td>
          <td class="actions">
            <RowActionButtons
              edit-title="Editar conta"
              delete-title="Excluir conta"
              @edit="openEditModal(item)"
              @delete="askDelete(item)"
            />
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="totalPages > 1" class="pager">
      <button class="btn btn-secondary" :disabled="page === 0" @click="loadAccounts(page - 1)">Anterior</button>
      <span>Página {{ page + 1 }} de {{ totalPages }}</span>
      <button class="btn btn-secondary" :disabled="page + 1 >= totalPages" @click="loadAccounts(page + 1)">Próxima</button>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="showFormModal" class="modal-backdrop" @click.self="closeFormModal">
      <section class="modal-card">
        <h3>{{ modalTitle }}</h3>

        <form class="form" @submit.prevent="submit">
          <label class="field">
            <span>Nome</span>
            <input v-model.trim="form.name" type="text" maxlength="100" required />
          </label>

          <label class="field">
            <span>Tipo</span>
            <select v-model="form.type" required>
              <option v-for="option in typeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>Moeda</span>
            <input v-model.trim="form.currency" type="text" minlength="3" maxlength="3" required />
          </label>

          <label class="field">
            <span>Saldo</span>
            <input v-model="form.balance" type="number" step="0.01" />
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
    title="Excluir conta"
    :message="`Deseja excluir a conta ${selectedDelete?.name ?? ''}?`"
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
  max-width: 420px;
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

@media (max-width: 920px) {
  .table {
    display: block;
    overflow-x: auto;
    white-space: nowrap;
  }
}
</style>
