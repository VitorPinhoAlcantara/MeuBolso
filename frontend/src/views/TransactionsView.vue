<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, reactive, ref } from 'vue'
import { Download, Eye, Pencil, UploadCloud, X } from 'lucide-vue-next'
import BaseConfirmModal from '../components/BaseConfirmModal.vue'
import { useToast } from '../composables/useToast'
import { api } from '../services/api'
import { transactionTypeLabel } from '../utils/enumLabels'

type TransactionType = 'INCOME' | 'EXPENSE'
type AccountType = 'CASH' | 'BANK' | 'CARD' | 'OTHER'
type CategoryType = 'INCOME' | 'EXPENSE'
type DatePreset = 'ALL' | 'THIS_MONTH' | 'LAST_7_DAYS' | 'LAST_30_DAYS' | 'CUSTOM'

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
  attachmentsCount?: number
}

type TransactionAttachment = {
  id: string
  transactionId: string
  fileName: string
  contentType: string
  sizeBytes: number
  createdAt: string
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
const showViewModal = ref(false)
const showDeleteModal = ref(false)
const deleting = ref(false)
const selectedDelete = ref<Transaction | null>(null)

const editingId = ref<string | null>(null)
const attachments = ref<TransactionAttachment[]>([])
const attachmentsLoading = ref(false)
const attachmentUploading = ref(false)
const selectedAttachment = ref<File | null>(null)
const attachmentInput = ref<HTMLInputElement | null>(null)
const isDraggingAttachment = ref(false)
const selectedView = ref<Transaction | null>(null)
const viewAttachments = ref<TransactionAttachment[]>([])
const viewAttachmentsLoading = ref(false)
const previewUrl = ref('')
const previewContentType = ref('')
const previewFileName = ref('')

const filters = reactive({
  from: '',
  to: '',
  type: '' as '' | TransactionType,
  accountId: '',
  categoryId: '',
  datePreset: 'ALL' as DatePreset,
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
  attachments.value = []
  selectedAttachment.value = null
  isDraggingAttachment.value = false
}

const selectType = (type: TransactionType) => {
  form.type = type
  const categoryStillValid = filteredCategories.value.some((c) => c.id === form.categoryId)
  if (!categoryStillValid) {
    form.categoryId = ''
  }
}

const openCreateModal = () => {
  resetForm()
  showFormModal.value = true
}

const openEditModal = async (item: Transaction) => {
  editingId.value = item.id
  form.accountId = item.accountId
  form.categoryId = item.categoryId
  form.type = item.type
  form.amount = String(item.amount)
  form.date = item.date
  form.description = item.description ?? ''
  selectedAttachment.value = null
  showFormModal.value = true
  await loadAttachments(item.id)
}

const closeFormModal = () => {
  showFormModal.value = false
  selectedAttachment.value = null
  isDraggingAttachment.value = false
}

const openViewModal = async (item: Transaction) => {
  selectedView.value = item
  showViewModal.value = true
  await loadViewAttachments(item.id)
}

const closeViewModal = () => {
  showViewModal.value = false
  selectedView.value = null
  viewAttachments.value = []
  clearPreview()
}

const formatBytes = (value: number) => {
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

const onAttachmentSelected = (event: Event) => {
  const target = event.target as HTMLInputElement
  selectedAttachment.value = target.files?.[0] ?? null
}

const onAttachmentDrop = (event: DragEvent) => {
  event.preventDefault()
  isDraggingAttachment.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    selectedAttachment.value = file
  }
}

const clearAttachmentSelection = () => {
  selectedAttachment.value = null
  if (attachmentInput.value) {
    attachmentInput.value.value = ''
  }
}

const loadAttachments = async (transactionId: string) => {
  attachmentsLoading.value = true
  try {
    const response = await api.get<TransactionAttachment[]>(`/api/v1/transactions/${transactionId}/attachments`)
    attachments.value = response.data
  } catch {
    toast.error('Não foi possível carregar anexos da transação.')
  } finally {
    attachmentsLoading.value = false
  }
}

const loadViewAttachments = async (transactionId: string) => {
  viewAttachmentsLoading.value = true
  try {
    const response = await api.get<TransactionAttachment[]>(`/api/v1/transactions/${transactionId}/attachments`)
    viewAttachments.value = response.data
  } catch {
    toast.error('Não foi possível carregar anexos da transação.')
  } finally {
    viewAttachmentsLoading.value = false
  }
}

const uploadSelectedAttachment = async (transactionId: string) => {
  if (!selectedAttachment.value) return

  attachmentUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedAttachment.value)
    await api.post(`/api/v1/transactions/${transactionId}/attachments`, formData)
    clearAttachmentSelection()
    await loadAttachments(transactionId)
    toast.success('Anexo enviado com sucesso.')
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao enviar anexo.')
    } else {
      toast.error('Falha ao enviar anexo.')
    }
  } finally {
    attachmentUploading.value = false
  }
}

const downloadAttachment = async (transactionId: string, attachment: TransactionAttachment) => {
  try {
    const response = await api.get(`/api/v1/transactions/${transactionId}/attachments/${attachment.id}/download`, {
      responseType: 'blob',
    })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.fileName
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    toast.error('Não foi possível baixar o anexo.')
  }
}

const clearPreview = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  previewContentType.value = ''
  previewFileName.value = ''
}

const previewAttachment = async (transactionId: string, attachment: TransactionAttachment) => {
  clearPreview()
  try {
    const response = await api.get(`/api/v1/transactions/${transactionId}/attachments/${attachment.id}/preview`, {
      responseType: 'blob',
    })
    previewUrl.value = URL.createObjectURL(response.data)
    previewContentType.value = attachment.contentType || response.data.type || 'application/octet-stream'
    previewFileName.value = attachment.fileName
  } catch {
    toast.error('Não foi possível visualizar o anexo.')
  }
}

const deleteAttachment = async (transactionId: string, attachmentId: string) => {
  try {
    await api.delete(`/api/v1/transactions/${transactionId}/attachments/${attachmentId}`)
    toast.success('Anexo excluído com sucesso.')
    await loadAttachments(transactionId)
    if (selectedView.value?.id === transactionId) {
      await loadViewAttachments(transactionId)
    }
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao excluir anexo.')
    } else {
      toast.error('Falha ao excluir anexo.')
    }
  }
}

const askDeleteFromEdit = () => {
  if (!editingId.value) return
  const item = items.value.find((t) => t.id === editingId.value)
  if (!item) return
  selectedDelete.value = item
  showDeleteModal.value = true
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

const toInputDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const applyDatePreset = () => {
  const today = new Date()

  if (filters.datePreset === 'ALL') {
    filters.from = ''
    filters.to = ''
    return
  }

  if (filters.datePreset === 'THIS_MONTH') {
    const first = new Date(today.getFullYear(), today.getMonth(), 1)
    const last = new Date(today.getFullYear(), today.getMonth() + 1, 0)
    filters.from = toInputDate(first)
    filters.to = toInputDate(last)
    return
  }

  if (filters.datePreset === 'LAST_7_DAYS') {
    const from = new Date(today)
    from.setDate(today.getDate() - 6)
    filters.from = toInputDate(from)
    filters.to = toInputDate(today)
    return
  }

  if (filters.datePreset === 'LAST_30_DAYS') {
    const from = new Date(today)
    from.setDate(today.getDate() - 29)
    filters.from = toInputDate(from)
    filters.to = toInputDate(today)
    return
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

    let transactionId = editingId.value

    if (editingId.value) {
      const response = await api.put<Transaction>(`/api/v1/transactions/${editingId.value}`, payload)
      transactionId = response.data.id
      toast.success('Transação atualizada com sucesso.')
    } else {
      const response = await api.post<Transaction>('/api/v1/transactions', payload)
      transactionId = response.data.id
      toast.success('Transação criada com sucesso.')
    }

    if (transactionId && selectedAttachment.value) {
      await uploadSelectedAttachment(transactionId)
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

const confirmDelete = async () => {
  if (!selectedDelete.value) return

  deleting.value = true

  try {
    await api.delete(`/api/v1/transactions/${selectedDelete.value.id}`)
    toast.success('Transação excluída com sucesso.')
    showDeleteModal.value = false
    showFormModal.value = false
    if (selectedView.value?.id === selectedDelete.value.id) {
      closeViewModal()
    }
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
    applyDatePreset()
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
      <select v-model="filters.accountId">
        <option value="">Conta (todas)</option>
        <option v-for="account in accounts" :key="account.id" :value="account.id">{{ account.name }}</option>
      </select>
      <select v-model="filters.type">
        <option value="">Tipo (todos)</option>
        <option value="EXPENSE">Despesas</option>
        <option value="INCOME">Receitas</option>
      </select>
      <select v-model="filters.categoryId">
        <option value="">Categoria (todas)</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
      </select>
      <select v-model="filters.datePreset" @change="applyDatePreset">
        <option value="ALL">Data (todas)</option>
        <option value="THIS_MONTH">Este mês</option>
        <option value="LAST_7_DAYS">Últimos 7 dias</option>
        <option value="LAST_30_DAYS">Últimos 30 dias</option>
        <option value="CUSTOM">Personalizado</option>
      </select>
      <input v-model="filters.from" type="date" :disabled="filters.datePreset === 'ALL'" />
      <input v-model="filters.to" type="date" :disabled="filters.datePreset === 'ALL'" />
      <button class="btn btn-secondary" @click="loadTransactions(0)">Aplicar</button>
    </div>

    <p v-if="loading" class="muted">Carregando...</p>
    <div v-else-if="!items.length" class="empty-state">
      <img src="/brand/badge.svg" alt="MeuBolso" />
      <p>Nenhuma transação encontrada.</p>
    </div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>Data</th>
          <th>Tipo</th>
          <th>Conta</th>
          <th>Categoria</th>
          <th>Valor</th>
          <th>Descrição</th>
          <th>Docs</th>
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
          <td>
            <span class="docs-badge">{{ item.attachmentsCount ?? 0 }}</span>
          </td>
          <td class="actions">
            <button class="icon-btn icon-view" type="button" title="Visualizar" @click="openViewModal(item)">
              <Eye :size="16" :stroke-width="2.2" />
            </button>
            <button class="icon-btn icon-edit" type="button" title="Editar" @click="openEditModal(item)">
              <Pencil :size="16" :stroke-width="2.2" />
            </button>
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
            <div class="type-switch" role="group" aria-label="Tipo de transação">
              <button
                type="button"
                class="type-option type-expense"
                :class="{ active: form.type === 'EXPENSE' }"
                @click="selectType('EXPENSE')"
              >
                Despesa
              </button>
              <button
                type="button"
                class="type-option type-income"
                :class="{ active: form.type === 'INCOME' }"
                @click="selectType('INCOME')"
              >
                Receita
              </button>
            </div>
          </label>

          <div class="form-row">
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
          </div>

          <div class="form-row">
            <label class="field">
              <span>Valor</span>
              <input v-model="form.amount" type="number" min="0.01" step="0.01" required />
            </label>

            <label class="field">
              <span>Descrição</span>
              <input v-model.trim="form.description" type="text" maxlength="255" />
            </label>
          </div>

          <label class="field">
            <span>Data</span>
            <input v-model="form.date" type="date" required />
          </label>

          <label class="field">
            <span>Anexo</span>
            <div
              class="dropzone"
              :class="{ dragging: isDraggingAttachment }"
              @dragover.prevent="isDraggingAttachment = true"
              @dragleave.prevent="isDraggingAttachment = false"
              @drop="onAttachmentDrop"
            >
              <input
                ref="attachmentInput"
                type="file"
                class="dropzone-input"
                @change="onAttachmentSelected"
              />
              <UploadCloud :size="18" />
              <p>
                Arraste e solte um arquivo aqui ou
                <button type="button" class="link-btn" @click="attachmentInput?.click()">escolha no dispositivo</button>
              </p>
            </div>
            <div v-if="selectedAttachment" class="attachment-selected">
              <small>{{ selectedAttachment.name }} ({{ formatBytes(selectedAttachment.size) }})</small>
              <button type="button" class="link-btn" @click="clearAttachmentSelection">Remover</button>
            </div>
          </label>

          <div v-if="editingId" class="attachments-box">
            <h4>Anexos da transação</h4>
            <p v-if="attachmentsLoading" class="muted">Carregando anexos...</p>
            <p v-else-if="!attachments.length" class="muted">Nenhum anexo nesta transação.</p>
            <ul v-else class="attachments-list">
              <li v-for="attachment in attachments" :key="attachment.id">
                <div>
                  <strong>{{ attachment.fileName }}</strong>
                  <small>{{ formatBytes(attachment.sizeBytes) }}</small>
                </div>
                <div class="attachment-actions">
                  <button type="button" class="link-btn" @click="previewAttachment(editingId!, attachment)">
                    Ver
                  </button>
                  <button type="button" class="link-btn" @click="downloadAttachment(editingId!, attachment)">
                    Baixar
                  </button>
                  <button type="button" class="link-btn danger" @click="deleteAttachment(editingId!, attachment.id)">
                    Excluir
                  </button>
                </div>
              </li>
            </ul>
          </div>

          <div class="modal-actions">
            <button v-if="editingId" type="button" class="btn btn-danger-soft" @click="askDeleteFromEdit">
              Excluir transação
            </button>
            <button type="button" class="btn btn-secondary" @click="closeFormModal">Cancelar</button>
            <button type="submit" class="btn btn-primary" :disabled="saving || attachmentUploading">
              {{ saving || attachmentUploading ? 'Salvando...' : submitLabel }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Teleport>

  <Teleport to="body">
    <div v-if="showViewModal && selectedView" class="modal-backdrop" @click.self="closeViewModal">
      <section class="modal-card view-modal">
        <h3>Detalhes da transação</h3>

        <dl class="details-grid">
          <div>
            <dt>Data</dt>
            <dd>{{ new Date(selectedView.date + 'T00:00:00').toLocaleDateString('pt-BR') }}</dd>
          </div>
          <div>
            <dt>Tipo</dt>
            <dd>{{ transactionTypeLabel[selectedView.type] ?? selectedView.type }}</dd>
          </div>
          <div>
            <dt>Conta</dt>
            <dd>{{ accountNameById(selectedView.accountId) }}</dd>
          </div>
          <div>
            <dt>Categoria</dt>
            <dd>{{ categoryNameById(selectedView.categoryId) }}</dd>
          </div>
          <div>
            <dt>Valor</dt>
            <dd>{{ Number(selectedView.amount).toFixed(2) }}</dd>
          </div>
          <div>
            <dt>Descrição</dt>
            <dd>{{ selectedView.description || '-' }}</dd>
          </div>
        </dl>

        <section class="attachments-box">
          <h4>Anexos</h4>
          <p v-if="viewAttachmentsLoading" class="muted">Carregando anexos...</p>
          <p v-else-if="!viewAttachments.length" class="muted">Nenhum anexo nesta transação.</p>
          <ul v-else class="attachments-list">
            <li v-for="attachment in viewAttachments" :key="attachment.id">
              <div>
                <strong>{{ attachment.fileName }}</strong>
                <small>{{ formatBytes(attachment.sizeBytes) }}</small>
              </div>
              <div class="attachment-actions">
                <button type="button" class="link-btn" @click="previewAttachment(selectedView.id, attachment)">Ver</button>
                <button type="button" class="link-btn" @click="downloadAttachment(selectedView.id, attachment)">
                  <Download :size="14" />
                </button>
              </div>
            </li>
          </ul>
        </section>

        <section v-if="previewUrl" class="preview-box">
          <header class="preview-header">
            <strong>{{ previewFileName }}</strong>
            <button type="button" class="icon-btn icon-close" title="Fechar pré-visualização" @click="clearPreview">
              <X :size="16" :stroke-width="2.2" />
            </button>
          </header>
          <div class="preview-content">
            <img v-if="previewContentType.startsWith('image/')" :src="previewUrl" alt="Pré-visualização do anexo" />
            <iframe v-else-if="previewContentType.startsWith('application/pdf')" :src="previewUrl" />
            <p v-else class="muted">Pré-visualização não disponível para este tipo de arquivo.</p>
          </div>
        </section>

        <div class="modal-actions">
          <button type="button" class="btn btn-secondary" @click="closeViewModal">Fechar</button>
        </div>
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
  grid-template-columns: repeat(7, minmax(0, 1fr));
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
  gap: 8px;
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
  margin-top: 12px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  min-height: 130px;
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

.view-modal {
  max-width: 840px;
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

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.type-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.type-option {
  height: 40px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.type-option.active {
  color: #fff;
}

.type-expense.active {
  background: #991b1b;
  border-color: #991b1b;
}

.type-income.active {
  background: #15803d;
  border-color: #15803d;
}

.modal-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.attachment-selected {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.attachments-box {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px;
  background: #f8fafc;
}

.attachments-box h4 {
  margin: 0;
  font-size: 14px;
}

.attachments-list {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

.attachments-list li {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  padding: 8px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.attachments-list strong {
  display: block;
  font-size: 13px;
}

.attachments-list small {
  color: var(--muted);
  font-size: 12px;
}

.attachment-actions {
  display: inline-flex;
  gap: 10px;
  align-items: center;
}

.link-btn {
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
  padding: 0;
}

.link-btn.danger {
  color: #dc2626;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #0f172a;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

.icon-btn:hover {
  border-color: #c3d0ea;
  background: #f8fbff;
}

.icon-view {
  color: #1d4ed8;
}

.icon-edit {
  color: #0f172a;
}

.icon-close {
  width: 32px;
  height: 32px;
}

.docs-badge {
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #f8fafc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.dropzone {
  position: relative;
  border: 1.5px dashed #c4d0e5;
  background: #f8fbff;
  border-radius: 12px;
  padding: 12px;
  display: grid;
  gap: 6px;
  justify-items: center;
  text-align: center;
  color: #4a638d;
}

.dropzone.dragging {
  border-color: #2563eb;
  background: #eef4ff;
}

.dropzone p {
  margin: 0;
  font-size: 13px;
}

.dropzone-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.details-grid {
  margin: 14px 0 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.details-grid dt {
  font-size: 12px;
  color: var(--muted);
}

.details-grid dd {
  margin: 2px 0 0;
  font-size: 14px;
  color: var(--text);
}

.preview-box {
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
}

.preview-header {
  height: 42px;
  padding: 0 10px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.preview-header strong {
  font-size: 13px;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-content {
  min-height: 280px;
  max-height: 420px;
  display: grid;
  place-items: center;
  padding: 10px;
  background: #f8fafc;
}

.preview-content img {
  max-width: 100%;
  max-height: 390px;
  border-radius: 8px;
}

.preview-content iframe {
  width: 100%;
  min-height: 380px;
  border: 0;
  border-radius: 8px;
  background: #fff;
}

@media (max-width: 1400px) {
  .filters {
    grid-template-columns: repeat(4, minmax(0, 1fr));
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

  .form-row {
    grid-template-columns: 1fr;
  }

  .details-grid {
    grid-template-columns: 1fr;
  }
}
</style>
