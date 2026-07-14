// 鉴权信息的内存态持有者。
// 出于安全考虑（G.WST.01），登录令牌与用户信息不再写入 localStorage / sessionStorage，
// 仅保留在当前页面会话的内存中。代价：刷新页面后需重新登录。
let token = '';
let userInfo = null;

export function getToken() {
  return token;
}

export function setToken(value) {
  token = value || '';
}

export function getUserInfo() {
  return userInfo;
}

export function setUserInfo(value) {
  userInfo = value || null;
}

export function clearAuth() {
  token = '';
  userInfo = null;
}

export function isLoggedIn() {
  return Boolean(token);
}
