// 导入defineConfig函数，用于定义Vite配置（提供类型提示和智能感知）,Vite类似于Webpack的打包和构建工具
import { defineConfig } from 'vite'

// 导入Vite的Vue插件，用于支持Vue单文件组件
import vue from '@vitejs/plugin-vue'

// Vite配置文件
// https://vitejs.dev/config/
export default defineConfig({
  // 插件配置数组
  plugins: [
    // 使用Vue插件，让Vite能够处理.vue文件
    vue()
  ],
})