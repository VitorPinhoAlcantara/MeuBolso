<script setup lang="ts">
import axios from 'axios'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  email: '',
  password: '',
  confirmPassword: '',
})

const loading = ref(false)
const errorMessage = ref('')

const passwordsMatch = computed(() => form.password === form.confirmPassword)

const submit = async () => {
  errorMessage.value = ''

  if (!passwordsMatch.value) {
    errorMessage.value = 'As senhas não conferem'
    return
  }

  loading.value = true

  try {
    await auth.register({ email: form.email, password: form.password })
    await router.push('/dashboard')
  } catch (error: unknown) {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 409) {
        errorMessage.value = 'Este email já está cadastrado'
      } else if (!error.response) {
        errorMessage.value = 'Não foi possível conectar na API'
      } else {
        errorMessage.value = error.response.data?.error ?? 'Falha ao criar conta'
      }
    } else {
      errorMessage.value = 'Falha ao criar conta'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="register-page">
    <section class="register-card">
      <header class="register-header">
        <h1>Criar conta</h1>
        <p>Comece seu controle financeiro em poucos segundos.</p>
      </header>

      <form class="register-form" @submit.prevent="submit">
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
            placeholder="mínimo 6 caracteres"
            required
            minlength="6"
            autocomplete="new-password"
          />
        </label>

        <label class="field">
          <span>Confirmar senha</span>
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="repita a senha"
            required
            minlength="6"
            autocomplete="new-password"
          />
        </label>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

        <button class="submit-btn" type="submit" :disabled="loading">
          {{ loading ? 'Criando conta...' : 'Criar conta' }}
        </button>
      </form>

      <footer class="register-footer">
        <span>Já tem conta?</span>
        <RouterLink to="/login">Entrar</RouterLink>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.register-card {
  width: 100%;
  max-width: 440px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 28px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
}

.register-header h1 {
  margin: 0;
  font-size: 24px;
}

.register-header p {
  margin: 8px 0 0;
  color: var(--muted);
}

.register-form {
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

.register-footer {
  margin-top: 18px;
  display: flex;
  gap: 8px;
  font-size: 14px;
  color: var(--muted);
}

.register-footer a {
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
}

.register-footer a:hover {
  text-decoration: underline;
}
</style>
