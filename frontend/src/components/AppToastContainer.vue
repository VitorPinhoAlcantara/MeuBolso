<script setup lang="ts">
import { useToast } from '../composables/useToast'

const toast = useToast()
</script>

<template>
  <div class="toast-stack" aria-live="polite" aria-atomic="true">
    <div
      v-for="item in toast.toasts"
      :key="item.id"
      class="toast-item"
      :class="`toast-${item.type}`"
      role="status"
    >
      <span>{{ item.message }}</span>
      <button type="button" class="toast-close" @click="toast.remove(item.id)">×</button>
    </div>
  </div>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  right: 16px;
  bottom: 16px;
  display: grid;
  gap: 10px;
  z-index: 80;
  pointer-events: none;
}

.toast-item {
  min-width: 280px;
  max-width: 380px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--surface);
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.12);
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  pointer-events: auto;
}

.toast-success {
  border-left: 4px solid #16a34a;
}

.toast-error {
  border-left: 4px solid var(--danger);
}

.toast-info {
  border-left: 4px solid var(--primary);
}

.toast-close {
  border: 0;
  background: transparent;
  color: var(--muted);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}
</style>
