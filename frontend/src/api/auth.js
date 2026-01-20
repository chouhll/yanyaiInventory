import axios from './index'

/**
 * 认证相关API
 */
export default {
  /**
   * 获取微信授权URL
   * @returns {Promise} 返回授权URL和state
   */
  getWeChatAuthUrl() {
    return axios.get('/auth/wechat/login')
  },

  /**
   * 处理微信回调
   * @param {string} code - 微信返回的授权码
   * @param {string} state - 状态参数
   * @returns {Promise} 返回Token和用户信息
   */
  handleWeChatCallback(code, state) {
    return axios.get('/auth/wechat/callback', {
      params: { code, state }
    })
  },

  /**
   * 验证Token是否有效
   * @returns {Promise} 返回验证结果
   */
  verifyToken() {
    return axios.post('/auth/verify')
  },

  /**
   * 获取当前用户信息
   * @returns {Promise} 返回用户信息
   */
  getUserInfo() {
    return axios.get('/auth/userinfo')
  },

  /**
   * 退出登录
   * @returns {Promise} 返回退出结果
   */
  logout() {
    return axios.post('/auth/logout')
  },

  /**
   * 存储Token到localStorage
   * @param {string} token - JWT Token
   */
  setToken(token) {
    localStorage.setItem('auth_token', token)
  },

  /**
   * 从localStorage或Cookie获取Token
   * @returns {string|null} JWT Token
   */
  getToken() {
    // 先尝试从localStorage获取
    let token = localStorage.getItem('auth_token')
    
    // 如果localStorage没有，尝试从Cookie获取
    if (!token) {
      token = this.getCookie('token')
      // 如果Cookie中有token，同步到localStorage
      if (token) {
        this.setToken(token)
      }
    }
    
    return token
  },

  /**
   * 从Cookie中获取值
   * @param {string} name - Cookie名称
   * @returns {string|null} Cookie值
   */
  getCookie(name) {
    const value = `; ${document.cookie}`
    const parts = value.split(`; ${name}=`)
    if (parts.length === 2) {
      return parts.pop().split(';').shift()
    }
    return null
  },

  /**
   * 从localStorage移除Token
   */
  removeToken() {
    localStorage.removeItem('auth_token')
  },

  /**
   * 存储用户信息到localStorage
   * @param {object} user - 用户信息
   */
  setUser(user) {
    localStorage.setItem('user_info', JSON.stringify(user))
  },

  /**
   * 从localStorage或Cookie获取用户信息
   * @returns {object|null} 用户信息
   */
  getUser() {
    // 先尝试从localStorage获取
    let userStr = localStorage.getItem('user_info')
    
    // 如果localStorage没有，尝试从Cookie获取
    if (!userStr) {
      const userCookie = this.getCookie('user')
      if (userCookie) {
        try {
          userStr = decodeURIComponent(userCookie)
          // 同步到localStorage
          localStorage.setItem('user_info', userStr)
        } catch (e) {
          console.error('解析用户Cookie失败:', e)
          return null
        }
      }
    }
    
    return userStr ? JSON.parse(userStr) : null
  },

  /**
   * 从localStorage移除用户信息
   */
  removeUser() {
    localStorage.removeItem('user_info')
  },

  /**
   * 检查是否已登录
   * @returns {boolean} 是否已登录
   */
  isAuthenticated() {
    const token = this.getToken()
    return !!token
  },

  /**
   * 生成二维码登录数据
   * @returns {Promise} 返回state、authUrl和expiresIn
   */
  generateQRCode() {
    return axios.get('/auth/qrcode/generate')
  },

  /**
   * 检查二维码登录状态
   * @param {string} state - 二维码唯一标识
   * @returns {Promise} 返回登录状态和Token（如果已登录）
   */
  checkQRCodeStatus(state) {
    return axios.get('/auth/qrcode/check', {
      params: { state }
    })
  }
}
