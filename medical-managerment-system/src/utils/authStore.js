// 鉴权信息的内存态持有者。
// 出于安全考虑（G.WST.01），token 不再由前端持有：改用 httpOnly cookie 由浏览器自动携带，
// 前端 JS 读不到。此处仅保留登录态标志与用户信息（UI 展示用），刷新后由 /api/session 恢复。
// getToken/setToken 保留为空操作仅为过渡兼容，待请求层改造完成后移除。
let loggedIn = false;
let userInfo = null;

export function isLoggedIn() {
  return loggedIn;
}

export function setLoggedIn(value) {
  loggedIn = Boolean(value);
}

export function getUserInfo() {
  return userInfo;
}

export function setUserInfo(value) {
  userInfo = value || null;
  loggedIn = userInfo != null;
}

// 过渡兼容：token 已迁移至 httpOnly cookie，前端不再持有。
export function getToken() {
  return '';
}

export function setToken() {
  // no-op：token 由 httpOnly cookie 承载，前端不持有。
}

export function clearAuth() {
  loggedIn = false;
  userInfo = null;
}
