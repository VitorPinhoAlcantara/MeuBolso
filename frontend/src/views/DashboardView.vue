<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const leave = async () => {
  auth.logout()
  await router.push('/login')
}
</script>

<template>
  <main class="dashboard-page">
    <section class="dashboard-panel">
      <h1>Dashboard</h1>
      <p>Login realizado com sucesso. Esta tela e um placeholder inicial.</p>
      <button type="button" @click="leave">Sair</button>
    </section>
  </main>
</template>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.dashboard-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 24px;
  width: 100%;
  max-width: 640px;
}

.dashboard-panel h1 {
  margin: 0;
}

.dashboard-panel p {
  margin: 10px 0 20px;
  color: var(--muted);
}

.dashboard-panel button {
  height: 40px;
  border: 0;
  padding: 0 18px;
  border-radius: 10px;
  background: var(--primary);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.dashboard-panel button:hover {
  background: var(--primary-600);
}
</style>
