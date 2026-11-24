<script setup>
defineProps({
  title: { type: String, default: '暂无数据' },
  hint: { type: String, default: '' },
  compact: { type: Boolean, default: false }
})
</script>

<template>
  <div class="empty-state" :class="{ compact }">
    <svg class="empty-art" viewBox="0 0 96 76" width="88" height="70" aria-hidden="true">
      <rect x="12" y="14" width="72" height="48" rx="10" fill="#F3FBF7" stroke="#D8F0E4" />
      <path d="M12 42h18l6 9h24l6-9h18" fill="none" stroke="#BFE8D3" stroke-width="2" stroke-linejoin="round" />
      <rect x="30" y="22" width="36" height="5" rx="2.5" fill="#E1F5EB" />
      <rect x="30" y="31" width="24" height="4" rx="2" fill="#EAF9F1" />
      <circle cx="73" cy="22" r="7" fill="#12B76A" opacity="0.12" />
      <circle cx="73" cy="22" r="3" fill="#12B76A" opacity="0.45" />
    </svg>
    <p class="empty-title">{{ title }}</p>
    <p v-if="hint" class="empty-hint">{{ hint }}</p>
    <slot />
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 56px 20px;
  text-align: center;
}

.empty-state.compact {
  padding: 30px 16px;
}

.empty-art {
  margin-bottom: 10px;
}

.empty-title {
  color: #475467;
  font-size: 13.5px;
  font-weight: 500;
}

.empty-hint {
  max-width: 320px;
  color: #98a2b3;
  font-size: 12px;
  line-height: 1.7;
}
</style>
