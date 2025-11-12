// 导入Vue的createApp函数，用于创建Vue应用实例
import { createApp } from 'vue'

// 导入全局样式文件
//import './style.css'

// 导入根组件App.vue
import App from './App.vue'

// 导入Element Plus组件库及其样式文件
import ElementPlus from 'element-plus' // 全局引入Element Plus
import 'element-plus/dist/index.css' // 引入Element Plus的样式

// 创建Vue应用实例，传入根组件App
const app = createApp(App);

// 使用Element Plus插件，这样可以在整个应用中使用Element Plus组件
app.use(ElementPlus)

// 将Vue应用挂载到HTML中id为'app'的DOM元素上
app.mount('#app')