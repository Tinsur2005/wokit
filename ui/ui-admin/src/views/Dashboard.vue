<script setup>
import { computed, onMounted, ref } from 'vue'
import { AlarmClock, Calendar, CircleCheck, Clock } from '@element-plus/icons-vue'
import dashboardApi from '@/api/dashboard'
import EChart from '@/components/EChart.vue'
import RefreshButton from '@/components/RefreshButton.vue'
import StatCard from '@/components/StatCard.vue'

const loading = ref(false)
const loadFailed = ref(false)
const dashboard = ref({
  todayPending: 0,
  todayTotal: 0,
  weekCompleted: 0,
  monthHours: 0,
  runningTimers: 0,
  taskTrend: [],
  projectHours: [],
  weeklyTrend: []
})

const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const todayText = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
})

const monthHoursText = computed(() => Number(dashboard.value.monthHours || 0).toFixed(1))

const taskTrend = computed(() => dashboard.value.taskTrend || [])
const projectHours = computed(() => {
  const merged = new Map()
  for (const item of dashboard.value.projectHours || []) {
    const name = String(item.project || '').trim() || '未命名事项'
    merged.set(name, (merged.get(name) || 0) + Number(item.hours || 0))
  }
  return [...merged.entries()]
    .map(([project, hours]) => ({
      project,
      hours: Number(hours.toFixed(1))
    }))
    .sort((a, b) => b.hours - a.hours)
})
const weeklyTrend = computed(() => dashboard.value.weeklyTrend || [])

const shortenLabel = (value, maxLength = 9) =>
  value.length > maxLength ? `${value.slice(0, maxLength)}…` : value
const escapeHtml = (value) => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#39;')

const taskEmpty = computed(() =>
  taskTrend.value.every((point) => Number(point.created || 0) === 0 && Number(point.completed || 0) === 0)
)
const projectEmpty = computed(() => projectHours.value.length === 0)
const weeklyEmpty = computed(() =>
  weeklyTrend.value.every((point) => Number(point.count || 0) === 0)
)

const taskTrendTotal = computed(() =>
  taskTrend.value.reduce((sum, point) => sum + Number(point.completed || 0), 0)
)
const projectTotalText = computed(() => `共 ${projectHours.value.length} 个项目`)
const weeklyThisWeek = computed(() => {
  const last = weeklyTrend.value[weeklyTrend.value.length - 1]
  return Number(last?.count || 0)
})

const chartTextStyle = {
  color: '#667085',
  fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif"
}

const tooltipStyle = {
  backgroundColor: '#ffffff',
  borderColor: '#EAECF0',
  borderWidth: 1,
  padding: [9, 12],
  textStyle: { color: '#344054', fontSize: 12 },
  extraCssText: 'box-shadow: 0 8px 24px rgba(16, 24, 40, 0.08); border-radius: 8px;'
}

const taskTrendOption = computed(() => ({
  textStyle: chartTextStyle,
  tooltip: {
    trigger: 'axis',
    ...tooltipStyle,
    axisPointer: { type: 'line', lineStyle: { color: '#D0D5DD', width: 1 } }
  },
  legend: {
    top: 0,
    right: 4,
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 16,
    icon: 'circle',
    textStyle: { color: '#667085', fontSize: 11 }
  },
  grid: { left: 4, right: 12, top: 36, bottom: 2, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: taskTrend.value.map((point) => point.label),
    axisLine: { lineStyle: { color: '#EAECF0' } },
    axisTick: { show: false },
    axisLabel: { color: '#98A2B3', fontSize: 11, margin: 10 }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitNumber: 3,
    axisLabel: { color: '#98A2B3', fontSize: 11 },
    splitLine: { lineStyle: { color: '#F2F4F7', type: 'dashed' } }
  },
  series: [
    {
      name: '新增任务',
      type: 'line',
      smooth: 0.25,
      showSymbol: false,
      data: taskTrend.value.map((point) => Number(point.created || 0)),
      lineStyle: { color: '#98A2B3', width: 1.5, type: 'dashed' },
      itemStyle: { color: '#98A2B3' },
      emphasis: { focus: 'series' }
    },
    {
      name: '完成任务',
      type: 'line',
      smooth: 0.25,
      showSymbol: true,
      symbol: 'circle',
      symbolSize: 5,
      data: taskTrend.value.map((point) => Number(point.completed || 0)),
      lineStyle: { color: '#12B76A', width: 2 },
      itemStyle: { color: '#12B76A', borderColor: '#ffffff', borderWidth: 1 },
      areaStyle: { color: 'rgba(18, 183, 106, 0.10)' },
      emphasis: { focus: 'series' }
    }
  ]
}))

const projectHoursOption = computed(() => {
  const data = projectHours.value.map((item) => ({
    name: item.project,
    value: Number(item.hours || 0)
  }))
  const compactLegend = data.length <= 2
  return {
    textStyle: chartTextStyle,
    color: ['#12B76A', '#2E90FA', '#F79009', '#7C5CD6', '#14B8A6', '#98A2B3'],
    title: {
      text: `${monthHoursText.value}h`,
      subtext: '本月工时',
      left: compactLegend ? 'center' : '34%',
      top: compactLegend ? '36%' : '33%',
      itemGap: 3,
      textStyle: { color: '#101828', fontSize: 20, fontWeight: 600 },
      subtextStyle: { color: '#98A2B3', fontSize: 11 }
    },
    tooltip: {
      trigger: 'item',
      ...tooltipStyle,
      formatter: (params) => {
        const name = escapeHtml(params.name)
        return `${name}<br/>${Number(params.value || 0).toFixed(1)} 小时 · ${params.percent}%`
      }
    },
    legend: compactLegend
      ? {
          bottom: 6,
          left: 'center',
          icon: 'circle',
          itemWidth: 8,
          itemHeight: 8,
          itemGap: 14,
          textStyle: { color: '#667085', fontSize: 11 },
          formatter: (name) => shortenLabel(name, 11)
        }
      : {
          type: 'scroll',
          orient: 'vertical',
          right: 2,
          top: 36,
          bottom: 14,
          icon: 'circle',
          itemWidth: 8,
          itemHeight: 8,
          itemGap: 9,
          textStyle: { color: '#667085', fontSize: 10.5 },
          pageIconSize: 8,
          pageTextStyle: { color: '#98A2B3', fontSize: 9 },
          formatter: (name) => shortenLabel(name, 9)
        },
    series: [
      {
        name: '项目工时',
        type: 'pie',
        radius: compactLegend ? ['54%', '72%'] : ['47%', '66%'],
        center: compactLegend ? ['50%', '44%'] : ['34%', '47%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#ffffff', borderWidth: 2, borderRadius: 4 },
        label: { show: false },
        labelLine: { show: false },
        emphasis: { scale: true, scaleSize: 4 },
        data
      }
    ]
  }
})

const weeklyTrendOption = computed(() => {
  const labels = weeklyTrend.value.map((point, index) =>
    index === weeklyTrend.value.length - 1 ? '本周' : point.label
  )
  const lastIndex = weeklyTrend.value.length - 1
  return {
    textStyle: chartTextStyle,
    tooltip: {
      trigger: 'axis',
      ...tooltipStyle,
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(18, 183, 106, 0.06)' } },
      formatter: (params) => {
        const item = params[0]
        return `${item.axisValue}<br/>周报 ${item.value} 条`
      }
    },
    grid: { left: 4, right: 16, top: 16, bottom: 2, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#EAECF0' } },
      axisTick: { show: false },
      axisLabel: { color: '#667085', fontSize: 11, margin: 10 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitNumber: 3,
      axisLabel: { color: '#98A2B3', fontSize: 11 },
      splitLine: { lineStyle: { color: '#F2F4F7', type: 'dashed' } }
    },
    series: [
      {
        name: '周报记录',
        type: 'bar',
        barWidth: 28,
        data: weeklyTrend.value.map((point, index) => ({
          value: Number(point.count || 0),
          itemStyle: {
            color: index === lastIndex ? '#12B76A' : '#A6E9C9',
            borderRadius: [5, 5, 0, 0]
          }
        })),
        label: {
          show: true,
          position: 'top',
          color: '#667085',
          fontSize: 11,
          formatter: ({ value }) => (value > 0 ? value : '')
        }
      }
    ]
  }
})

const load = async () => {
  loading.value = true
  loadFailed.value = false
  try {
    const result = await dashboardApi.get()
    if (result.code === 1 && result.data) {
      dashboard.value = {
        ...dashboard.value,
        ...result.data
      }
    } else {
      loadFailed.value = true
    }
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="wk-page dashboard-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">首页</h1>
        <p class="wk-page-desc">{{ todayText }} · 今天的工作，一眼看清</p>
      </div>
      <div class="wk-page-actions">
        <RefreshButton cooldown-key="dashboard" :loading="loading" plain size="small" @refresh="load" />
      </div>
    </div>

    <div v-if="loadFailed" class="dash-notice">
      <span>数据加载失败，请稍后重试。</span>
      <el-button link type="primary" @click="load">重新加载</el-button>
    </div>

    <div class="stat-grid">
      <StatCard label="今日待办" :value="dashboard.todayPending" unit="项" tone="green" :icon="Calendar" />
      <StatCard label="本周完成" :value="dashboard.weekCompleted" unit="项" tone="blue" :icon="CircleCheck" />
      <StatCard label="本月工时" :value="monthHoursText" unit="h" tone="amber" :icon="Clock" />
      <StatCard label="进行中计时" :value="dashboard.runningTimers" unit="个" tone="violet" :icon="AlarmClock" />
    </div>

    <div class="dash-grid">
      <section class="wk-card chart-card chart-card--main">
        <div class="chart-head">
          <div>
            <h2 class="chart-title">任务完成趋势</h2>
            <p class="chart-sub">近 7 天 · 完成 {{ taskTrendTotal }} 项</p>
          </div>
          <span class="chart-badge">近 7 天</span>
        </div>
        <div class="chart-body">
          <EChart
            :option="taskTrendOption"
            height="var(--dash-chart-main)"
            :empty="taskEmpty"
            empty-text="近 7 天暂无任务记录"
          />
        </div>
      </section>

      <section class="wk-card chart-card">
        <div class="chart-head">
          <div>
            <h2 class="chart-title">本月工时构成</h2>
            <p class="chart-sub">{{ projectTotalText }}</p>
          </div>
        </div>
        <div class="chart-body">
          <EChart
            :option="projectHoursOption"
            height="var(--dash-chart-main)"
            :empty="projectEmpty"
            empty-text="本月还没有工时记录"
          />
        </div>
      </section>
    </div>

    <section class="wk-card chart-card chart-card--weekly">
      <div class="chart-head">
        <div>
          <h2 class="chart-title">周报记录</h2>
          <p class="chart-sub">近 4 周 · 本周 {{ weeklyThisWeek }} 条</p>
        </div>
        <span class="chart-badge">每周汇总</span>
      </div>
      <div class="chart-body">
        <EChart
          :option="weeklyTrendOption"
          height="var(--dash-chart-sub)"
          :empty="weeklyEmpty"
          empty-text="近 4 周还没有周报记录"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  --dash-chart-main: 286px;
  --dash-chart-sub: 208px;
}

.dash-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 16px;
  color: #b54708;
  font-size: 12.5px;
  background: #fffaeb;
  border: 1px solid #fedf89;
  border-radius: 10px;
}

.dash-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.85fr) minmax(310px, 1fr);
  gap: 16px;
}

.chart-card {
  min-width: 0;
  overflow: hidden;
}

.chart-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px 4px;
}

.chart-title {
  margin: 0;
  color: var(--wk-text);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}

.chart-sub {
  margin: 3px 0 0;
  color: var(--wk-text-3);
  font-size: 12px;
}

.chart-badge {
  flex-shrink: 0;
  margin-top: 1px;
  padding: 3px 9px;
  color: var(--wk-text-3);
  font-size: 11px;
  line-height: 1.5;
  background: #f8faf9;
  border: 1px solid var(--wk-line-soft);
  border-radius: 999px;
}

.chart-body {
  padding: 2px 10px 10px;
}

.chart-card--weekly .chart-body {
  padding-top: 4px;
}

@media (max-width: 1100px) {
  .dash-grid {
    grid-template-columns: minmax(0, 1.55fr) minmax(280px, 1fr);
  }
}

@media (max-width: 768px) {
  .dashboard-page {
    --dash-chart-main: 238px;
    --dash-chart-sub: 196px;
  }

  .dash-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 12px;
  }

  .chart-head {
    padding: 16px 16px 2px;
  }

  .chart-title {
    font-size: 14px;
  }

  .chart-body {
    padding: 0 4px 6px;
  }

  .chart-card--weekly .chart-body {
    padding-top: 4px;
  }

  .wk-page-actions :deep(.el-button) {
    padding: 7px 10px;
    font-size: 12px;
  }
}
</style>
