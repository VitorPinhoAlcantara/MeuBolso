<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../services/api'

type MeResponse = {
  email: string
  createdAt: string
}

const loading = ref(true)
const error = ref('')
const profile = ref<MeResponse | null>(null)

const loadProfile = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get<MeResponse>('/api/v1/auth/me')
    profile.value = response.data
  } catch {
    error.value = 'Não foi possível carregar os dados do perfil'
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <section class="panel">
    <h3>Perfil</h3>
    <p v-if="loading" class="muted">Carregando...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <dl v-else-if="profile" class="profile-data">
      <div>
        <dt>Email</dt>
        <dd>{{ profile.email }}</dd>
      </div>
      <div>
        <dt>Criado em</dt>
        <dd>{{ new Date(profile.createdAt).toLocaleString('pt-BR') }}</dd>
      </div>
    </dl>
  </section>
</template>

<style scoped>
.panel {
  margin-top: 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 18px;
}

.panel h3 {
  margin: 0;
}

.muted {
  color: var(--muted);
}

.error {
  color: var(--danger);
}

.profile-data {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.profile-data div {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
}

.profile-data dt {
  color: var(--muted);
  font-size: 12px;
}

.profile-data dd {
  margin: 2px 0 0;
}
</style>
