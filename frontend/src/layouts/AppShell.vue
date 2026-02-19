<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseConfirmModal from '../components/BaseConfirmModal.vue'
import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const showLogoutModal = ref(false)
const logoutLoading = ref(false)
const showUserMenu = ref(false)
const userEmail = ref('')
const userName = ref('')

const navItems = [
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'Transações', to: '/transactions' },
  { label: 'Faturas', to: '/invoices' },
  { label: 'Contas', to: '/accounts' },
  { label: 'Categorias', to: '/categories' },
]

const pageTitle = computed(() => {
  const current = navItems.find((item) => item.to === route.path)
  if (route.path === '/profile') {
    return 'Perfil'
  }
  return current?.label ?? 'MeuBolso'
})

const leave = async () => {
  logoutLoading.value = true
  try {
    if (auth.refreshToken) {
      await api.post('/api/v1/auth/logout', { refreshToken: auth.refreshToken })
    }
  } finally {
    auth.logout()
    logoutLoading.value = false
    showLogoutModal.value = false
    await router.push('/login')
  }
}

const openProfile = async () => {
  showUserMenu.value = false
  await router.push('/profile')
}

const loadMe = async () => {
  try {
    const response = await api.get<{ email: string; firstName?: string | null; lastName?: string | null }>('/api/v1/auth/me')
    userEmail.value = response.data.email
    const first = (response.data.firstName ?? '').trim()
    const last = (response.data.lastName ?? '').trim()
    userName.value = `${first} ${last}`.trim()
  } catch {
    userEmail.value = ''
    userName.value = ''
  }
}

const handleMeUpdated = () => {
  loadMe()
}

onMounted(() => {
  loadMe()
  window.addEventListener('auth-me-updated', handleMeUpdated)
})

onUnmounted(() => {
  window.removeEventListener('auth-me-updated', handleMeUpdated)
})
</script>

<template>
  <main class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <img src="/brand/logo.svg" alt="MeuBolso" class="brand-logo" />
        <p>Controle financeiro</p>
      </div>

      <nav class="nav nav-section nav-main">
        <span class="nav-title">Início</span>
        <RouterLink
          :to="navItems[0].to"
          class="nav-link nav-link-dashboard"
          :class="{ active: route.path === navItems[0].to }"
        >
          {{ navItems[0].label }}
        </RouterLink>
      </nav>

      <nav class="nav nav-section">
        <span class="nav-title">Movimentação</span>
        <RouterLink
          :to="navItems[1].to"
          class="nav-link"
          :class="{ active: route.path === navItems[1].to }"
        >
          {{ navItems[1].label }}
        </RouterLink>
        <RouterLink
          :to="navItems[2].to"
          class="nav-link"
          :class="{ active: route.path === navItems[2].to }"
        >
          {{ navItems[2].label }}
        </RouterLink>
        <RouterLink
          :to="navItems[3].to"
          class="nav-link"
          :class="{ active: route.path === navItems[3].to }"
        >
          {{ navItems[3].label }}
        </RouterLink>
      </nav>

      <nav class="nav nav-section nav-section-last">
        <span class="nav-title">Organização</span>
        <RouterLink
          :to="navItems[4].to"
          class="nav-link"
          :class="{ active: route.path === navItems[4].to }"
        >
          {{ navItems[4].label }}
        </RouterLink>
      </nav>

      <div class="user-menu-wrapper">
        <div class="profile-divider"></div>
        <span class="profile-title">Perfil</span>
        <button type="button" class="user-trigger" @click="showUserMenu = !showUserMenu">
          <img src="/brand/badge.svg" alt="Avatar MeuBolso" class="avatar" />
          <span class="user-email">{{ userName || userEmail || 'Usuário' }}</span>
          <span class="caret">▾</span>
        </button>

        <div v-if="showUserMenu" class="user-dropdown">
          <button type="button" class="dropdown-item" @click="openProfile">Perfil</button>
          <button type="button" class="dropdown-item danger" @click="showLogoutModal = true">
            Sair
          </button>
        </div>
      </div>
    </aside>

    <section class="content">
      <header class="content-topbar">
        <h2>{{ pageTitle }}</h2>
      </header>
      <RouterView />
    </section>
  </main>

  <BaseConfirmModal
    :open="showLogoutModal"
    title="Sair da conta"
    message="Tem certeza que deseja encerrar sua sessão?"
    confirm-text="Sair"
    :loading="logoutLoading"
    @cancel="showLogoutModal = false"
    @confirm="leave"
  />
</template>

<style scoped>
.app-shell {
  height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
  overflow: hidden;
}

.sidebar {
  height: 100vh;
  background: var(--surface);
  border-right: 1px solid var(--border);
  padding: 24px 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}

.brand p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 14px;
}

.brand-logo {
  display: block;
  width: 100%;
  max-width: 228px;
  height: auto;
}

.brand {
  padding: 0 4px 12px;
  border-bottom: 1px solid #d7dee9;
}

.nav {
  display: grid;
  gap: 6px;
}

.nav-section {
  padding: 0 4px 12px;
  border-bottom: 1px solid #e2e8f0;
}

.nav-main {
  margin-top: 2px;
}

.nav-section-last {
  border-bottom: 0;
  padding-bottom: 4px;
}

.nav-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 6px;
}

.nav-link {
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  color: var(--text);
  text-decoration: none;
  font-weight: 600;
}

.nav-link:hover {
  background: #f1f5f9;
}

.nav-link.active {
  background: #e8efff;
  color: #1d4ed8;
  border-left: 3px solid #2563eb;
}

.nav-link-dashboard {
  background: #f8fbff;
}

.nav-link-dashboard.active {
  background: #dbeafe;
}

.user-menu-wrapper {
  margin-top: auto;
  position: relative;
  padding: 10px 4px 0;
}

.profile-divider {
  height: 1px;
  background: #d7dee9;
  margin-bottom: 10px;
}

.profile-title {
  display: block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 8px;
}

.user-trigger {
  width: 100%;
  border: 1px solid var(--border);
  background: #f8fafc;
  border-radius: 12px;
  height: 46px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  cursor: pointer;
}

.user-trigger:hover {
  background: #eff6ff;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  flex: 0 0 auto;
}

.user-email {
  font-size: 13px;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  margin-left: auto;
  color: var(--muted);
}

.user-dropdown {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 52px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.1);
  overflow: hidden;
}

.dropdown-item {
  width: 100%;
  border: 0;
  background: #fff;
  text-align: left;
  height: 40px;
  padding: 0 12px;
  cursor: pointer;
  color: var(--text);
}

.dropdown-item:hover {
  background: #f8fafc;
}

.dropdown-item.danger {
  color: var(--danger);
}

.content {
  padding: 24px 28px;
  overflow-y: auto;
  min-height: 0;
}

.content-topbar h2 {
  margin: 0;
}

@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    border-right: 0;
    border-bottom: 1px solid var(--border);
    gap: 16px;
  }

  .nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .logout-btn {
    margin-top: 0;
  }
}
</style>
