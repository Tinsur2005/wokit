<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: 0 },
  unit: { type: String, default: '' },
  tone: { type: String, default: 'green' },
  icon: { type: Object, default: null },
  hint: { type: String, default: '' }
})
</script>

<template>
  <div class="stat-card">
    <div class="stat-icon" :class="`tone-${tone}`">
      <el-icon :size="19">
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="stat-body">
      <p class="stat-label">{{ label }}</p>
      <p class="stat-value">
        {{ value }}<span v-if="unit" class="unit">{{ unit }}</span>
      </p>
      <p v-if="hint" class="stat-hint">{{ hint }}</p>
    </div>
  </div>
</template>
