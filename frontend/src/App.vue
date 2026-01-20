<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo" @click="$router.push('/dashboard')">
        <img src="/logo.png" alt="燕泰智管" class="logo-image" />
        <span class="logo-text">燕泰智管</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="sidebar-menu"
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Box /></el-icon>
          <span>产品管理</span>
        </el-menu-item>
        <el-menu-item index="/purchases">
          <el-icon><ShoppingCart /></el-icon>
          <span>采购管理</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><DocumentCopy /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/customers">
          <el-icon><User /></el-icon>
          <span>客户管理</span>
        </el-menu-item>
        <el-menu-item index="/suppliers">
          <el-icon><OfficeBuilding /></el-icon>
          <span>供应商管理</span>
        </el-menu-item>
        <el-menu-item index="/inventory-report">
          <el-icon><Document /></el-icon>
          <span>库存报表</span>
        </el-menu-item>
        <el-menu-item index="/inventory-alerts">
          <el-icon><Warning /></el-icon>
          <span>库存预警</span>
        </el-menu-item>
        <el-menu-item index="/warehouses">
          <el-icon><OfficeBuilding /></el-icon>
          <span>仓库管理</span>
        </el-menu-item>
        <el-menu-item index="/financial-reports">
          <el-icon><Document /></el-icon>
          <span>财务报表</span>
        </el-menu-item>
        <el-menu-item index="/tax-management">
          <el-icon><Tickets /></el-icon>
          <span>税务管理</span>
        </el-menu-item>
        <el-menu-item index="/cost-analysis">
          <el-icon><TrendCharts /></el-icon>
          <span>成本分析</span>
        </el-menu-item>
        <el-menu-item index="/reconciliation">
          <el-icon><Finished /></el-icon>
          <span>对账管理</span>
        </el-menu-item>
        <el-menu-item index="/data-analytics">
          <el-icon><PieChart /></el-icon>
          <span>数据分析</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="header-title">{{ pageTitle }}</div>
          <div class="header-subtitle">燕泰智管APP - 专业库存管理解决方案</div>
        </div>
        <div class="header-right">
          <el-badge :value="3" class="notification-badge">
            <el-icon><Bell /></el-icon>
          </el-badge>
          <el-icon><Setting /></el-icon>
          
          <!-- 用户信息下拉菜单 -->
          <el-dropdown v-if="currentUser" trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :src="currentUser.avatarUrl" :size="36">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username">{{ currentUser.nickname }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div class="user-detail">
                    <div class="user-name">{{ currentUser.nickname }}</div>
                    <div class="user-openid">OpenID: {{ currentUser.openid }}</div>
                    <div class="user-role">{{ currentUser.role === 'ADMIN' ? '管理员' : '普通用户' }}</div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import authApi from './api/auth'

const route = useRoute()
const router = useRouter()
const currentUser = ref(null)

const pageTitle = computed(() => {
  const titles = {
    '/dashboard': '仪表盘',
    '/products': '产品管理',
    '/purchases': '采购管理',
    '/orders': '订单管理',
    '/customers': '客户管理',
    '/suppliers': '供应商管理',
    '/inventory-report': '库存报表',
    '/inventory-alerts': '库存预警',
    '/warehouses': '仓库管理',
    '/financial-reports': '财务报表',
    '/tax-management': '税务管理',
    '/cost-analysis': '成本分析',
    '/reconciliation': '对账管理',
    '/data-analytics': '数据分析'
  }
  return titles[route.path] || '超级库存管理系统'
})

// 加载用户信息
const loadUserInfo = async () => {
  // 检查是否有Token
  const token = authApi.getToken()
  if (!token) {
    // 没有Token，清除用户信息
    currentUser.value = null
    authApi.removeUser()
    return
  }

  try {
    // 验证Token是否有效
    const response = await authApi.getUserInfo()
    if (response.data && response.data.user) {
      // Token有效，更新用户信息
      currentUser.value = response.data.user
      authApi.setUser(response.data.user)
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // Token无效，清除所有信息并跳转到登录页
    currentUser.value = null
    authApi.removeToken()
    authApi.removeUser()
    
    if (route.path !== '/login') {
      ElMessage.warning('登录已过期，请重新登录')
      router.push('/login')
    }
  }
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  }
}

// 处理退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用退出API
    await authApi.logout()
    
    // 清除本地存储
    authApi.removeToken()
    authApi.removeUser()
    
    ElMessage.success('已退出登录')
    
    // 跳转到登录页
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出登录失败:', error)
    }
  }
}

// 监听路由变化，每次切换页面都重新加载用户信息
watch(() => route.path, (newPath) => {
  if (newPath !== '/login') {
    loadUserInfo()
  }
})

onMounted(() => {
  // 初次加载时，如果不是登录页，加载用户信息
  if (route.path !== '/login') {
    loadUserInfo()
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: row;
}

/* iOS-style Sidebar */
.sidebar {
  background: linear-gradient(180deg, #f7f7f7 0%, #ffffff 100%);
  border-right: 0.5px solid rgba(0, 0, 0, 0.08);
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  backdrop-filter: blur(20px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.logo {
  height: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.8);
}

.logo:hover {
  background: rgba(0, 122, 255, 0.05);
  transform: scale(1.02);
}

.logo:active {
  transform: scale(0.98);
}

.logo-image {
  width: 50px;
  height: 50px;
  object-fit: contain;
  margin-bottom: 6px;
  animation: logoFloat 3s ease-in-out infinite;
  filter: drop-shadow(0 2px 12px rgba(0, 122, 255, 0.25));
}

.logo-text {
  color: #1d1d1f;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', sans-serif;
}

@keyframes logoFloat {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-4px);
  }
}

/* iOS-style Menu */
.sidebar-menu {
  border-right: none;
  background: transparent !important;
  padding: 8px;
}

.sidebar-menu :deep(.el-menu-item) {
  margin: 2px 0;
  border-radius: 10px;
  height: 44px;
  line-height: 44px;
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  background: transparent;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', sans-serif;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(0, 122, 255, 0.08) !important;
  color: #007aff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #007aff 0%, #0051d5 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  margin-right: 10px;
  font-size: 18px;
}

/* iOS-style Header */
.header {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.03);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.header-title {
  font-size: 22px;
  font-weight: 700;
  color: #1d1d1f;
  letter-spacing: -0.5px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.header-subtitle {
  font-size: 11px;
  color: #86868b;
  font-weight: 400;
  letter-spacing: 0.3px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', sans-serif;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right .el-icon {
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 10px;
  border-radius: 50%;
  color: #1d1d1f;
  font-size: 20px;
  background: rgba(0, 0, 0, 0.04);
}

.header-right .el-icon:hover {
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
  transform: scale(1.1);
}

.header-right .el-icon:active {
  transform: scale(0.95);
}

.notification-badge {
  cursor: pointer;
}

.notification-badge :deep(.el-badge__content) {
  background: #ff3b30;
  border: 2px solid rgba(255, 255, 255, 0.8);
  font-weight: 600;
}

/* User Info Dropdown */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 20px;
  background: rgba(0, 122, 255, 0.08);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.user-info:hover {
  background: rgba(0, 122, 255, 0.15);
  transform: translateY(-1px);
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-detail {
  padding: 8px 12px;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 4px;
}

.user-openid {
  font-size: 11px;
  color: #007aff;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  margin-bottom: 6px;
  padding: 4px 8px;
  background: rgba(0, 122, 255, 0.08);
  border-radius: 4px;
  word-break: break-all;
}

.user-role {
  font-size: 12px;
  color: #86868b;
}

/* iOS-style Main Content */
.main-content {
  background: #f5f5f7;
  padding: 20px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* Mobile Responsive - iOS optimized */
@media (max-width: 768px) {
  .layout-container {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100% !important;
    height: auto;
    border-right: none;
    border-bottom: 0.5px solid rgba(0, 0, 0, 0.08);
  }
  
  .logo {
    height: 70px;
    padding: 12px 10px;
  }
  
  .logo-image {
    width: 40px;
    height: 40px;
  }
  
  .logo-text {
    font-size: 12px;
  }
  
  .sidebar-menu {
    display: flex;
    overflow-x: auto;
    overflow-y: hidden;
    white-space: nowrap;
    padding: 8px;
    -webkit-overflow-scrolling: touch;
  }
  
  .sidebar-menu :deep(.el-menu-item) {
    display: inline-flex;
    margin: 0 4px;
    padding: 0 16px;
    border-radius: 20px;
    height: 36px;
    line-height: 36px;
    font-size: 13px;
  }
  
  .sidebar-menu :deep(.el-menu-item span) {
    margin-left: 6px;
  }
  
  .header {
    padding: 0 16px;
  }
  
  .header-title {
    font-size: 18px;
  }
  
  .header-subtitle {
    font-size: 10px;
  }
  
  .header-right {
    gap: 8px;
  }
  
  .header-right .el-icon {
    padding: 8px;
    font-size: 18px;
  }
  
  .main-content {
    padding: 16px;
  }
}

/* iPhone X and later - safe area support */
@supports (padding: max(0px)) {
  .sidebar,
  .header,
  .main-content {
    padding-left: max(16px, env(safe-area-inset-left));
    padding-right: max(16px, env(safe-area-inset-right));
  }
  
  .main-content {
    padding-bottom: max(20px, env(safe-area-inset-bottom));
  }
}

/* Dark mode support (iOS 13+) */
@media (prefers-color-scheme: dark) {
  .sidebar {
    background: linear-gradient(180deg, #1c1c1e 0%, #2c2c2e 100%);
    border-right-color: rgba(255, 255, 255, 0.1);
  }
  
  .logo {
    background: rgba(28, 28, 30, 0.8);
    border-bottom-color: rgba(255, 255, 255, 0.05);
  }
  
  .logo-text {
    color: #f5f5f7;
  }
  
  .sidebar-menu :deep(.el-menu-item) {
    color: #f5f5f7;
  }
  
  .header {
    background: rgba(28, 28, 30, 0.8);
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }
  
  .header-title {
    color: #f5f5f7;
  }
  
  .header-subtitle {
    color: #98989d;
  }
  
  .header-right .el-icon {
    color: #f5f5f7;
    background: rgba(255, 255, 255, 0.1);
  }
  
  .main-content {
    background: #000000;
  }
}
</style>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

#app {
  height: 100vh;
}

* {
  box-sizing: border-box;
}
</style>
