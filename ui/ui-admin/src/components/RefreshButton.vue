<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  cooldown: {
    type: Number,
    default: 30
  },
  cooldownKey: {
    type: String,
    default: 'default'
  }
})

const emit = defineEmits(['refresh'])

const storageKey = `wokit:refresh-cooldown:${props.cooldownKey}`
const remaining = ref(0)
let timer = null
let cooldownEndAt = 0

const stopTimer = () => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
}

const clearCooldown = () => {
  cooldownEndAt = 0
  window.localStorage.removeItem(storageKey)
}

const syncRemaining = () => {
  remaining.value = cooldownEndAt
    ? Math.max(0, Math.ceil((cooldownEndAt - Date.now()) / 1000))
    : 0
  if (remaining.value === 0) {
    clearCooldown()
    stopTimer()
  }
}

const startTimer = () => {
  if (timer) {
    return
  }
  timer = window.setInterval(syncRemaining, 1000)
}

const restoreCooldown = () => {
  const savedEndAt = Number(window.localStorage.getItem(storageKey) || 0)
  if (!Number.isFinite(savedEndAt) || savedEndAt <= Date.now()) {
    clearCooldown()
    return
  }
  cooldownEndAt = savedEndAt
  syncRemaining()
  if (remaining.value > 0) {
    startTimer()
  }
}

const startCooldown = () => {
  cooldownEndAt = Date.now() + Math.max(1, Math.floor(props.cooldown)) * 1000
  window.localStorage.setItem(storageKey, String(cooldownEndAt))
  syncRemaining()
  startTimer()
}

const coolingDown = computed(() => remaining.value > 0)
const buttonText = computed(() => (coolingDown.value ? `${remaining.value}秒后可刷新` : '刷新'))

const handleClick = () => {
  if (props.disabled || props.loading || coolingDown.value) {
    return
  }
  startCooldown()
  emit('refresh')
}

restoreCooldown()
onBeforeUnmount(stopTimer)
</script>

<template>
  <el-button
    v-bind="$attrs"
    :icon="Refresh"
    :loading="loading"
    :disabled="disabled || coolingDown"
    @click="handleClick"
  >
    {{ buttonText }}
  </el-button>
</template>
