<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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

const navItems = [
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'Contas', to: '/accounts' },
  { label: 'Categorias', to: '/categories' },
  { label: 'Transações', to: '/transactions' },
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
    const response = await api.get<{ email: string }>('/api/v1/auth/me')
    userEmail.value = response.data.email
  } catch {
    userEmail.value = ''
  }
}

onMounted(loadMe)
</script>

<template>
  <main class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <h1>MeuBolso</h1>
        <p>Controle financeiro</p>
      </div>

      <nav class="nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ active: route.path === item.to }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="user-menu-wrapper">
        <button type="button" class="user-trigger" @click="showUserMenu = !showUserMenu">
          <span class="avatar">{{ (userEmail || 'U').charAt(0).toUpperCase() }}</span>
          <span class="user-email">{{ userEmail || 'Usuário' }}</span>
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
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
}

.sidebar {
  background: var(--surface);
  border-right: 1px solid var(--border);
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.brand h1 {
  margin: 0;
  font-size: 24px;
}

.brand p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 14px;
}

.nav {
  display: grid;
  gap: 8px;
}

.nav-link {
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  color: var(--text);
  text-decoration: none;
  font-weight: 500;
}

.nav-link:hover {
  background: #eef2ff;
}

.nav-link.active {
  background: var(--primary);
  color: #fff;
}

.user-menu-wrapper {
  margin-top: auto;
  position: relative;
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
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: var(--primary);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
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
