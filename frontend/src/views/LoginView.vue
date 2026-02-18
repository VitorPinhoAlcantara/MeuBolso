<script setup lang="ts">
import axios from 'axios'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  email: '',
  password: '',
})

const loading = ref(false)
const errorMessage = ref('')

const submit = async () => {
  errorMessage.value = ''
  loading.value = true

  try {
    await auth.login(form)
    await router.push('/dashboard')
  } catch (error: unknown) {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401) {
        errorMessage.value = 'Email ou senha inválidos'
      } else if (!error.response) {
        errorMessage.value = 'Não foi possível conectar na API'
      } else {
        errorMessage.value = error.response.data?.error ?? 'Falha ao realizar login'
      }
    } else {
      errorMessage.value = 'Falha ao realizar login'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <header class="login-header">
        <h1>Entrar no MeuBolso</h1>
        <p>Use sua conta para acessar o painel financeiro.</p>
      </header>

      <form class="login-form" @submit.prevent="submit">
        <label class="field">
          <span>Email</span>
          <input
            v-model.trim="form.email"
            type="email"
            placeholder="voce@exemplo.com"
            required
            autocomplete="email"
          />
        </label>

        <label class="field">
          <span>Senha</span>
          <input
            v-model="form.password"
            type="password"
            placeholder="******"
            required
            autocomplete="current-password"
          />
        </label>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

        <button class="submit-btn" type="submit" :disabled="loading">
          {{ loading ? 'Entrando...' : 'Entrar' }}
        </button>
      </form>

      <footer class="login-footer">
        <span>Ainda não tem conta?</span>
        <RouterLink to="/register">Criar conta</RouterLink>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 28px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
}

.login-header h1 {
  margin: 0;
  font-size: 24px;
}

.login-header p {
  margin: 8px 0 0;
  color: var(--muted);
}

.login-form {
  margin-top: 24px;
  display: grid;
  gap: 14px;
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-size: 14px;
  color: var(--text);
}

.field input {
  border: 1px solid var(--border);
  border-radius: 10px;
  height: 42px;
  padding: 0 12px;
  font-size: 14px;
  color: var(--text);
  outline: none;
}

.field input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.error {
  margin: 0;
  color: var(--danger);
  font-size: 14px;
}

.submit-btn {
  height: 44px;
  border: 0;
  border-radius: 10px;
  background: var(--primary);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.submit-btn:hover {
  background: var(--primary-600);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-footer {
  margin-top: 18px;
  display: flex;
  gap: 8px;
  font-size: 14px;
  color: var(--muted);
}

.login-footer a {
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
}

.login-footer a:hover {
  text-decoration: underline;
}
</style>
