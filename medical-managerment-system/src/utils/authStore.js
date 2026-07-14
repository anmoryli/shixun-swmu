// 鉴权信息的内存态持有者。
// 出于安全考虑（G.WST.01），token 不再由前端持有：改用 httpOnly cookie 由浏览器自动携带，
// 前端 JS 读不到。此处仅保留登录态标志与用户信息（UI 展示用），刷新后由 /api/session 恢复。
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

export function clearAuth() {
  loggedIn = false;
  userInfo = null;
}
