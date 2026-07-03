<template>
  <span :class="['entity-label', { 'entity-label--stacked': stacked }]">
    <span v-if="showType" class="entity-label__type">{{ typeText }}</span>
    <span v-if="displayName" class="entity-label__name">{{ displayName }}</span>
    <span v-if="showId" class="entity-label__id">ID: {{ entityId }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { useEntityResolver } from '@/composables/useEntityResolver'

const TYPE_MAP = {
  user: '用户',
  project: '项目',
  evaluation: '评价',
  penalty: '处罚',
  report: '举报',
  appeal: '申诉',
}

const props = defineProps({
  type: {
    type: String,
    required: true,
    validator: (value) => ['user', 'project', 'evaluation', 'penalty', 'report', 'appeal'].includes(value),
  },
  entityId: { type: [Number, String], required: true },
  showType: { type: Boolean, default: true },
  showId: { type: Boolean, default: true },
  stacked: { type: Boolean, default: false },
})

const { userName, projectName } = useEntityResolver()

const typeText = computed(() => TYPE_MAP[props.type] || props.type)

const resolvedName = computed(() => {
  if (props.entityId == null || props.entityId === '') return ''
  if (props.type === 'user') return userName(props.entityId)
  if (props.type === 'project') return projectName(props.entityId)
  return ''
})

/** 名称已解析时展示；fallback「用户#123」等与类型+ID重复则不展示 */
const displayName = computed(() => {
  const name = resolvedName.value
  if (!name || name === '-') return ''
  const typedFallback = `${typeText.value}#${props.entityId}`
  if (name === typedFallback || name === `用户#${props.entityId}` || name === `项目#${props.entityId}`) {
    return ''
  }
  return name
})
</script>

<style scoped>
.entity-label { display: inline-flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.entity-label--stacked { flex-direction: column; align-items: flex-start; gap: 4px; }
.entity-label__type {
  padding: 1px 6px;
  border-radius: 2px;
  background: rgba(0, 96, 169, 0.10);
  color: #0060A9;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.entity-label__name { color: #191C1E; font-weight: 500; }
.entity-label__id {
  padding: 1px 6px;
  border-radius: 2px;
  background: rgba(192, 199, 212, 0.30);
  color: #404752;
  font-size: 11px;
  font-family: Inter, sans-serif;
  white-space: nowrap;
}
</style>
