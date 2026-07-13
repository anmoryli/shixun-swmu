import Vue from 'vue'
import App from './App.vue'
import router from './router' // 引入router
import store from './store' // 引入vuex

import _ from 'lodash' // js库
import "./style/reset.css"; // 引入样式
import Fragment from "vue-fragment"; // 处理Dom插件
import "animate.css"; // 引入样式
import Element from "element-ui";
import "element-ui/lib/theme-chalk/index.css";

Vue.config.productionTip = false;
Vue.use(Fragment.Plugin);
Vue.use(Element);
Vue.prototype._ = _;
Vue.mixin({
  computed: {
      // 进行按钮权限控制
      hasRole() {
          if(localStorage.getItem('userInfo')) {
              if(JSON.parse(localStorage.getItem('userInfo')).utype === 1) {
                  return true
              } else {
                  return false
              }
          } else {
              return false
          }
      }
  }
});
new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')