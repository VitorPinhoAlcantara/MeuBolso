<script setup lang="ts">
defineProps<{
  open: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  cancel: []
  confirm: []
}>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="emit('cancel')">
      <section class="modal-card">
        <h3>{{ title }}</h3>
        <p>{{ message }}</p>

        <div class="modal-actions">
          <button type="button" class="btn btn-secondary" :disabled="loading" @click="emit('cancel')">
            {{ cancelText ?? 'Cancelar' }}
          </button>
          <button type="button" class="btn btn-primary" :disabled="loading" @click="emit('confirm')">
            {{ loading ? 'Processando...' : confirmText ?? 'Confirmar' }}
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
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

.modal-card p {
  margin: 8px 0 0;
  color: var(--muted);
}

.modal-actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
