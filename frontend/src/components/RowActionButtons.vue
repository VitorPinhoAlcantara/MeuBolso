<script setup lang="ts">
import { Pencil, Trash2 } from 'lucide-vue-next'

withDefaults(
  defineProps<{
    editTitle?: string
    deleteTitle?: string
  }>(),
  {
    editTitle: 'Editar',
    deleteTitle: 'Excluir',
  },
)

const emit = defineEmits<{
  edit: []
  delete: []
}>()
</script>

<template>
  <div class="actions">
    <button
      class="icon-btn icon-edit"
      type="button"
      :title="editTitle"
      :aria-label="editTitle"
      @click="emit('edit')"
    >
      <Pencil :size="16" :stroke-width="2.2" />
    </button>
    <button
      class="icon-btn icon-delete"
      type="button"
      :title="deleteTitle"
      :aria-label="deleteTitle"
      @click="emit('delete')"
    >
      <Trash2 :size="16" :stroke-width="2.2" />
    </button>
  </div>
</template>

<style scoped>
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

.icon-edit:hover {
  background: #e2e8f0;
}

.icon-delete {
  background: #fee2e2;
  border-color: #fecaca;
  color: var(--danger);
}

.icon-delete:hover {
  background: #fecaca;
}
</style>
