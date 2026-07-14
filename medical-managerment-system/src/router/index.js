import { createRouter, createWebHashHistory } from 'vue-router';
import Login from '../views/Login.vue';
import { ElMessage } from 'element-plus';
import { isLoggedIn } from '../utils/authStore';

export const constantRoutes = [
    {
      path: '/user/login',
      name: 'Login',
      component: Login,
      meta: { title: '登录' },
    },
    {
      path: '/',
      redirect: () => (isLoggedIn() ? '/home' : '/user/login'),
    },
  ];

  const router = createRouter({
    history: createWebHashHistory(),
    routes: constantRoutes,
  });
  // 判断登录状态
router.beforeEach(async (to, from, next) => {
    document.title = (to.meta && to.meta.title) || '慧医数字医疗应用系统';
    let loggedIn = isLoggedIn();
    const store = router.appStore;
    // 内存无登录态时，尝试从 httpOnly cookie 恢复（前端读不到 cookie，需询问后端）。
    if (!loggedIn && store) {
        loggedIn = await store.dispatch('app/checkSession');
    }
    if (!loggedIn && to.path !== '/user/login') {
        next('/user/login');
        return;
    }
    if (!loggedIn) {
        next();
        return;
    }

    // store 在守卫执行时再加载，避免 router/store 相互引用导致初始化竞态。
    if (store.state.app.routesLoaded) {
        if (to.path === '/user/login') {
            next('/');
        } else {
            next();
        }
        return;
    }

    store
      .dispatch('app/setMenuList')
      .then(() => {
          if (to.path === '/user/login') {
              next('/');
          } else {
              next({ path: to.fullPath, replace: true });
          }
      })
      .catch((error) => {
          store.dispatch('app/logout');
          ElMessage.error(error.message || '登录状态已失效，请重新登录');
          next('/user/login');
      });
  });

export function attachStore(store) {
    router.appStore = store;
}

export default router;
