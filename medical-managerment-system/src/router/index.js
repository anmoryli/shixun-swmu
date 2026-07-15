import { createRouter, createWebHashHistory } from 'vue-router';
import Login from '../views/Login.vue';
import { ElMessage } from 'element-plus';
import { isLoggedIn } from '../utils/authStore';
import { allowedPath } from '../utils/permissions';

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

export function isRouteAllowed(path, appState) {
    if (path === '/user/login' || path === '/') {
        return true;
    }
    const paths = appState && appState.allowedRoutePaths;
    // A loaded real profile must have an explicit route allowlist. Empty
    // metadata is tolerated only before dynamic routes have been installed.
    if (!Array.isArray(paths) || !paths.length) {
        return !(appState && appState.routesLoaded);
    }
    return allowedPath(path, paths);
}

function firstAllowedPath(appState) {
    const paths = appState && Array.isArray(appState.allowedRoutePaths)
        ? appState.allowedRoutePaths
        : [];
    if (allowedPath('/home', paths)) {
        return '/home';
    }
    return paths.find((path) => path && path !== '/' && path !== '/user/login') || '/';
}

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
    const appState = store.state && store.state.app ? store.state.app : {};
    if (appState.routesLoaded) {
        if (to.path === '/user/login') {
            next('/');
        } else if (!isRouteAllowed(to.path, appState)) {
            next(firstAllowedPath(appState));
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
          } else if (!isRouteAllowed(to.path, store.state && store.state.app)) {
              next(firstAllowedPath(store.state && store.state.app));
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
