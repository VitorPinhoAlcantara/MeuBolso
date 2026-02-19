<script setup lang="ts">
import axios from 'axios'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseConfirmModal from '../components/BaseConfirmModal.vue'
import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'

type MeResponse = {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  createdAt: string
  updatedAt: string
}

const loading = ref(true)
const savingProfile = ref(false)
const savingEmail = ref(false)
const savingPassword = ref(false)
const deletingAccount = ref(false)
const error = ref('')
const profile = ref<MeResponse | null>(null)
const showDeleteModal = ref(false)

const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

const profileForm = reactive({
  firstName: '',
  lastName: '',
})

const emailForm = reactive({
  newEmail: '',
  currentPassword: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const deleteForm = reactive({
  currentPassword: '',
})

const dispatchMeUpdated = () => {
  window.dispatchEvent(new Event('auth-me-updated'))
}

const applyProfileToForm = (data: MeResponse) => {
  profileForm.firstName = data.firstName ?? ''
  profileForm.lastName = data.lastName ?? ''
  emailForm.newEmail = data.email
}

const loadProfile = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get<MeResponse>('/api/v1/auth/me')
    profile.value = response.data
    applyProfileToForm(response.data)
  } catch {
    error.value = 'Não foi possível carregar os dados do perfil'
  } finally {
    loading.value = false
  }
}

const saveProfile = async () => {
  savingProfile.value = true
  try {
    const response = await api.put<MeResponse>('/api/v1/auth/me/profile', {
      firstName: profileForm.firstName || null,
      lastName: profileForm.lastName || null,
    })
    profile.value = response.data
    applyProfileToForm(response.data)
    dispatchMeUpdated()
    toast.success('Perfil atualizado com sucesso.')
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao atualizar perfil.')
    } else {
      toast.error('Falha ao atualizar perfil.')
    }
  } finally {
    savingProfile.value = false
  }
}

const changeEmail = async () => {
  savingEmail.value = true
  try {
    const response = await api.put<MeResponse>('/api/v1/auth/me/email', {
      newEmail: emailForm.newEmail,
      currentPassword: emailForm.currentPassword,
    })

    profile.value = response.data
    applyProfileToForm(response.data)
    emailForm.currentPassword = ''
    dispatchMeUpdated()
    toast.success('Email atualizado com sucesso.')
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao atualizar email.')
    } else {
      toast.error('Falha ao atualizar email.')
    }
  } finally {
    savingEmail.value = false
  }
}

const changePassword = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toast.error('Confirmação de senha não confere.')
    return
  }

  savingPassword.value = true
  try {
    await api.put('/api/v1/auth/me/password', {
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })

    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    toast.success('Senha alterada com sucesso.')
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao alterar senha.')
    } else {
      toast.error('Falha ao alterar senha.')
    }
  } finally {
    savingPassword.value = false
  }
}

const deleteAccount = async () => {
  deletingAccount.value = true
  try {
    await api.delete('/api/v1/auth/me', {
      data: { currentPassword: deleteForm.currentPassword },
    })

    auth.logout()
    showDeleteModal.value = false
    toast.success('Conta removida com sucesso.')
    await router.push('/login')
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      toast.error(err.response?.data?.error ?? 'Falha ao excluir conta.')
    } else {
      toast.error('Falha ao excluir conta.')
    }
  } finally {
    deletingAccount.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <section class="profile-page">
    <p v-if="loading" class="muted">Carregando...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <template v-else-if="profile">
      <section class="panel">
        <h3>Perfil</h3>

        <form class="form" @submit.prevent="saveProfile">
          <div class="form-row">
            <label class="field">
              <span>Nome</span>
              <input v-model.trim="profileForm.firstName" type="text" maxlength="100" placeholder="Seu nome" />
            </label>
            <label class="field">
              <span>Sobrenome</span>
              <input v-model.trim="profileForm.lastName" type="text" maxlength="100" placeholder="Seu sobrenome" />
            </label>
          </div>

          <div class="profile-meta">
            <div>
              <small>Email atual</small>
              <strong>{{ profile.email }}</strong>
            </div>
            <div>
              <small>Criado em</small>
              <strong>{{ new Date(profile.createdAt).toLocaleString('pt-BR') }}</strong>
            </div>
          </div>

          <div class="actions">
            <button class="btn btn-primary" type="submit" :disabled="savingProfile">
              {{ savingProfile ? 'Salvando...' : 'Salvar perfil' }}
            </button>
          </div>
        </form>
      </section>

      <section class="panel">
        <h3>Segurança</h3>

        <form class="form" @submit.prevent="changeEmail">
          <h4>Trocar email</h4>
          <label class="field">
            <span>Novo email</span>
            <input v-model.trim="emailForm.newEmail" type="email" maxlength="255" required />
          </label>
          <label class="field">
            <span>Senha atual</span>
            <input v-model="emailForm.currentPassword" type="password" minlength="6" maxlength="255" required />
          </label>
          <div class="actions">
            <button class="btn btn-secondary" type="submit" :disabled="savingEmail">
              {{ savingEmail ? 'Atualizando...' : 'Atualizar email' }}
            </button>
          </div>
        </form>

        <form class="form" @submit.prevent="changePassword">
          <h4>Trocar senha</h4>
          <label class="field">
            <span>Senha atual</span>
            <input v-model="passwordForm.currentPassword" type="password" minlength="6" maxlength="255" required />
          </label>
          <div class="form-row">
            <label class="field">
              <span>Nova senha</span>
              <input v-model="passwordForm.newPassword" type="password" minlength="6" maxlength="255" required />
            </label>
            <label class="field">
              <span>Confirmar nova senha</span>
              <input v-model="passwordForm.confirmPassword" type="password" minlength="6" maxlength="255" required />
            </label>
          </div>
          <div class="actions">
            <button class="btn btn-secondary" type="submit" :disabled="savingPassword">
              {{ savingPassword ? 'Atualizando...' : 'Atualizar senha' }}
            </button>
          </div>
        </form>

        <form class="form danger-zone" @submit.prevent="showDeleteModal = true">
          <h4>Excluir conta</h4>
          <p>Esta ação remove sua conta e todos os dados permanentemente.</p>
          <label class="field">
            <span>Senha atual</span>
            <input v-model="deleteForm.currentPassword" type="password" minlength="6" maxlength="255" required />
          </label>
          <div class="actions">
            <button class="btn btn-danger-soft" type="submit">Excluir conta</button>
          </div>
        </form>
      </section>
    </template>
  </section>

  <BaseConfirmModal
    :open="showDeleteModal"
    title="Excluir conta"
    message="Tem certeza? Essa ação é permanente e apaga todos os seus dados."
    confirm-text="Excluir conta"
    :loading="deletingAccount"
    @cancel="showDeleteModal = false"
    @confirm="deleteAccount"
  />
</template>

<style scoped>
.profile-page {
  margin-top: 20px;
  display: grid;
  gap: 16px;
}

.panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 18px;
}

.panel h3 {
  margin: 0;
}

.panel h4 {
  margin: 0;
  font-size: 16px;
}

.form {
  margin-top: 14px;
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
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
  height: 40px;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 0 10px;
  outline: none;
}

.field input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.profile-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.profile-meta div {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  display: grid;
  gap: 2px;
}

.profile-meta small {
  color: var(--muted);
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.danger-zone {
  border-color: #fecaca;
  background: #fff7f7;
}

.danger-zone p {
  margin: 0;
  color: #9f1239;
}

.muted {
  color: var(--muted);
}

.error {
  color: var(--danger);
}

@media (max-width: 900px) {
  .form-row,
  .profile-meta {
    grid-template-columns: 1fr;
  }
}
</style>
