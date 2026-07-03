<template>
  <EntityLabel type="project" :entity-id="projectId" :show-type="showType" :show-id="showId" :stacked="stacked" />
</template>

<script setup>
import EntityLabel from '@/components/EntityLabel.vue'

defineProps({
  projectId: { type: [Number, String], required: true },
  showType: { type: Boolean, default: true },
  showId: { type: Boolean, default: true },
  stacked: { type: Boolean, default: false },
})
</script>
