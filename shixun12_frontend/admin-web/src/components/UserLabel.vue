<template>
  <EntityLabel type="user" :entity-id="userId" :show-type="showType" :show-id="showId" :stacked="stacked" />
</template>

<script setup>
import EntityLabel from '@/components/EntityLabel.vue'

defineProps({
  userId: { type: [Number, String], required: true },
  showType: { type: Boolean, default: true },
  showId: { type: Boolean, default: true },
  stacked: { type: Boolean, default: false },
})
</script>
