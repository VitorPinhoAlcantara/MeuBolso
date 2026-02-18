<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { CompactPicker } from 'vue-color'
import BaseConfirmModal from '../components/BaseConfirmModal.vue'
import RowActionButtons from '../components/RowActionButtons.vue'
import { useToast } from '../composables/useToast'
import { api } from '../services/api'

type CategoryType = 'INCOME' | 'EXPENSE'

type Category = {
  id: string
  name: string
  type: CategoryType
  color: string
}

type PageResponse<T> = {
  content: T[]
  number: number
  totalPages: number
}

const typeOptions: Array<{ label: string; value: CategoryType }> = [
  { label: 'Despesa', value: 'EXPENSE' },
  { label: 'Receita', value: 'INCOME' },
]

const categoryPalette = [
  '#EF4444', '#F97316', '#F59E0B', '#EAB308', '#84CC16', '#22C55E',
  '#10B981', '#14B8A6', '#06B6D4', '#0EA5E9', '#3B82F6', '#2563EB',
  '#4F46E5', '#6366F1', '#8B5CF6', '#A855F7', '#D946EF', '#EC4899',
  '#F43F5E', '#FB7185', '#F87171', '#FB923C', '#2DD4BF', '#38BDF8',
]

const loading = ref(false)
const saving = ref(false)
const toast = useToast()

const incomeItems = ref<Category[]>([])
const expenseItems = ref<Category[]>([])

const showFormModal = ref(false)
const showDeleteModal = ref(false)
const deleting = ref(false)
const selectedDelete = ref<Category | null>(null)

const editingId = ref<string | null>(null)
const form = reactive({
  name: '',
  type: 'EXPENSE' as CategoryType,
  color: '#2563EB',
})
const colorHexDraft = ref('#2563EB')

const submitLabel = computed(() => (editingId.value ? 'Salvar alterações' : 'Criar categoria'))
const modalTitle = computed(() => (editingId.value ? 'Editar categoria' : 'Nova categoria'))

const normalizeHex = (value: string) => {
  const normalized = value.trim().toUpperCase()
  return /^#([A-F0-9]{6})$/.test(normalized) ? normalized : '#2563EB'
}

const fetchByType = async (type: CategoryType) => {
  const response = await api.get<PageResponse<Category>>('/api/v1/categories', {
    params: {
      type,
      page: 0,
      size: 200,
      sort: 'name,asc',
    },
  })

  return response.data.content
}

const loadCategories = async () => {
  loading.value = true

  try {
    const [incomes, expenses] = await Promise.all([
      fetchByType('INCOME'),
      fetchByType('EXPENSE'),
    ])

    incomeItems.value = incomes
    expenseItems.value = expenses
  } catch {
    toast.error('Não foi possível carregar as categorias.')
  } finally {
    loading.value = false
  }
}

const openCreateModal = () => {
  editingId.value = null
  form.name = ''
  form.type = 'EXPENSE'
  form.color = '#2563EB'
  colorHexDraft.value = '#2563EB'
  showFormModal.value = true
}

const openEditModal = (item: Category) => {
  editingId.value = item.id
  form.name = item.name
  form.type = item.type
  form.color = item.color || '#2563EB'
  colorHexDraft.value = normalizeHex(form.color)
  showFormModal.value = true
}

const closeFormModal = () => {
  showFormModal.value = false
}

const onHexBlur = () => {
  form.color = normalizeHex(colorHexDraft.value)
  colorHexDraft.value = form.color
}

const submit = async () => {
  saving.value = true

  try {
    const normalizedColor = normalizeHex(form.color)
    const payload = {
      name: form.name,
      type: form.type,
      color: normalizedColor,
    }

    if (editingId.value) {
      await api.put(`/api/v1/categories/${editingId.value}`, payload)
      toast.success('Categoria atualizada com sucesso.')
    } else {
      await api.post('/api/v1/categories', payload)
      toast.success('Categoria criada com sucesso.')
    }

    showFormModal.value = false
    await loadCategories()
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao salvar categoria.')
    } else {
      toast.error('Falha ao salvar categoria.')
    }
  } finally {
    saving.value = false
  }
}

const askDelete = (item: Category) => {
  selectedDelete.value = item
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  if (!selectedDelete.value) return

  deleting.value = true

  try {
    await api.delete(`/api/v1/categories/${selectedDelete.value.id}`)
    toast.success('Categoria excluída com sucesso.')
    showDeleteModal.value = false
    selectedDelete.value = null
    await loadCategories()
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao excluir categoria.')
    } else {
      toast.error('Falha ao excluir categoria.')
    }
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  loadCategories()
})

watch(
  () => form.color,
  (newValue) => {
    colorHexDraft.value = normalizeHex(newValue)
  },
)
</script>

<template>
  <section class="panel list-panel">
    <header class="list-header">
      <h3>Categorias cadastradas</h3>
      <button class="btn btn-primary" type="button" @click="openCreateModal">Nova categoria</button>
    </header>

    <p v-if="loading" class="muted">Carregando...</p>

    <div v-else class="split-grid">
      <article class="type-column">
        <h4>Despesas</h4>
        <p v-if="!expenseItems.length" class="muted">Nenhuma categoria de despesa.</p>
        <table v-else class="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Cor</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in expenseItems" :key="item.id">
              <td>{{ item.name }}</td>
              <td>
                <span class="color-dot" :style="{ backgroundColor: item.color || '#94a3b8' }" />
                {{ item.color || 'N/A' }}
              </td>
              <td class="actions">
                <RowActionButtons
                  edit-title="Editar categoria"
                  delete-title="Excluir categoria"
                  @edit="openEditModal(item)"
                  @delete="askDelete(item)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="type-column">
        <h4>Receitas</h4>
        <p v-if="!incomeItems.length" class="muted">Nenhuma categoria de receita.</p>
        <table v-else class="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Cor</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in incomeItems" :key="item.id">
              <td>{{ item.name }}</td>
              <td>
                <span class="color-dot" :style="{ backgroundColor: item.color || '#94a3b8' }" />
                {{ item.color || 'N/A' }}
              </td>
              <td class="actions">
                <RowActionButtons
                  edit-title="Editar categoria"
                  delete-title="Excluir categoria"
                  @edit="openEditModal(item)"
                  @delete="askDelete(item)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </article>
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
            <span>Cor</span>
            <div class="color-section">
              <div class="color-picker-wrap">
                <CompactPicker v-model="form.color" :palette="categoryPalette" />
              </div>
              <div class="hex-row">
                <span class="selected-color" :style="{ backgroundColor: form.color }" />
                <input
                  v-model.trim="colorHexDraft"
                  type="text"
                  maxlength="7"
                  placeholder="#2563EB"
                  @blur="onHexBlur"
                />
              </div>
            </div>
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
    title="Excluir categoria"
    :message="`Deseja excluir a categoria ${selectedDelete?.name ?? ''}?`"
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

.split-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.type-column {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
}

.type-column h4 {
  margin: 0;
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

.color-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  margin-right: 8px;
}

.actions {
  display: flex;
  justify-content: flex-end;
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
  max-width: 520px;
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

.color-section {
  display: grid;
  gap: 8px;
}

.color-picker-wrap :deep(.vc-compact-picker) {
  width: 100% !important;
  padding-top: 8px;
  padding-left: 8px;
}

.color-picker-wrap :deep(.color-item) {
  width: 22px;
  height: 22px;
  margin-right: 6px;
  margin-bottom: 6px;
}

.color-picker-wrap :deep(.dot) {
  inset: 7px;
}

.hex-row {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 8px;
  align-items: center;
}

.selected-color {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid var(--border);
}

.hex-row input {
  height: 36px;
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

@media (max-width: 1200px) {
  .split-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 920px) {
  .table {
    display: block;
    overflow-x: auto;
    white-space: nowrap;
  }
}
</style>
