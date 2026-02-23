// 在 vue.config.js 中添加配置
const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8083,
    host: 'localhost',
    // 需要添加对 /api 路径的代理
    proxy: {
      '/auth': {
        target: 'http://localhost:8088',
        changeOrigin: true
      },
      '/api': {  // 添加这一段
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  },

  chainWebpack: config => {
    config.plugin('define').tap(definitions => {
      definitions[0] = definitions[0] || {}
      definitions[0]['__VUE_PROD_HYDRATION_MISMATCH_DETAILS__'] = false
      return definitions
    })
  }
})

