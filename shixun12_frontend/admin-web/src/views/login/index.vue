<template>
  <!-- Figma: 1280x1024 canvas, bg #F7F9FC + 蓝色径向渐变 + 装饰圆形 -->
  <div class="login-page">
    <!-- 背景层：径向渐变叠加 -->
    <div class="bg-overlay"></div>
    <!-- 装饰光晕圆形 -->
    <div class="deco-circle deco-circle--left"></div>
    <div class="deco-circle deco-circle--right"></div>

    <!-- 居中卡片区域（Figma: 420px 容器，卡片 372x657px 偏移 left:24px） -->
    <div class="card-wrapper">
      <div class="login-card">
        <!-- ===== Logo & 标题区（Figma: left:41px, top:41px） ===== -->
        <div class="brand-area">
          <div class="brand-logo">
            <img src="/assets/CodeBuddyAssets/2_2/1.svg" alt="logo" class="logo-icon" />
          </div>
          <h1 class="brand-title">TeamMatch</h1>
          <p class="brand-subtitle">校园组队与互评平台</p>
        </div>

        <!-- ===== 表单区（Figma: left:41px, top:192px） ===== -->
        <div class="form-area">
          <el-form @keyup.enter="handleLogin">
            <!-- 用户名 -->
            <div class="field-group">
              <label class="field-label">用户名</label>
              <el-input
                v-model="username"
                placeholder="请输入您的用户名"
                :prefix-icon="User"
                size="large"
                class="login-input"
                clearable
              />
            </div>

            <!-- 密码 -->
            <div class="field-group">
              <label class="field-label">
                <span>密码</span>
                <span class="forgot-link">忘记密码？</span>
              </label>
              <el-input
                v-model="password"
                placeholder="请输入您的登录密码"
                type="password"
                :prefix-icon="Lock"
                size="large"
                class="login-input"
                show-password
              />
            </div>

            <!-- 记住登录 -->
            <label class="field-remember">
              <el-checkbox v-model="rememberMe" />
              <span class="remember-text">记住登录状态</span>
            </label>

            <!-- 登录按钮 -->
            <el-button
              type="primary"
              class="login-btn"
              :loading="loading"
              :disabled="!canLogin"
              @click="handleLogin"
            >
              <span>登录</span>
              <el-icon class="btn-arrow"><ArrowRight /></el-icon>
            </el-button>
          </el-form>
        </div>

      </div>

      <!-- ===== 底部链接 + 版权（Figma: top:705px） ===== -->
      <div class="footer-area">
        <div class="footer-links">
          <a href="#">服务条款</a>
          <span class="footer-divider"></span>
          <a href="#">隐私政策</a>
          <span class="footer-divider"></span>
          <a href="#">联系支持</a>
        </div>
        <p class="footer-copyright">
          TeamMatch&nbsp;&copy;&nbsp;2026&nbsp;版权所有&nbsp;·&nbsp;<a href="https://beian.miit.gov.cn" target="_blank" rel="noopener">粤ICP备2026073756号</a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, ArrowRight } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const username = ref('')
const password = ref('')
const rememberMe = ref(false)
const loading = ref(false)

const canLogin = computed(() => username.value.trim() && password.value.trim())

const handleLogin = async () => {
  if (!canLogin.value || loading.value) return
  loading.value = true

  try {
    const res = await axios.post('/api/auth/login/password', {
      username: username.value.trim(),
      password: password.value
    })
    const data = res.data
    if (data.code === '00000' && data.data && data.data.token) {
      localStorage.setItem('adminToken', data.data.token)
      const nickname = data.data.displayName || data.data.nickname || '管理员'
      ElMessage.success(`欢迎, ${nickname}`)
      router.push('/board')
    } else {
      ElMessage.error(data.message || '登录失败，请检查账号密码')
    }
  } catch (err) {
    const msg = err.response?.data?.message || '登录失败，请检查账号密码'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ================================================================
 * 所有色值、间距、圆角严格从 Figma 原型图提取
 * 画布尺寸: 1280 x 1024px
 * 卡片尺寸: 372 x 657px（容器 420px，卡片偏移 left:24px）
 * ================================================================ */

/* ========== 页面背景层 ========== */
.login-page {
  width: 100%;
  min-height: 100vh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  /* Figma: background: linear-gradient(0deg, #F7F9FC, #F7F9FC), white */
  background: #F7F9FC;
}

/* 径向渐变叠加层 */
.bg-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse 110.44% 138.04% at 10% 20%, rgba(64,158,255,0.05) 0%, rgba(64,158,255,0) 40%),
    radial-gradient(ellipse 110.44% 138.04% at 90% 80%, rgba(64,158,255,0.08) 0%, rgba(64,158,255,0) 40%);
}

/* 装饰光晕圆 */
.deco-circle {
  position: absolute;
  width: 384px;
  height: 384px;
  border-radius: 9999px;
  filter: blur(32px);
  pointer-events: none;
}
.deco-circle--left {
  left: -96px;
  top: -96px;
  background: rgba(64,158,255,0.10);
  box-shadow: 64px 64px 64px rgba(64,158,255,0.05);
}
.deco-circle--right {
  right: -96px;
  bottom: -96px;
  background: rgba(210,228,255,0.10);
  box-shadow: 64px 64px 64px rgba(210,228,255,0.05);
}

/* ========== 卡片容器 ========== */
.card-wrapper {
  position: relative;
  width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 1;
}

/* Figma: 372x657, bg white, border-radius 8px, 
   box-shadow 0 2px 12px rgba(0,0,0,0.05), outline 1px #C0C7D4 */
.login-card {
  width: 372px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0px 2px 12px rgba(0,0,0,0.05);
  border: 1px solid #C0C7D4;
  padding: 41px 41px 32px;
  box-sizing: border-box;
}

/* ========== Brand 区域 ========== */
.brand-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

/* Figma: 56x35, bg #0060A9, border-radius 8px */
.brand-logo {
  width: 56px;
  height: 35px;
  background: #0060A9;
  border-radius: 8px;
  box-shadow: 0px 1px 2px rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}
.logo-icon {
  width: 30px;
  height: 15px;
}

/* Figma: "TeamMatch", 30px, Inter Bold 700, color #191C1E */
.brand-title {
  margin: 0;
  font-size: 30px;
  font-family: Inter, -apple-system, BlinkMacSystemFont, sans-serif;
  font-weight: 700;
  color: #191C1E;
  line-height: 1.4;
}

/* Figma: "校园组队与互评平台", 14px, color #404752 */
.brand-subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #404752;
  font-weight: 400;
}

/* ========== 表单区域 ========== */
.form-area {
  width: 290px;
  margin: 0 auto;
}

.field-group {
  margin-bottom: 24px;
}

/* Figma: 12px, weight 500, color #404752, letter-spacing 0.12px */
.field-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
  color: #404752;
  letter-spacing: 0.12px;
  margin-bottom: 8px;
}

.forgot-link {
  color: #0060A9;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.12px;
}
.forgot-link:hover {
  opacity: 0.8;
}

/* ===== Element Plus 输入框覆写（匹配 Figma） ===== */
.login-input :deep(.el-input__wrapper) {
  height: 44px;
  border-radius: 4px;
  border: 1px solid #C0C7D4;
  box-shadow: none;
  background: #fff;
  padding: 0 12px 0 0;
}
.login-input :deep(.el-input__wrapper:hover) {
  border-color: #409EFF;
}
.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: #409EFF;
  box-shadow: 0 0 0 1px rgba(64,158,255,0.2);
}
/* 前缀图标区域 */
.login-input :deep(.el-input__prefix) {
  left: 12px;
}
.login-input :deep(.el-input__prefix .el-input__icon) {
  color: #C0C7D4;
  font-size: 20px;
}
/* 输入文字 */
.login-input :deep(.el-input__inner) {
  font-size: 16px;
  color: #191C1E;
  height: 42px;
  line-height: 42px;
  padding-left: 40px;
}
.login-input :deep(.el-input__inner::placeholder) {
  color: #C0C7D4;
  font-size: 16px;
}
/* 密码可见图标 */
.login-input :deep(.el-input__suffix .el-input__icon) {
  color: #C0C7D4;
}

/* ===== 记住登录 ===== */
.field-remember {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  cursor: pointer;
}
.field-remember :deep(.el-checkbox__inner) {
  border-color: #C0C7D4;
  border-radius: 2px;
}
.remember-text {
  font-size: 13px;
  color: #404752;
}

/* ===== 登录按钮 ===== */
.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 4px;
  background: #409EFF;
  border: none;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  box-shadow: 0px 1px 2px rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.2s;
}
.login-btn:not(:disabled):hover {
  background: #337ECC;
}
.login-btn.is-disabled,
.login-btn:disabled {
  background: #A0CFFF;
  cursor: not-allowed;
}
.btn-arrow {
  font-size: 18px;
  vertical-align: middle;
}

/* ===== 分隔线区域 ===== */
.divider-area {
  position: relative;
  width: 290px;
  margin: 32px auto;
  text-align: center;
}
.divider-line {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #C0C7D4;
}
.divider-text {
  position: relative;
  display: inline-block;
  background: #fff;
  padding: 0 16px;
  font-size: 12px;
  color: #707784;
  font-weight: 500;
  letter-spacing: 0.12px;
}

/* ===== 社交登录按钮 ===== */
.social-area {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}
.social-btn {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid #C0C7D4;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: border-color 0.2s;
}
.social-btn:hover {
  border-color: #409EFF;
}
.social-btn img {
  width: 24px;
  height: 24px;
}

/* ========== 底部区域 ========== */
.footer-area {
  text-align: center;
  margin-top: 16px;
}

/* Figma: 12px, color #404752, letter-spacing 0.12px */
.footer-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 8px;
}
.footer-links a {
  font-size: 12px;
  color: #404752;
  font-weight: 500;
  letter-spacing: 0.12px;
  text-decoration: none;
  cursor: pointer;
  padding: 0 8px;
}
.footer-links a:hover {
  color: #0060A9;
}
.footer-divider {
  width: 1px;
  height: 10px;
  background: #C0C7D4;
}

/* Figma: 12px, Inter 500, color #707784, letter-spacing 0.12px */
.footer-copyright {
  margin: 0;
  font-size: 12px;
  font-family: Inter, -apple-system, BlinkMacSystemFont, sans-serif;
  font-weight: 500;
  color: #707784;
  letter-spacing: 0.12px;
  line-height: 16px;
}

/* ========== 响应式适配 ========== */
@media (max-width: 480px) {
  .login-card {
    width: 92vw;
    padding: 32px 5vw 24px;
  }
  .form-area,
  .divider-area {
    width: 100%;
  }
  .deco-circle {
    width: 200px;
    height: 200px;
  }
}
</style>
