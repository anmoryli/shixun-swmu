import Vue from "vue";
import VueRouter from "vue-router";
import Login from "../views/Login.vue";
import { Message } from "element-ui";

Vue.use(VueRouter);

// 解决导航栏或者底部导航tabBar中的vue-router在3.0版本以上频繁点击菜单报错的问题。
const originalPush = VueRouter.prototype.push;
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch((err) => err);
};
export const constantRoutes = [
    {
      path: "/user/login",
      name: "Login",
      component: Login,
      meta: { title: "登录" },
    },
    {
      path: "/",
      redirect: "/home",
    },
  ]
  
  const router = new VueRouter({
    mode: "hash",
    routes: constantRoutes
  });
  // 判断登录状态
router.beforeEach((to, from, next) => {
    document.title = (to.meta && to.meta.title) || "慧医数字医疗应用系统";
    const token = localStorage.getItem("token");
    if (!token && to.path !== "/user/login") {
      next("/user/login");
      return;
    }
    if (!token) {
      next();
      return;
    }

    // store 在守卫执行时再加载，避免 router/store 相互引用导致初始化竞态。
    const store = require("../store").default;
    if (store.state.app.routesLoaded) {
      if (to.path === "/user/login") {
        next("/");
      } else {
        next();
      }
      return;
    }

    store
      .dispatch("app/setMenuList")
      .then(() => {
        if (to.path === "/user/login") {
          next("/");
        } else {
          next({ path: to.fullPath, replace: true });
        }
      })
      .catch((error) => {
        store.dispatch("app/logout");
        Message.error(error.message || "登录状态已失效，请重新登录");
        next("/user/login");
      });
  });
  
  export default router;
