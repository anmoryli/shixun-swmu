module.exports = {
  publicPath: '/',
  lintOnSave: false, // 关闭ESLint检查
  devServer: {
    host: "0.0.0.0",
    port: "9092", // 代理端口
    proxy: {
      "/api": {
        target: process.env.VUE_APP_PROXY_TARGET || "http://localhost:8082",
        changeOrigin: true,
      },
    },
  },
};
