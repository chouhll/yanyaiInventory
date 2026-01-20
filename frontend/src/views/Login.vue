<template>
  <div class="login-container">
    <div class="login-card">
      <!-- Logo区域 -->
      <div class="logo-section">
        <img src="/logo.png" alt="燕泰智管" class="logo" />
        <h1 class="app-name">燕泰智管</h1>
        <p class="app-subtitle">SuperInventory - 智能库存管理</p>
      </div>

      <!-- 二维码登录区域 -->
      <div class="qrcode-section" v-if="!loading">
        <div class="qrcode-container">
          <img v-if="qrcodeUrl && !qrcodeExpired" :src="qrcodeUrl" alt="扫码登录" class="qrcode-image" />
          
          <!-- 过期遮罩 -->
          <div v-if="qrcodeExpired" class="qrcode-expired">
            <el-icon :size="50"><CircleClose /></el-icon>
            <p>二维码已过期</p>
            <el-button type="primary" @click="refreshQRCode" :loading="refreshing">
              刷新二维码
            </el-button>
          </div>
          
          <!-- 扫码中状态 -->
          <div v-if="scanning" class="qrcode-scanning">
            <el-icon class="is-loading" :size="40"><Loading /></el-icon>
            <p>扫码成功，等待授权...</p>
          </div>
        </div>
        
        <div class="qrcode-tips">
          <el-icon><Iphone /></el-icon>
          <span>请使用微信扫一扫登录</span>
        </div>
        
        <!-- 倒计时 -->
        <div class="countdown" v-if="countdown > 0 && !qrcodeExpired">
          <el-icon><Clock /></el-icon>
          <span>{{ Math.floor(countdown / 60) }}:{{ String(countdown % 60).padStart(2, '0') }}</span>
        </div>
      </div>

      <!-- 初始加载中 -->
      <div v-if="loading" class="loading-section">
        <el-icon class="is-loading" :size="50"><Loading /></el-icon>
        <p>正在生成二维码...</p>
      </div>
    </div>

    <!-- 页脚 -->
    <div class="footer">
      <p>© 2026 燕泰智管 SuperInventory</p>
      <p class="footer-tips">安全 · 快捷 · 便利</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, CircleClose, Iphone, Clock } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import authApi from '../api/auth'

const router = useRouter()
const route = useRoute()

// 状态
const loading = ref(true)
const refreshing = ref(false)
const scanning = ref(false)
const qrcodeUrl = ref('')
const qrcodeState = ref('')
const qrcodeExpired = ref(false)
const countdown = ref(300) // 5分钟倒计时

// 轮询相关
let pollInterval = null
let countdownInterval = null
let pollAttempts = 0
const maxPollAttempts = 150 // 5分钟 = 150次 * 2秒

/**
 * 生成二维码
 */
const generateQRCode = async () => {
  try {
    loading.value = true
    qrcodeExpired.value = false
    countdown.value = 300
    pollAttempts = 0
    
    // 获取二维码数据
    const response = await authApi.generateQRCode()
    
    if (response.success) {
      const { state, authUrl, expiresIn } = response.data
      
      // 保存state
      qrcodeState.value = state
      
      // 生成二维码图片
      qrcodeUrl.value = await QRCode.toDataURL(authUrl, {
        width: 280,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#ffffff'
        }
      })
      
      // 开始轮询
      startPolling()
      
      // 开始倒计时
      startCountdown(expiresIn)
      
      loading.value = false
    } else {
      ElMessage.error(response.message || '生成二维码失败')
      loading.value = false
    }
  } catch (error) {
    console.error('生成二维码失败:', error)
    ElMessage.error('生成二维码失败，请刷新页面重试')
    loading.value = false
  }
}

/**
 * 开始轮询登录状态
 */
const startPolling = () => {
  // 清除之前的轮询
  if (pollInterval) {
    clearInterval(pollInterval)
  }
  
  pollInterval = setInterval(async () => {
    pollAttempts++
    
    // 检查是否超过最大尝试次数
    if (pollAttempts >= maxPollAttempts) {
      stopPolling()
      qrcodeExpired.value = true
      ElMessage.warning('二维码已过期，请刷新')
      return
    }
    
    try {
      const response = await authApi.checkQRCodeStatus(qrcodeState.value)
      
      if (response.success) {
        const { status, token } = response.data
        
        if (status === 'success') {
          // 登录成功
          scanning.value = true
          stopPolling()
          
          // 获取用户信息
          authApi.setToken(token)
          const userResponse = await authApi.getUserInfo()
          
          if (userResponse.success) {
            authApi.setUser(userResponse.data)
            ElMessage.success('登录成功！')
            
            setTimeout(() => {
              router.push('/dashboard')
            }, 500)
          }
        } else if (status === 'expired') {
          // 二维码过期
          stopPolling()
          qrcodeExpired.value = true
          ElMessage.warning('二维码已过期，请刷新')
        }
        // status === 'pending' 继续轮询
      }
    } catch (error) {
      console.error('检查登录状态失败:', error)
      // 继续轮询，不中断
    }
  }, 2000) // 每2秒检查一次
}

/**
 * 停止轮询
 */
const stopPolling = () => {
  if (pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
  if (countdownInterval) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }
}

/**
 * 开始倒计时
 */
const startCountdown = (expiresIn) => {
  countdown.value = expiresIn
  
  if (countdownInterval) {
    clearInterval(countdownInterval)
  }
  
  countdownInterval = setInterval(() => {
    countdown.value--
    
    if (countdown.value <= 0) {
      stopPolling()
      qrcodeExpired.value = true
    }
  }, 1000)
}

/**
 * 刷新二维码
 */
const refreshQRCode = async () => {
  refreshing.value = true
  scanning.value = false
  await generateQRCode()
  refreshing.value = false
}

/**
 * 处理URL回调（兼容旧的直接跳转方式）
 */
const handleCallback = async () => {
  const wechatToken = route.query.wechat_token
  const wechatUser = route.query.wechat_user
  const error = route.query.error
  
  if (error) {
    if (error === 'no_code') {
      ElMessage.error('授权失败：未获取到授权码')
    } else if (error === 'auth_failed') {
      ElMessage.error('授权失败：请重试')
    }
    // 重新生成二维码
    await generateQRCode()
    return
  }
  
  if (wechatToken && wechatUser) {
    try {
      const token = decodeURIComponent(wechatToken)
      const userStr = decodeURIComponent(wechatUser)
      const user = JSON.parse(userStr)
      
      authApi.setToken(token)
      authApi.setUser(user)
      
      ElMessage.success('登录成功！')
      router.push('/dashboard')
    } catch (error) {
      console.error('处理登录信息失败:', error)
      ElMessage.error('登录失败，请重试')
      await generateQRCode()
    }
  }
}

onMounted(async () => {
  // 检查是否已登录
  if (authApi.isAuthenticated()) {
    router.push('/')
    return
  }

  // 检查是否有回调参数
  if (route.query.wechat_token || route.query.error) {
    await handleCallback()
  } else {
    // 生成二维码
    await generateQRCode()
  }
})

onUnmounted(() => {
  // 组件卸载时清理定时器
  stopPolling()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 50px 40px;
  max-width: 450px;
  width: 100%;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.login-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.logo-section {
  margin-bottom: 30px;
  animation: fadeInDown 0.6s ease-out;
}

.logo {
  width: 80px;
  height: 80px;
  object-fit: contain;
  margin-bottom: 15px;
}

.app-name {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 10px 0 5px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.app-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

/* 二维码区域 */
.qrcode-section {
  animation: fadeInUp 0.6s ease-out 0.2s both;
}

.qrcode-container {
  width: 280px;
  height: 280px;
  margin: 0 auto 20px;
  position: relative;
  background: #f5f5f5;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.qrcode-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qrcode-expired {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 15px;
}

.qrcode-expired .el-icon {
  color: #f56c6c;
}

.qrcode-expired p {
  margin: 0;
  color: #666;
  font-size: 16px;
}

.qrcode-scanning {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(102, 126, 234, 0.95);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 15px;
}

.qrcode-scanning .el-icon {
  color: white;
}

.qrcode-scanning p {
  margin: 0;
  color: white;
  font-size: 16px;
  font-weight: 500;
}

.qrcode-tips {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
  margin-bottom: 15px;
}

.qrcode-tips .el-icon {
  color: #09bb07;
  font-size: 20px;
}

.countdown {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
  font-size: 13px;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
}

.countdown .el-icon {
  color: #667eea;
}

/* 加载中 */
.loading-section {
  padding: 60px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.loading-section .el-icon {
  color: #667eea;
}

.loading-section p {
  margin: 0;
  color: #666;
  font-size: 16px;
}

/* 页脚 */
.footer {
  margin-top: 40px;
  text-align: center;
  animation: fadeIn 0.6s ease-out 0.5s both;
}

.footer p {
  margin: 5px 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.footer-tips {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 2px;
}

/* 动画 */
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-card {
    padding: 40px 30px;
  }

  .logo {
    width: 70px;
    height: 70px;
  }

  .app-name {
    font-size: 24px;
  }

  .app-subtitle {
    font-size: 13px;
  }

  .qrcode-container {
    width: 240px;
    height: 240px;
  }
}
</style>
