<template>
  <!-- Figma: 1280x1024, 侧边栏 220px + 右侧 1060px -->
  <el-container class="layout-container">
    <!-- ===== 侧边栏 ===== -->
    <el-aside width="220px" class="layout-aside">
      <!-- Logo 区 (Figma: 50px 高) -->
      <div class="aside-logo">
        <img src="/assets/CodeBuddyAssets/3_328/18.svg" alt="logo" class="logo-icon" />
        <span class="logo-text">TeamMatch</span>
      </div>

      <!-- 菜单 (Figma: 菜单项 42px 高, 选中态蓝色) -->
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#0A1C30"
        text-color="#B6C8E2"
        active-text-color="#D3E4FF"
        class="aside-menu"
      >
        <el-menu-item index="/board">
          <img src="/assets/CodeBuddyAssets/3_328/19.svg" class="menu-icon" />
          <span>板块管理</span>
        </el-menu-item>
        <el-menu-item index="/reports">
          <img src="/assets/CodeBuddyAssets/3_328/20.svg" class="menu-icon" />
          <span>举报处理</span>
        </el-menu-item>
        <el-menu-item index="/appeals">
          <img src="/assets/CodeBuddyAssets/3_328/21.svg" class="menu-icon" />
          <span>申诉处理</span>
        </el-menu-item>
        <el-menu-item index="/evaluations">
          <img src="/assets/CodeBuddyAssets/3_328/22.svg" class="menu-icon" />
          <span>评价复核</span>
        </el-menu-item>
        <el-menu-item index="/penalties">
          <img src="/assets/CodeBuddyAssets/3_328/23.svg" class="menu-icon" />
          <span>处罚管理</span>
        </el-menu-item>
      </el-menu>

      <!-- 底部用户区 (Figma: border-top #37485D) -->
      <div class="aside-footer">
        <div class="aside-user">
          <img src="/assets/CodeBuddyAssets/3_328/29.svg" class="user-avatar" />
          <div class="user-info">
            <span class="user-name">Admin_Console</span>
            <span class="user-role">SUPER ADMINISTRATOR</span>
          </div>
        </div>
        <div class="aside-actions">
          <button class="aside-btn" title="设置">
            <img src="/assets/CodeBuddyAssets/3_328/25.svg" />
          </button>
          <button class="aside-btn" title="退出" @click="handleLogout">
            <img src="/assets/CodeBuddyAssets/3_328/26.svg" />
          </button>
        </div>
      </div>
    </el-aside>

    <!-- ===== 右侧主区域 ===== -->
    <el-container class="layout-main">
      <!-- 顶部栏 (Figma: 50px, 白色, border-bottom #C0C7D4) -->
      <el-header height="50px" class="layout-header">
        <!-- 面包屑 -->
        <el-breadcrumb separator="/" class="header-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/board' }">
            <img src="/assets/CodeBuddyAssets/3_328/1.svg" class="breadcrumb-home" />
          </el-breadcrumb-item>
          <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
            {{ item.name }}
          </el-breadcrumb-item>
        </el-breadcrumb>

        <!-- 右侧: 通知 + 用户 -->
        <div class="header-right">
          <div class="header-icons">
            <img src="/assets/CodeBuddyAssets/3_328/3.svg" class="header-icon" title="切换暗色模式" @click="toggleTheme" />
          </div>
          <div class="header-user">
            <span>系统管理员</span>
          </div>
        </div>
      </el-header>

      <!-- 内容区 (Figma: #F5F7FA, padding 24px) -->
      <el-main class="layout-content">
        <router-view />
      </el-main>

      <!-- 底部备案号 -->
      <div class="layout-footer">
        <a href="https://beian.miit.gov.cn" target="_blank" rel="noopener">粤ICP备2026073756号</a>
      </div>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  const items = []
  const matched = route.matched.filter(r => r.meta && r.meta.title)
  for (const m of matched) {
    items.push({ name: m.meta.title, path: m.path })
  }
  return items
})

const isDark = ref(localStorage.getItem('adminTheme') === 'dark')

const toggleTheme = () => {
  isDark.value = !isDark.value
  localStorage.setItem('adminTheme', isDark.value ? 'dark' : 'light')
  document.documentElement.classList.toggle('dark', isDark.value)
}

// 初始化暗色模式
if (isDark.value) {
  document.documentElement.classList.add('dark')
}

const handleLogout = () => {
  localStorage.removeItem('adminToken')
  router.push('/login')
}
</script>

<style scoped>
/* ================================================================
 * 所有色值、间距严格从 Figma 3_328 提取
 * 侧边栏: 220px, bg #0A1C30
 * 顶部栏: 50px, bg white, border #C0C7D4
 * 内容区: bg #F5F7FA
 * ================================================================ */

.layout-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* ========== 侧边栏 ========== */
.layout-aside {
  background: #0A1C30;
  box-shadow: 0px 4px 6px -4px rgba(0,0,0,0.10), 0px 10px 15px -3px rgba(0,0,0,0.10);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Logo (Figma: 50px, 文字白色 24px Inter Bold) */
.aside-logo {
  height: 50px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 6px;
  flex-shrink: 0;
}
.logo-icon {
  width: 18px;
  height: 18px;
}
.logo-text {
  font-size: 24px;
  font-family: Inter, sans-serif;
  font-weight: 700;
  color: #fff;
}

/* 菜单 */
.aside-menu {
  flex: 1;
  overflow-y: auto;
  border-right: none;
  padding-top: 16px;
}
.aside-menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.12px;
  padding-left: 16px !important;
}
.aside-menu :deep(.el-menu-item:hover) {
  background: rgba(64,158,255,0.10);
}
.aside-menu :deep(.el-menu-item.is-active) {
  background: rgba(64,158,255,0.20);
  border-right: 4px solid #409EFF;
  color: #D3E4FF;
}
.menu-icon {
  width: 18px;
  height: 18px;
  margin-right: 12px;
  vertical-align: middle;
}

/* 底部用户区 */
.aside-footer {
  flex-shrink: 0;
  border-top: 1px solid #37485D;
  padding: 17px 16px;
}
.aside-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
}
.user-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.user-name {
  font-size: 12px;
  font-family: Inter, sans-serif;
  font-weight: 500;
  letter-spacing: 0.12px;
  color: #fff;
}
.user-role {
  font-size: 10px;
  font-family: Inter, sans-serif;
  font-weight: 400;
  letter-spacing: 0.5px;
  color: #B6C8E2;
}
.aside-actions {
  display: flex;
  gap: 8px;
}
.aside-btn {
  width: 90px;
  height: 28px;
  background: #37485D;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}
.aside-btn:hover {
  background: #4a5f78;
}
.aside-btn img {
  width: 16px;
  height: 16px;
}

/* ========== 右侧主区 ========== */
.layout-main {
  flex-direction: column;
  background: #F5F7FA;
}

/* 顶部栏 (Figma: 50px, white, border-bottom #C0C7D4) */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #C0C7D4;
  padding: 0 24px;
  flex-shrink: 0;
}

/* 面包屑 */
.header-breadcrumb :deep(.el-breadcrumb__item .el-breadcrumb__inner) {
  font-size: 13px;
  color: #404752;
  font-weight: 400;
}
.header-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #0060A9;
  font-weight: 700;
}
.header-breadcrumb :deep(.el-breadcrumb__separator) {
  color: #C0C7D4;
}
.breadcrumb-home {
  width: 16px;
  height: 16px;
  vertical-align: middle;
}

/* 右侧图标+用户 */
.header-right {
  display: flex;
  align-items: center;
  gap: 0;
}
.header-icons {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-icon {
  width: 34px;
  height: 34px;
  cursor: pointer;
}
.header-user {
  margin-left: 24px;
  padding-left: 24px;
  border-left: 1px solid #C0C7D4;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.12px;
  color: #191C1E;
  white-space: nowrap;
}

/* 内容区 (Figma: #F5F7FA, padding 24px) */
.layout-content {
  background: #F5F7FA;
  padding: 24px;
  overflow-y: auto;
}

/* ========== 暗色模式 ========== */
:root.dark .layout-header { background: #1E293B; border-color: #334155; }
:root.dark .layout-main { background: #0F172A; }
:root.dark .layout-content { background: #0F172A; }
:root.dark .header-breadcrumb :deep(.el-breadcrumb__item .el-breadcrumb__inner) { color: #94A3B8; }
:root.dark .header-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color: #60A5FA; }
:root.dark .header-user { color: #E2E8F0; border-color: #475569; }
:root.dark .header-icon { filter: invert(1); }
</style>

<!-- 暗色模式全局样式（非scoped，覆盖Element Plus组件） -->
<style>
:root.dark .el-table,
:root.dark .el-table__body-wrapper,
:root.dark .el-table__header-wrapper { background: #1E293B !important; }
:root.dark .el-table th.el-table__cell { background: #0F172A !important; color: #94A3B8 !important; border-color: #334155 !important; }
:root.dark .el-table td.el-table__cell { background: #1E293B !important; color: #E2E8F0 !important; border-color: #334155 !important; }
:root.dark .el-table tr { background: #1E293B !important; }
:root.dark .el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell { background: #283445 !important; }
:root.dark .el-table__body tr:hover > td { background: #334155 !important; }
:root.dark .el-table .el-table__cell { color: #E2E8F0; }

:root.dark .el-card { background: #1E293B !important; border-color: #334155 !important; color: #E2E8F0 !important; }
:root.dark .el-card__header { background: #0F172A !important; border-color: #334155 !important; color: #E2E8F0 !important; }
:root.dark .el-card__body { color: #94A3B8 !important; }

:root.dark .el-pagination button,
:root.dark .el-pager li { background: #1E293B !important; color: #94A3B8 !important; }
:root.dark .el-pager li.is-active { background: #2563EB !important; color: #fff !important; }
:root.dark .el-pagination .btn-next, :root.dark .el-pagination .btn-prev { color: #94A3B8 !important; }

:root.dark .el-input__inner,
:root.dark .el-textarea__inner,
:root.dark .el-input__wrapper { background: #1E293B !important; border-color: #475569 !important; color: #E2E8F0 !important; }
:root.dark .el-input__inner::placeholder { color: #64748B; }
:root.dark .el-select .el-input .el-select__caret { color: #94A3B8; }

:root.dark .el-form-item__label { color: #94A3B8 !important; }

:root.dark .el-tag--primary { background: rgba(37,99,235,0.2); border-color: rgba(37,99,235,0.3); color: #60A5FA; }
:root.dark .el-tag--success { background: rgba(16,185,129,0.2); border-color: rgba(16,185,129,0.3); color: #34D399; }
:root.dark .el-tag--danger { background: rgba(239,68,68,0.2); border-color: rgba(239,68,68,0.3); color: #F87171; }
:root.dark .el-tag--warning { background: rgba(245,158,11,0.2); border-color: rgba(245,158,11,0.3); color: #FBBF24; }
:root.dark .el-tag--info { background: rgba(148,163,184,0.2); border-color: rgba(148,163,184,0.3); color: #94A3B8; }

:root.dark .el-dialog { background: #1E293B !important; }
:root.dark .el-dialog__title { color: #E2E8F0 !important; }
:root.dark .el-dialog__body { color: #94A3B8 !important; }
:root.dark .el-dialog__header { border-color: #334155; }

:root.dark .el-button--default { background: #334155 !important; border-color: #475569 !important; color: #E2E8F0 !important; }
:root.dark .el-button--default:hover { background: #475569 !important; }

:root.dark .el-tabs__item { color: #94A3B8 !important; }
:root.dark .el-tabs__item.is-active { color: #60A5FA !important; }
:root.dark .el-tabs__nav-wrap::after { background: #334155; }

:root.dark .el-divider__text { background: #1E293B; color: #94A3B8; }
:root.dark .el-divider { border-color: #334155; }

:root.dark .el-popconfirm__main { color: #E2E8F0; }
:root.dark .el-popper.is-light { background: #1E293B !important; border-color: #475569 !important; color: #E2E8F0 !important; }
:root.dark .el-popper.is-light .el-popper__arrow::before { background: #1E293B !important; border-color: #475569 !important; }

:root.dark { color-scheme: dark; }
:root.dark body { background: #0F172A; color: #E2E8F0; }

/* 按钮全集 */
:root.dark .el-button--primary { --el-button-bg-color: #2563EB; --el-button-border-color: #2563EB; }
:root.dark .el-button--success { --el-button-bg-color: #059669; --el-button-border-color: #059669; }
:root.dark .el-button--warning { --el-button-bg-color: #D97706; --el-button-border-color: #D97706; }
:root.dark .el-button--danger { --el-button-bg-color: #DC2626; --el-button-border-color: #DC2626; }
:root.dark .el-button--info { --el-button-bg-color: #475569; --el-button-border-color: #475569; }

/* 开关 / 复选框 / 单选框 */
:root.dark .el-switch__label { color: #94A3B8; }
:root.dark .el-checkbox__label { color: #94A3B8; }
:root.dark .el-radio__label { color: #94A3B8; }
:root.dark .el-checkbox__inner { background: #1E293B; border-color: #475569; }
:root.dark .el-radio__inner { background: #1E293B; border-color: #475569; }

/* 下拉菜单 / 选择器面板 */
:root.dark .el-dropdown-menu { background: #1E293B !important; border-color: #334155 !important; }
:root.dark .el-dropdown-menu__item { color: #E2E8F0 !important; }
:root.dark .el-dropdown-menu__item:hover { background: #334155 !important; }
:root.dark .el-select-dropdown { background: #1E293B !important; border-color: #334155 !important; }
:root.dark .el-select-dropdown__item { color: #E2E8F0 !important; }
:root.dark .el-select-dropdown__item.hover, :root.dark .el-select-dropdown__item:hover { background: #334155 !important; }
:root.dark .el-select-dropdown__item.selected { color: #60A5FA !important; }
:root.dark .el-select .el-input.is-focus .el-input__wrapper { box-shadow: 0 0 0 1px #2563EB inset; }
:root.dark .el-input__wrapper.is-focus { box-shadow: 0 0 0 1px #2563EB inset; }

/* 日期选择器 */
:root.dark .el-date-picker { background: #1E293B !important; border-color: #334155 !important; }
:root.dark .el-date-picker__header-label { color: #E2E8F0 !important; }
:root.dark .el-date-table td { color: #94A3B8; }
:root.dark .el-date-table td.current:not(.disabled) { background: #2563EB; color: #fff; }
:root.dark .el-date-table td.today { color: #60A5FA; }
:root.dark .el-month-table td .cell, :root.dark .el-year-table td .cell { color: #94A3B8; }
:root.dark .el-picker-panel__icon-btn { color: #94A3B8; }
:root.dark .el-date-editor .el-input__wrapper { background: #1E293B; }

/* 级联选择器 */
:root.dark .el-cascader__dropdown { background: #1E293B !important; border-color: #334155 !important; }
:root.dark .el-cascader-node { color: #E2E8F0 !important; }
:root.dark .el-cascader-node:not(.is-disabled):hover { background: #334155 !important; }
:root.dark .el-cascader-node__label { color: #E2E8F0; }
:root.dark .el-cascader-panel { background: #1E293B; }

/* 步骤条 */
:root.dark .el-step__title { color: #94A3B8 !important; }
:root.dark .el-step__title.is-process { color: #60A5FA !important; }
:root.dark .el-step__description { color: #64748B !important; }

/* 提示 / 警告 */
:root.dark .el-alert--info { background: rgba(37,99,235,0.15) !important; border-color: rgba(37,99,235,0.3) !important; }
:root.dark .el-alert__title { color: #E2E8F0 !important; }
:root.dark .el-alert__description { color: #94A3B8 !important; }
:root.dark .el-notification { background: #1E293B !important; border-color: #334155 !important; }
:root.dark .el-notification__title { color: #E2E8F0 !important; }
:root.dark .el-notification__content { color: #94A3B8 !important; }

/* 消息提示 */
:root.dark .el-message { background: #1E293B !important; border-color: #334155 !important; }
:root.dark .el-message__content { color: #E2E8F0 !important; }

/* 加载 / 骨架屏 */
:root.dark .el-loading-mask { background: rgba(15,23,42,0.8) !important; }
:root.dark .el-loading-spinner .el-loading-text { color: #E2E8F0; }
:root.dark .el-skeleton__item { background: #334155; }

/* 空状态 */
:root.dark .el-empty__description { color: #64748B; }
:root.dark .el-empty__image svg { fill: #334155; }

/* 描述列表 */
:root.dark .el-descriptions__title { color: #E2E8F0; }
:root.dark .el-descriptions__label { color: #94A3B8; }
:root.dark .el-descriptions__content { color: #E2E8F0; }
:root.dark .el-descriptions__body { background: #1E293B; }
:root.dark .el-descriptions__body .el-descriptions__table.is-bordered .el-descriptions__cell { border-color: #334155; }

/* 头像 */
:root.dark .el-avatar { background: #334155; color: #E2E8F0; }

/* 链接 */
:root.dark .el-link { color: #60A5FA; }
:root.dark .el-link--danger { color: #F87171; }

/* 标签页内容 */
:root.dark .el-tabs__content { color: #E2E8F0; }

/* 输入框各种状态 */
:root.dark .el-input.is-disabled .el-input__wrapper { background: #0F172A !important; box-shadow: 0 0 0 1px #334155 inset; }
:root.dark .el-textarea.is-disabled .el-textarea__inner { background: #0F172A !important; border-color: #334155; }
:root.dark .el-input__inner:focus, :root.dark .el-textarea__inner:focus { border-color: #2563EB; }

/* 抽屉 */
:root.dark .el-drawer { background: #1E293B !important; }
:root.dark .el-drawer__title { color: #E2E8F0 !important; }
:root.dark .el-drawer__header { border-color: #334155; }

/* 树形控件 */
:root.dark .el-tree { background: transparent; color: #E2E8F0; }
:root.dark .el-tree-node__content:hover { background: #334155; }
:root.dark .el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content { background: rgba(37,99,235,0.15); }

/* 菜单（下拉型，非侧边栏） */
:root.dark .el-menu--horizontal { border-color: #334155; }
:root.dark .el-menu--horizontal .el-menu-item { color: #94A3B8; }
:root.dark .el-menu--horizontal .el-menu-item:hover, :root.dark .el-menu--horizontal .el-menu-item.is-active { color: #60A5FA; }

/* 弹窗 */
:root.dark .el-message-box { background: #1E293B !important; border-color: #334155 !important; }
:root.dark .el-message-box__title { color: #E2E8F0 !important; }
:root.dark .el-message-box__message { color: #94A3B8 !important; }

/* 统计数值 */
:root.dark .el-statistic__head { color: #94A3B8; }
:root.dark .el-statistic__number { color: #E2E8F0; }

/* 时间线 */
:root.dark .el-timeline-item__node { background: #334155; }
:root.dark .el-timeline-item__content { color: #94A3B8; }

/* 滑块 */
:root.dark .el-slider__runway { background: #334155; }
:root.dark .el-slider__bar { background: #2563EB; }

/* 进度条 */
:root.dark .el-progress-bar__outer { background: #334155; }

/* 标签页 */
:root.dark .el-tag { border-color: transparent; }

/* ========== 底部备案 ========== */
.layout-footer {
  text-align: center;
  padding: 10px 0;
  background: #F5F7FA;
  border-top: 1px solid #E5E7EB;
  flex-shrink: 0;
}
.layout-footer a {
  font-size: 12px;
  color: #9CA3AF;
  text-decoration: none;
}
.layout-footer a:hover {
  color: #6B7280;
}
</style>
