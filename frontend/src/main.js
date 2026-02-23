import { createApp } from 'vue'
import App from './App.vue'
import router from './router'  // 改为导入路由实例
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)
app.use(router)  // 使用路由实例
app.use(ElementPlus)
app.mount('#app')
