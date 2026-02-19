<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, reactive, ref } from 'vue'
import BaseConfirmModal from '../components/BaseConfirmModal.vue'
import RowActionButtons from '../components/RowActionButtons.vue'
import { useToast } from '../composables/useToast'
import { api } from '../services/api'

type PaymentMethodType = 'CARD' | 'PIX' | 'CASH'

type PaymentMethod = {
  id: string
  accountId: string
  name: string
  type: PaymentMethodType
  billingClosingDay: number | null
  billingDueDay: number | null
  creditLimit: number | null
  isDefault: boolean
}

type Account = {
  id: string
  name: string
  balance: number
  paymentMethods: PaymentMethod[]
}

type PageResponse<T> = {
  content: T[]
  number: number
  totalPages: number
}

const methodTypeLabel: Record<PaymentMethodType, string> = {
  CARD: 'Cartão',
  PIX: 'PIX',
  CASH: 'Dinheiro',
}

const methodTypeOptions: Array<{ label: string; value: PaymentMethodType }> = [
  { label: 'Cartão', value: 'CARD' },
  { label: 'Dinheiro', value: 'CASH' },
  { label: 'PIX', value: 'PIX' },
]

const loading = ref(false)
const savingAccount = ref(false)
const savingMethod = ref(false)
const deleting = ref(false)
const toast = useToast()

const items = ref<Account[]>([])
const page = ref(0)
const totalPages = ref(0)

const showAccountModal = ref(false)
const showMethodModal = ref(false)
const showDeleteModal = ref(false)

const deletingKind = ref<'account' | 'method'>('account')
const selectedDelete = ref<{ id: string; label: string } | null>(null)

const editingAccountId = ref<string | null>(null)
const accountForm = reactive({
  name: '',
  balance: '0.00',
})

const editingMethodId = ref<string | null>(null)
const currentMethodAccountId = ref<string | null>(null)
const methodForm = reactive({
  name: '',
  type: 'PIX' as PaymentMethodType,
  billingClosingDay: '',
  billingDueDay: '',
  creditLimit: '',
  isDefault: false,
})

const isCardMethod = computed(() => methodForm.type === 'CARD')

const accountModalTitle = computed(() => (editingAccountId.value ? 'Editar banco' : 'Novo banco'))
const accountSubmitLabel = computed(() => (editingAccountId.value ? 'Salvar alterações' : 'Criar banco'))

const methodModalTitle = computed(() => (editingMethodId.value ? 'Editar método' : 'Novo método de pagamento'))
const methodSubmitLabel = computed(() => (editingMethodId.value ? 'Salvar alterações' : 'Criar método'))

const formatMoney = (value: number) => {
  const amount = new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value ?? 0))
  return `$ ${amount}`
}

const formatCycle = (method: PaymentMethod) => {
  if (method.type !== 'CARD' || !method.billingClosingDay || !method.billingDueDay) return '-'
  return `${method.billingClosingDay}/${method.billingDueDay}`
}

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

const openCreateAccountModal = () => {
  editingAccountId.value = null
  accountForm.name = ''
  accountForm.balance = '0.00'
  showAccountModal.value = true
}

const openEditAccountModal = (account: Account) => {
  editingAccountId.value = account.id
  accountForm.name = account.name
  accountForm.balance = Number(account.balance ?? 0).toFixed(2)
  showAccountModal.value = true
}

const closeAccountModal = () => {
  showAccountModal.value = false
}

const submitAccount = async () => {
  savingAccount.value = true
  try {
    const payload = {
      name: accountForm.name,
      balance: Number(accountForm.balance || 0),
    }

    if (editingAccountId.value) {
      await api.put(`/api/v1/accounts/${editingAccountId.value}`, payload)
      toast.success('Banco atualizado com sucesso.')
    } else {
      await api.post('/api/v1/accounts', payload)
      toast.success('Banco criado com sucesso.')
    }

    showAccountModal.value = false
    await loadAccounts(page.value)
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao salvar banco.')
    } else {
      toast.error('Falha ao salvar banco.')
    }
  } finally {
    savingAccount.value = false
  }
}

const openCreateMethodModal = (accountId: string) => {
  editingMethodId.value = null
  currentMethodAccountId.value = accountId
  methodForm.name = ''
  methodForm.type = 'PIX'
  methodForm.billingClosingDay = ''
  methodForm.billingDueDay = ''
  methodForm.creditLimit = ''
  methodForm.isDefault = false
  showMethodModal.value = true
}

const openEditMethodModal = (method: PaymentMethod) => {
  editingMethodId.value = method.id
  currentMethodAccountId.value = method.accountId
  methodForm.name = method.name
  methodForm.type = method.type
  methodForm.billingClosingDay = method.billingClosingDay?.toString() ?? ''
  methodForm.billingDueDay = method.billingDueDay?.toString() ?? ''
  methodForm.creditLimit = method.creditLimit != null ? String(method.creditLimit) : ''
  methodForm.isDefault = method.isDefault
  showMethodModal.value = true
}

const closeMethodModal = () => {
  showMethodModal.value = false
}

const submitMethod = async () => {
  if (!currentMethodAccountId.value) return
  savingMethod.value = true
  try {
    const payload = {
      name: methodForm.name,
      type: methodForm.type,
      billingClosingDay: isCardMethod.value ? Number(methodForm.billingClosingDay) : null,
      billingDueDay: isCardMethod.value ? Number(methodForm.billingDueDay) : null,
      creditLimit: isCardMethod.value && methodForm.creditLimit !== '' ? Number(methodForm.creditLimit) : null,
      isDefault: methodForm.isDefault,
    }

    if (editingMethodId.value) {
      await api.put(`/api/v1/payment-methods/${editingMethodId.value}`, payload)
      toast.success('Método atualizado com sucesso.')
    } else {
      await api.post(`/api/v1/accounts/${currentMethodAccountId.value}/payment-methods`, payload)
      toast.success('Método criado com sucesso.')
    }

    showMethodModal.value = false
    await loadAccounts(page.value)
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao salvar método.')
    } else {
      toast.error('Falha ao salvar método.')
    }
  } finally {
    savingMethod.value = false
  }
}

const askDeleteAccount = (account: Account) => {
  deletingKind.value = 'account'
  selectedDelete.value = { id: account.id, label: account.name }
  showDeleteModal.value = true
}

const askDeleteMethod = (method: PaymentMethod) => {
  deletingKind.value = 'method'
  selectedDelete.value = { id: method.id, label: method.name }
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  if (!selectedDelete.value) return
  deleting.value = true
  try {
    if (deletingKind.value === 'account') {
      await api.delete(`/api/v1/accounts/${selectedDelete.value.id}`)
      toast.success('Banco excluído com sucesso.')
    } else {
      await api.delete(`/api/v1/payment-methods/${selectedDelete.value.id}`)
      toast.success('Método excluído com sucesso.')
    }
    showDeleteModal.value = false
    selectedDelete.value = null
    await loadAccounts(page.value)
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao excluir.')
    } else {
      toast.error('Falha ao excluir.')
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
      <h3>Bancos cadastrados</h3>
      <button class="btn btn-primary" type="button" @click="openCreateAccountModal">Novo banco</button>
    </header>

    <p v-if="loading" class="muted">Carregando...</p>
    <div v-else-if="!items.length" class="empty-state">
      <img src="/brand/badge.svg" alt="MeuBolso" />
      <p>Nenhum banco cadastrado.</p>
    </div>

    <div v-else class="banks-list">
      <article v-for="account in items" :key="account.id" class="bank-card">
        <header class="bank-header">
          <div>
            <h4>{{ account.name }}</h4>
            <strong>{{ formatMoney(account.balance) }}</strong>
          </div>
          <div class="bank-actions">
            <button class="btn btn-secondary" type="button" @click="openCreateMethodModal(account.id)">
              Novo método
            </button>
            <RowActionButtons
              edit-title="Editar banco"
              delete-title="Excluir banco"
              @edit="openEditAccountModal(account)"
              @delete="askDeleteAccount(account)"
            />
          </div>
        </header>

        <table class="table methods-table">
          <thead>
            <tr>
              <th>Método</th>
              <th>Tipo</th>
              <th>Fech./Venc.</th>
              <th>Limite</th>
              <th>Padrão</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="method in account.paymentMethods" :key="method.id">
              <td>{{ method.name }}</td>
              <td>{{ methodTypeLabel[method.type] ?? method.type }}</td>
              <td>{{ formatCycle(method) }}</td>
              <td>{{ method.type === 'CARD' && method.creditLimit != null ? formatMoney(method.creditLimit) : '-' }}</td>
              <td>{{ method.isDefault ? 'Sim' : '-' }}</td>
              <td class="actions">
                <RowActionButtons
                  edit-title="Editar método"
                  delete-title="Excluir método"
                  @edit="openEditMethodModal(method)"
                  @delete="askDeleteMethod(method)"
                />
              </td>
            </tr>
            <tr v-if="!account.paymentMethods?.length">
              <td colspan="6" class="muted">Nenhum método neste banco.</td>
            </tr>
          </tbody>
        </table>
      </article>
    </div>

    <div v-if="totalPages > 1" class="pager">
      <button class="btn btn-secondary" :disabled="page === 0" @click="loadAccounts(page - 1)">Anterior</button>
      <span>Página {{ page + 1 }} de {{ totalPages }}</span>
      <button class="btn btn-secondary" :disabled="page + 1 >= totalPages" @click="loadAccounts(page + 1)">Próxima</button>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="showAccountModal" class="modal-backdrop" @click.self="closeAccountModal">
      <section class="modal-card">
        <h3>{{ accountModalTitle }}</h3>
        <form class="form" @submit.prevent="submitAccount">
          <label class="field">
            <span>Nome</span>
            <input v-model.trim="accountForm.name" type="text" maxlength="100" required />
          </label>
          <label class="field">
            <span>Saldo</span>
            <input v-model="accountForm.balance" type="number" step="0.01" />
          </label>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="closeAccountModal">Cancelar</button>
            <button type="submit" class="btn btn-primary" :disabled="savingAccount">
              {{ savingAccount ? 'Salvando...' : accountSubmitLabel }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Teleport>

  <Teleport to="body">
    <div v-if="showMethodModal" class="modal-backdrop" @click.self="closeMethodModal">
      <section class="modal-card">
        <h3>{{ methodModalTitle }}</h3>
        <form class="form" @submit.prevent="submitMethod">
          <label class="field">
            <span>Nome</span>
            <input v-model.trim="methodForm.name" type="text" maxlength="100" required />
          </label>
          <label class="field">
            <span>Tipo</span>
            <select v-model="methodForm.type" required>
              <option v-for="option in methodTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <div v-if="isCardMethod" class="field-grid">
            <label class="field">
              <span>Dia de fechamento</span>
              <input v-model="methodForm.billingClosingDay" type="number" min="1" max="31" required />
            </label>
            <label class="field">
              <span>Dia de vencimento</span>
              <input v-model="methodForm.billingDueDay" type="number" min="1" max="31" required />
            </label>
          </div>
          <label v-if="isCardMethod" class="field">
            <span>Limite (opcional)</span>
            <input v-model="methodForm.creditLimit" type="number" step="0.01" min="0" />
          </label>
          <label class="field checkbox-field">
            <input v-model="methodForm.isDefault" type="checkbox" />
            <span>Definir como método padrão</span>
          </label>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="closeMethodModal">Cancelar</button>
            <button type="submit" class="btn btn-primary" :disabled="savingMethod">
              {{ savingMethod ? 'Salvando...' : methodSubmitLabel }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Teleport>

  <BaseConfirmModal
    :open="showDeleteModal"
    :title="deletingKind === 'account' ? 'Excluir banco' : 'Excluir método'"
    :message="`Deseja excluir ${selectedDelete?.label ?? ''}?`"
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

.banks-list {
  margin-top: 12px;
  display: grid;
  gap: 14px;
}

.bank-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
}

.bank-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.bank-header h4 {
  margin: 0 0 2px;
}

.bank-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table {
  width: 100%;
  margin-top: 10px;
  border-collapse: collapse;
}

.table th,
.table td {
  text-align: left;
  padding: 10px;
  border-bottom: 1px solid var(--border);
}

.methods-table th,
.methods-table td {
  padding: 8px;
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

.checkbox-field {
  grid-template-columns: 18px 1fr;
  align-items: center;
}

.checkbox-field input {
  height: auto;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.modal-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 920px) {
  .methods-table {
    display: block;
    overflow-x: auto;
    white-space: nowrap;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
