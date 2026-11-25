import {
  AlarmClock,
  Avatar,
  Calendar,
  DocumentChecked,
  FolderOpened,
  HomeFilled,
  Key,
  Memo,
  Setting,
  Timer,
  User
} from '@element-plus/icons-vue'

export const menuIconOptions = [
  { value: 'HomeFilled', label: '工作台', component: HomeFilled },
  { value: 'Calendar', label: '日历', component: Calendar },
  { value: 'DocumentChecked', label: '文档', component: DocumentChecked },
  { value: 'FolderOpened', label: '项目', component: FolderOpened },
  { value: 'Memo', label: '备忘录', component: Memo },
  { value: 'Timer', label: '计时', component: Timer },
  { value: 'AlarmClock', label: '闹钟', component: AlarmClock },
  { value: 'User', label: '用户', component: User },
  { value: 'Avatar', label: '角色', component: Avatar },
  { value: 'Key', label: '权限', component: Key },
  { value: 'Setting', label: '设置', component: Setting }
]

export const menuIconMap = Object.fromEntries(
  menuIconOptions.map((item) => [item.value, item.component])
)
