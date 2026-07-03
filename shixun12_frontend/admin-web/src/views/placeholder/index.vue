<template>
  <div class="placeholder-page">
    <div class="placeholder-card">
      <h1 class="placeholder-title">{{ pageTitle }}</h1>
      <p class="placeholder-desc">该功能模块开发中，敬请期待</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const pageTitle = computed(() => route.meta.title || '功能开发中')
</script>

<style scoped>
.placeholder-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
.placeholder-card {
  text-align: center;
}
.placeholder-title {
  font-size: 30px;
  font-weight: 700;
  color: #191C1E;
  margin: 0 0 8px;
}
.placeholder-desc {
  font-size: 14px;
  color: #404752;
  margin: 0;
}
</style>
