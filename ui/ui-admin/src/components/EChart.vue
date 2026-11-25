<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import * as echarts from 'echarts/core'

use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  CanvasRenderer
])

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '300px' },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' },
  loading: { type: Boolean, default: false }
})

const chartEl = ref()
let chart = null
let resizeObserver = null

const render = async () => {
  await nextTick()
  if (!chartEl.value) {
    return
  }
  if (!chart) {
    chart = echarts.init(chartEl.value)
  }
  chart.setOption(props.option, true)
}

watch(
  () => props.option,
  () => render(),
  { deep: true }
)

onMounted(() => {
  render()
  if (chartEl.value) {
    resizeObserver = new ResizeObserver(() => {
      chart?.resize()
    })
    resizeObserver.observe(chartEl.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="chart-wrap" :style="{ height }">
    <div ref="chartEl" class="chart-canvas" v-loading="loading" element-loading-background="rgba(255, 255, 255, 0.92)"></div>
    <div v-if="empty && !loading" class="chart-empty">{{ emptyText }}</div>
  </div>
</template>

<style scoped>
.chart-wrap {
  position: relative;
  width: 100%;
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 16px;
  color: var(--wk-text-3);
  font-size: 12px;
  pointer-events: none;
}
</style>
