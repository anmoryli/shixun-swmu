import { login } from '../../api/Login';
import { ElMessage } from 'element-plus';
import router, { constantRoutes } from '../../router/index';
import {getMenu} from '../../utils/routeParse';
import { getToken, setToken, setUserInfo, clearAuth } from '../../utils/authStore';
const initialState = {
  token: getToken(),
  menuList: constantRoutes.slice(),
  routesLoaded: false,
};
const mutations = {
  SET_TOKEN(state, payload) {
    state.token = payload;
  },
  // 存储完整的路由
  SET_ROUTER_MENULIST(state, payload) {
    // 把固定路由和后端传来的路由合并为完整路由
    const dynamicRoutes = Array.isArray(payload) ? payload : [];
    const routes = constantRoutes.concat(dynamicRoutes);
    state.menuList = routes;
    state.routesLoaded = true;
    dynamicRoutes.forEach((route) => router.addRoute(route));
  },
  RESET_AUTH(state) {
    state.token = '';
    state.menuList = constantRoutes.slice();
    state.routesLoaded = false;
  },
};
const actions = {
  // 登录接口
  async login({ commit }, loginInfo) {
    // 清除同一浏览器会话中可能残留的上一账号菜单状态。
    commit('RESET_AUTH');
    const username = loginInfo.username.trim();
    const res = await login(username, loginInfo.password);
    if (!res.data || Number(res.data.code) !== 20000) {
      throw new Error((res.data && res.data.message) || '账号或密码错误');
    }

    const data = res.data.data || {};
    if (!data.token || !data.userInfo) {
      throw new Error('登录接口返回的数据不完整');
    }

    setUserInfo(data.userInfo);
    setToken(data.token);
    commit('SET_TOKEN', data.token);
    ElMessage({
      type: 'success',
      message: '登录成功',
    });
    return data;
  },
  // 获取后端传来的路由列表
  async setMenuList({ commit, state }) {
    if (state.routesLoaded) {
      return state.menuList.slice(constantRoutes.length);
    }
    const routes = await getMenu();
    commit('SET_ROUTER_MENULIST', routes);
    return routes;
  },
  logout({ commit }) {
    clearAuth();
    commit('RESET_AUTH');
  },
};
export default {
  namespaced: true,
  state: initialState,
  mutations,
  actions,
};
